plugins {
    id("java-library")
}

dependencies {
    implementation("com.squareup:javapoet:1.12.1")
    implementation("androidx.annotation:annotation:1.5.0")
    testImplementation("com.google.guava:guava:29.0-android")
    testImplementation("com.google.truth:truth:1.0.1")
    //testImplementation("com.google.truth:truth-java8-extension:1.0.1")
    testImplementation("com.google.testing.compile:compile-testing:0.18")
    testImplementation("com.google.android:android:4.1.1.4")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
