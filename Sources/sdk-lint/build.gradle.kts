plugins {
    id("java-library")
}

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)
    testImplementation(libs.junit)
    testImplementation(libs.lint)
    testImplementation(libs.lint.tests)
    testImplementation(libs.testutils)
}

tasks.withType<Jar> {
    manifest {
        attributes["Lint-Registry-v2"] = "com.batch.android.lint.BatchIssueRegistry"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}