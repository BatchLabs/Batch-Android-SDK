pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "1.9.0"
        id("com.google.devtools.ksp") version "1.9.0-1.0.13" apply false
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Batch Android SDK"
include (":sdk", ":sdk-sample", ":sdk-stubs", ":sdk-processor", ":sdk-lint", ":dokka-limit-public-api")
