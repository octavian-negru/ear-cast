import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.junit)
}

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
val localRelease =
    providers
        .gradleProperty("earcastLocalRelease")
        .orElse("false")
        .get()
        .toBooleanStrict()
// APK splits do not filter bundle ABIs; build bundles with this flag off.
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
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
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}

val verifyPlayRelease =
    tasks.register("verifyPlayRelease") {
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

val verifyLocalReleaseApk =
    tasks.register("verifyLocalReleaseApk") {
        group = "verification"
        description = "Verifies the signatures of all APKs produced for local installation."
        dependsOn(signatureChecks)
    }

val assembleLocalRelease =
    tasks.register("assembleLocalRelease") {
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
