// Android library: real-time DSP core (multiband gain, WDRC compression,
// feedback/howl guard, and the SAFETY-CRITICAL output limiter). The DSP math is
// pure Kotlin behind interfaces; AAudio/Oboe is only the thin I/O shell, so the
// core — including the limiter — is unit-testable without a device.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.android.junit)
}

android {
    namespace = "app.earcast.core.audio"
    compileSdk =
        libs
            .versions
            .compileSdk
            .get()
            .toInt()
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        minSdk =
            libs
                .versions
                .minSdk
                .get()
                .toInt()
        consumerProguardFiles("consumer-rules.pro")
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86") }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = libs.versions.cmake.get()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents.onVariants { variant ->
    variant.hostTests["unitTest"]?.sources?.kotlin?.addStaticSourceDirectory(
        rootProject.file("audio-quality/kotlin").path,
    )
}

dependencies {
    implementation(project(":foundation"))
    implementation(project(":sound-profile"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
}
