// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Google Services Gradle plugin (processes google-services.json)
    id("com.google.gms.google-services") version "4.4.4" apply false

    // Hilt Gradle plugin (processes build.gradle.kts)
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
