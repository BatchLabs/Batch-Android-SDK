plugins {
    id("java-library")
}

dependencies {
    val lintVersion = ProjectConsts.ANDROID_LINT_VERSION

    compileOnly("com.android.tools.lint:lint-api:$lintVersion")
    compileOnly("com.android.tools.lint:lint-checks:$lintVersion")

    testImplementation("junit:junit:4.12")
    testImplementation("com.android.tools.lint:lint:$lintVersion")
    testImplementation("com.android.tools.lint:lint-tests:$lintVersion")
    testImplementation("com.android.tools:testutils:$lintVersion")
}


tasks.withType<Jar> {
    manifest {
        attributes["Lint-Registry-v2"] = "com.batch.android.lint.BatchIssueRegistry"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}