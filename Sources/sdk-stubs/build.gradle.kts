plugins {
    id("com.android.library")
}

android {
    namespace = "sun.misc"

    compileSdk = ProjectConsts.COMPILE_SDK

    defaultConfig {
        minSdk = SDKConsts.MIN_SDK
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

dependencies {
}
