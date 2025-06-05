import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.dokka")
    id("com.google.devtools.ksp")
    id("com.ncorti.ktfmt.gradle")
}

base { archivesName.set("Batch") }

android {
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    resourcePrefix = libs.versions.batchResourcePrefix.get()
    namespace = libs.versions.batchNamespace.get()
    testNamespace = libs.versions.batchTestNamespace.get()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()

        buildConfigField("String", "SDK_VERSION", "\"${libs.versions.batchSdk.get()}\"")
        buildConfigField("Integer", "API_LEVEL", "${libs.versions.batchApiLevel.get().toInt()}")
        buildConfigField(
            "Integer",
            "MESSAGING_API_LEVEL",
            "${libs.versions.batchMessagingApiLevel.get().toInt()}",
        )

        consumerProguardFiles("proguard-consumer-rules.txt")
        project.version = libs.versions.batchSdk.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions { jvmTarget = "1.8" }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-sdk-release.txt",
            )

            buildConfigField("boolean", "ENABLE_DEBUG_LOGGER", "false")
            buildConfigField("boolean", "ENABLE_WS_INTERCEPTOR", "false")
        }

        debug {
            multiDexEnabled = true
            isMinifyEnabled = false

            buildConfigField("boolean", "ENABLE_DEBUG_LOGGER", "true")
            buildConfigField("boolean", "ENABLE_WS_INTERCEPTOR", "true")
        }
    }

    buildFeatures { buildConfig = true }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
            all {
                apply {
                    it.jvmArgs(
                        "--add-opens=java.base/java.lang=ALL-UNNAMED",
                        "--add-opens=java.base/java.util=ALL-UNNAMED",
                    )
                    it.testLogging.events =
                        setOf(
                            TestLogEvent.STARTED,
                            TestLogEvent.PASSED,
                            TestLogEvent.SKIPPED,
                            TestLogEvent.FAILED,
                        )
                    it.testLogging.showCauses = true
                    it.testLogging.showExceptions = true
                }
            }
        }
    }
    lint {
        abortOnError = true
        baseline = file("lint-baseline.xml")
        lintConfig = file("lint.xml")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

// Print the sdk version name
tasks.register("printVersion") { doLast { println(project.version) } }

dokka {
    moduleName.set("Batch Android SDK")
    dokkaSourceSets.main {
        // includes.from("README.md") // Texte a mettre sur la page d'intro
        sourceLink {
            localDirectory.set(file("src/main"))
            remoteUrl("https://github.com/BatchLabs/Batch-Android-SDK")
            // remoteLineSuffix.set("#L")
        }

        documentedVisibilities(VisibilityModifier.Public)

        perPackageOption {
            matchingRegex.set("""com\.batch\.android\..*""") // will match all subpackages
            suppress.set(true)
        }

        perPackageOption {
            matchingRegex.set("""com\.batch\.android\.json""") // will match all subpackages
            suppress.set(false)
        }
    }
    pluginsConfiguration.html { footerMessage.set("(c) Batch") }
    dokkaPublications.html { outputDirectory.set(project.rootProject.file("../javadoc")) }
}

ktfmt { kotlinLangStyle() }

dependencies {
    // We use `api` since some androidx compatibility models
    // are exposed through our public api (eg: BatchNotificationInterceptor)
    api(libs.androidx.core.ktx)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)

    compileOnly(project(":sdk-stubs"))
    compileOnly(project(":sdk-processor"))
    ksp(project(":sdk-processor"))
    lintChecks(project(":sdk-lint"))

    compileOnly(libs.google.play.review)
    compileOnly(libs.firebase.messaging)
    compileOnly(libs.material)
    compileOnly(libs.androidx.appcompat)
    compileOnly(libs.androidx.dynamicanimation)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.core.ktx)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.truth)
    androidTestImplementation(libs.firebase.core)
    androidTestImplementation(libs.androidx.appcompat)
    androidTestImplementation(libs.androidx.annotation)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.rules)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.test.junit.ktx)
    testImplementation(libs.androidx.test.espresso.intents)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.test.truth)
    testImplementation(libs.androidx.appcompat)
    testImplementation(libs.kotlin.test)

    testImplementation(libs.robolectric)
    testImplementation(libs.powermock.module.junit4)
    testImplementation(libs.powermock.module.junit4.rule)
    testImplementation(libs.powermock.api.mockito2)
    testImplementation(libs.powermock.classloading.xstream)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.android)

    dokkaPlugin(project(":dokka-limit-public-api"))
}

apply(from = "maven-publish.gradle")

apply(from = "jacoco.gradle")

apply(from = "metalava.gradle")
