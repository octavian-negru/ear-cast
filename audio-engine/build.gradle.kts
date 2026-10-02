import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

androidComponents.onVariants { variant ->
    variant.hostTests.values.forEach { hostTest ->
        hostTest.sources.kotlin?.addStaticSourceDirectory(
            rootProject.file("audio-quality/kotlin").path,
        )
    }
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
