import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.FailOnSeverity

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
    apply(plugin = "dev.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    detekt {
        buildUponDefaultConfig.set(true)
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        ignoreFailures.set(false)
        failOnSeverity.set(FailOnSeverity.Info)
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget.set("17")
    }

    ktlint {
        version.set(ktlintEngineVersion)
        android.set(true)
        ignoreFailures.set(false)
    }
}
