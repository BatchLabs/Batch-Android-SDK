// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version ProjectConsts.ANDROID_GRADLE_PLUGIN_VERSION apply false
    id("com.android.library") version ProjectConsts.ANDROID_GRADLE_PLUGIN_VERSION apply false
    id("org.jetbrains.kotlin.android") version ProjectConsts.KOTLIN_VERSION apply false
    id("com.google.gms.google-services") version ProjectConsts.GMS_GRADLE_PLUGIN_VERSION apply false
    id("io.codearte.nexus-staging") version "0.30.0"
    id("org.sonarqube") version "4.2.1.3168"
}
