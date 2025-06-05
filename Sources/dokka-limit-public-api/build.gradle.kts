plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    compileOnly(libs.dokka.core)
    implementation(libs.dokka.base)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.dokka.base.test.utils)
    testImplementation(libs.dokka.test.api)
}