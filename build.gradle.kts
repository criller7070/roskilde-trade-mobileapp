// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// Add buildscript classpath and repositories for Google Services plugin
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Google Services Gradle plugin (processes google-services.json)
        classpath("com.google.gms:google-services:4.4.0")
    }
}
