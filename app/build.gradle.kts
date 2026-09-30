// Android application: Compose + Material 3 UI, navigation, Hilt wiring,
// onboarding/disclaimers, and debug screens. Depends on the core modules; no
// core module depends back on :app.
import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.junit5)
}

// Release signing is read from a gitignored keystore.properties (never committed).
// Unsigned audit bundles require an explicit opt-in — see docs/RELEASE.md.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps =
    Properties().apply {
        if (keystorePropsFile.exists()) {
            keystorePropsFile
                .inputStream()
                .use { load(it) }
        }
    }

val privacyPolicyUrl = providers.gradleProperty("earcastPrivacyPolicyUrl").orElse("")
val supportEmail = providers.gradleProperty("earcastSupportEmail").orElse("")
// Test inventory is opt-in and compiled only into debug. Release never includes the SDK.
val testAdsEnabled =
    providers
        .gradleProperty("earcastTestAds")
        .orElse("false")
        .get()
        .toBooleanStrict()
// The local APK recipe explicitly permits debug-key signing when no release key is configured.
val localRelease =
    providers
        .gradleProperty("earcastLocalRelease")
        .orElse("false")
        .get()
        .toBooleanStrict()
// ABI-specific APKs preserve all features but avoid shipping other CPUs' native libraries.
// This selects APK outputs, not ABI filters. Build App Bundles separately with this flag off.
val splitApks =
    providers
        .gradleProperty("earcastSplitApks")
        .orElse("false")
        .get()
        .toBooleanStrict()
val supportedAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
val localApkNames =
    if (splitApks) supportedAbis.associateWith { "app-$it-release.apk" } else mapOf("universal" to "app-release.apk")
val unsignedAudit = providers.gradleProperty("earcastUnsignedAudit").orElse("false")

// Only validated characters are interpolated into generated Java source.
require(privacyPolicyUrl.get().matches(Regex("[A-Za-z0-9:/?&=._%+#~-]*"))) { "Invalid privacy URL characters" }
require(supportEmail.get().matches(Regex("[A-Za-z0-9@._+%-]*"))) { "Invalid support email characters" }

android {
    namespace = "app.earcast"
    // The APK packager needs the same installed strip tool as the native build.
    ndkVersion = libs.versions.ndk.get()
    compileSdk =
        libs
            .versions
            .compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "app.earcast"
        minSdk =
            libs
                .versions
                .minSdk
                .get()
                .toInt()
        targetSdk =
            libs
                .versions
                .targetSdk
                .get()
                .toInt()
        versionCode = 2
        versionName = "0.0.1"
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"${privacyPolicyUrl.get()}\"")
        buildConfigField("String", "SUPPORT_EMAIL", "\"${supportEmail.get()}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("debug") {
            java.srcDir(if (testAdsEnabled) "src/testAds/kotlin" else "src/noAds/kotlin")
            if (testAdsEnabled) {
                manifest.srcFile("src/testAds/AndroidManifest.xml")
                res.srcDir("src/testAds/res")
            }
        }
        getByName("release").java.srcDir("src/noAds/kotlin")
    }

    splits {
        abi {
            isEnable = splitApks
            reset()
            include(*supportedAbis.toTypedArray())
            isUniversalApk = false
        }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                signingConfigs.findByName("release")
                    ?: if (localRelease) signingConfigs.getByName("debug") else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true // BuildConfig.VERSION_NAME shown on the About card
    }
}

dependencies {
    if (testAdsEnabled) debugImplementation(libs.google.mobile.ads)

    implementation(project(":foundation"))
    implementation(project(":sound-profile"))
    implementation(project(":audio-engine"))
    implementation(project(":local-storage"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}

// Keep local tests and APK audits available without pretending an unsigned AAB
// is publishable. The normal Play bundle command fails on missing release inputs.
val verifyPlayRelease by tasks.registering {
    group = "verification"
    description = "Checks signing and public privacy contact before packaging a Play bundle."
    val auditOnly = unsignedAudit.get().toBoolean()
    val apkSplitsEnabled = splitApks
    val signingValues = listOf("storeFile", "storePassword", "keyAlias", "keyPassword").map { keystoreProps.getProperty(it) }
    val uploadStore = keystoreProps.getProperty("storeFile")?.let { rootProject.file(it) }
    val policyUrl = privacyPolicyUrl.get()
    val contactEmail = supportEmail.get()
    doLast {
        check(!apkSplitsEnabled) {
            "Build App Bundles with -PearcastSplitApks=false; AGP 8.10 resource shrinking requires one APK output."
        }
        if (auditOnly) {
            logger.warn("UNSIGNED AUDIT ONLY: this bundle has not passed Play release configuration checks.")
        } else {
            check(signingValues.all { !it.isNullOrBlank() }) {
                "Configure signing in gitignored keystore.properties. See docs/RELEASE.md."
            }
            check(uploadStore?.isFile == true) { "Upload keystore does not exist." }
            val uri = runCatching { URI(policyUrl) }.getOrNull()
            check(uri?.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null) {
                "Set earcastPrivacyPolicyUrl to your published HTTPS privacy policy."
            }
            check(contactEmail.matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) {
                "Set earcastSupportEmail to a monitored public contact address."
            }
        }
    }
}
tasks.matching { it.name == "bundleRelease" || it.name == "buildReleasePreBundle" }.configureEach {
    dependsOn(verifyPlayRelease)
}

// Validate every produced APK, including all ABI-specific outputs.
val signatureChecks =
    localApkNames.map { (abi, fileName) ->
        val suffix = abi.split('-', '_').joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        tasks.register<Exec>("verifyLocalRelease${suffix}Signature") {
            group = "verification"
            description = "Verifies the $abi APK signature."
            dependsOn("assembleRelease")
            val sdk =
                androidComponents.sdkComponents.sdkDirectory
                    .get()
                    .asFile
            val signerName = if (System.getProperty("os.name").startsWith("Windows")) "apksigner.bat" else "apksigner"
            commandLine(
                sdk.resolve("build-tools/${android.buildToolsVersion}/$signerName"),
                "verify",
                "--verbose",
                layout.buildDirectory
                    .file("outputs/apk/release/$fileName")
                    .get()
                    .asFile,
            )
        }
    }

val verifyLocalReleaseApk by tasks.registering {
    group = "verification"
    description = "Verifies the signatures of all APKs produced for local installation."
    dependsOn(signatureChecks)
}

// Installable release APKs for local testing; publication still uses verifyPlayRelease.
val assembleLocalRelease by tasks.registering {
    group = "build"
    description = "Builds signed, optimized APKs for installation on a local device."
    val localSigningAllowed = localRelease
    val configuredReleaseSigning = keystoreProps.isNotEmpty()
    val apks = localApkNames.mapValues { (_, name) -> layout.buildDirectory.file("outputs/apk/release/$name") }
    dependsOn(verifyLocalReleaseApk)
    doLast {
        check(configuredReleaseSigning || localSigningAllowed) {
            "Use just build-prod or pass -PearcastLocalRelease=true for local signing."
        }
        if (!configuredReleaseSigning) {
            logger.lifecycle("LOCAL TEST APK: signed with the debug key; configure keystore.properties before publishing.")
        }
        apks.forEach { (abi, output) ->
            val apk = output.get().asFile
            check(apk.isFile) { "Expected signed APK was not produced: $apk" }
            logger.lifecycle("Install APK ($abi): $apk (${apk.length() / 1_000_000} MB)")
        }
    }
}
