import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// Required for Groovy compatiblity, which cannot use Consts.kt
ext {
    set("mavenGroupId", SDKConsts.MAVEN_GROUP_ID)
    set("mavenArtifact", SDKConsts.MAVEN_ARTIFACT)
    set("mavenArtifactVersion", SDKConsts.MAVEN_ARTIFACT_VERSION)
}

base {
    archivesName.set("Batch")
}

android {
    compileSdk = ProjectConsts.COMPILE_SDK

    resourcePrefix = SDKConsts.R_PREFIX
    namespace = SDKConsts.NAMESPACE
    testNamespace = SDKConsts.TEST_NAMESPACE

    defaultConfig {
        minSdk = SDKConsts.MIN_SDK
        targetSdk = SDKConsts.TARGET_SDK

        buildConfigField("String", "SDK_VERSION", "\"${SDKConsts.VERSION}\"")
        buildConfigField("Integer", "API_LEVEL", "${SDKConsts.API_LEVEL}")
        buildConfigField("Integer", "MESSAGING_API_LEVEL", "${SDKConsts.MESSAGING_API_LEVEL}")
        buildConfigField("String", "WS_DOMAIN", "\"ws.batch.com\"")

        consumerProguardFiles("proguard-consumer-rules.txt")
        project.version = SDKConsts.VERSION
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildTypes {

        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-sdk-release.txt")

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

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
            all {
                apply {
                    it.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED")
                    it.testLogging.events = setOf(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
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

// Prevent from having accidental kotlin in production code
/*afterEvaluate {
    android.sourceSets.all { sourceSet ->
        if (!sourceSet.name.startsWith("test")) {
            sourceSet.kotlin.setSrcDirs([])
        }
    }
}*/

// Print the sdk version name
tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}

dependencies {
    val androidXLibraryVersion = SDKConsts.DependenciesVersions.ANDROIDX
    val playServicesVersion = SDKConsts.DependenciesVersions.PLAY_SERVICES
    val kotlinVersion = ProjectConsts.KOTLIN_VERSION
    val kotlinCoroutinesVersion = ProjectConsts.KOTLIN_COROUTINES_VERSION

    compileOnly("com.google.android.gms:play-services-ads:${playServicesVersion}")
    compileOnly("com.google.android.gms:play-services-gcm:${playServicesVersion}")
    compileOnly("com.google.android.gms:play-services-location:${playServicesVersion}")
    compileOnly("com.google.android.gms:play-services-nearby:${playServicesVersion}")
    compileOnly("com.google.android.play:core:1.9.0")
    compileOnly("com.google.firebase:firebase-iid:21.1.0")
    compileOnly("com.google.firebase:firebase-messaging:22.0.0")
    compileOnly(project(":sdk-stubs"))
    compileOnly(project(":sdk-processor"))
    annotationProcessor(project(":sdk-processor"))
    lintChecks(project(":sdk-lint"))

    api("androidx.core:core:${androidXLibraryVersion}")
    compileOnly("androidx.appcompat:appcompat:${androidXLibraryVersion}")
    compileOnly("com.google.android.material:material:${androidXLibraryVersion}")
    compileOnly("androidx.dynamicanimation:dynamicanimation:$androidXLibraryVersion")

    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.annotation:annotation:${androidXLibraryVersion}")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.ext:truth:1.5.0")

    androidTestImplementation("com.google.android.gms:play-services-ads:${playServicesVersion}")
    androidTestImplementation("com.google.android.gms:play-services-gcm:${playServicesVersion}")
    androidTestImplementation("com.google.android.gms:play-services-location:${playServicesVersion}")
    androidTestImplementation("com.google.android.gms:play-services-nearby:${playServicesVersion}")
    androidTestImplementation("com.google.firebase:firebase-core:17.4.3")
    androidTestImplementation("androidx.appcompat:appcompat:${androidXLibraryVersion}")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.4.0")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:rules:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test.ext:junit-ktx:1.1.5")
    testImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    testImplementation("androidx.test.espresso:espresso-core:3.5.1")
    testImplementation("androidx.test.ext:truth:1.5.0")
    testImplementation("com.google.android.gms:play-services-ads:${playServicesVersion}")
    testImplementation("com.google.android.gms:play-services-gcm:${playServicesVersion}")
    testImplementation("com.google.android.gms:play-services-location:${playServicesVersion}")
    testImplementation("com.google.android.gms:play-services-nearby:${playServicesVersion}")
    testImplementation("androidx.appcompat:appcompat:${androidXLibraryVersion}")

    testImplementation("org.robolectric:robolectric:4.9.2")
    testImplementation("org.powermock:powermock-module-junit4:2.0.9")
    testImplementation("org.powermock:powermock-module-junit4-rule:2.0.9")
    testImplementation("org.powermock:powermock-api-mockito2:2.0.9")
    testImplementation("org.powermock:powermock-classloading-xstream:2.0.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlinCoroutinesVersion}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${kotlinCoroutinesVersion}")
}

apply(from = "maven-publish.gradle")
apply(from = "jacoco.gradle")
apply(from = "metalava.gradle")
