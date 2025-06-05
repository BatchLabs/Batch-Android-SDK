plugins {
    id("java-library")
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.ksp.symbol.processing.api)
    implementation(libs.androidx.annotation)

    testImplementation (libs.assertj.core)
    testImplementation(libs.kotlin.compile.testing.ksp)
    testImplementation(libs.android)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(8)
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}