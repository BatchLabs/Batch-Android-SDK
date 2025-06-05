// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.gms.google.services) apply false
    alias(libs.plugins.testify) apply false
    alias(libs.plugins.nexus.staging)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kfmt)
}
