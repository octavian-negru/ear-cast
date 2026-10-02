import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.android.junit) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

val ktlintEngineVersion = libs.versions.ktlintEngine.get()

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    detekt {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        ignoreFailures = false
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget = "17"
    }

    ktlint {
        version.set(ktlintEngineVersion)
        android.set(true)
        ignoreFailures.set(false)
    }
}
