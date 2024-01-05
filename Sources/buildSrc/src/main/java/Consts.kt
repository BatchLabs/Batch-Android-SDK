object ProjectConsts {
    const val COMPILE_SDK = 34

    const val KOTLIN_VERSION = "1.7.21"
    const val KOTLIN_COROUTINES_VERSION = "1.6.4"

    const val ANDROID_LINT_VERSION = "30.1.2"

    const val ANDROID_GRADLE_PLUGIN_VERSION = "8.0.2"
    const val GMS_GRADLE_PLUGIN_VERSION = "4.3.14"
}

object SDKConsts {
    const val VERSION = "1.21.0"
    const val API_LEVEL = 70
    const val MESSAGING_API_LEVEL = 12

    const val MIN_SDK = 15
    const val TARGET_SDK = 34
    const val R_PREFIX = "com_batchsdk_"
    const val NAMESPACE = "com.batch.android"
    const val TEST_NAMESPACE = "com.batch.android.test"
    const val MAVEN_GROUP_ID = "com.batch.android"
    const val MAVEN_ARTIFACT = "batch-sdk"
    const val MAVEN_ARTIFACT_VERSION = VERSION

    // SDK and Sample don't share the same deps as the SDK
    // usually wants lower versions for compatibility.
    object DependenciesVersions {
        const val ANDROIDX = "1.0.0"
        const val PLAY_SERVICES = "11.8.0"
    }
}

object SampleConsts {
    object DependenciesVersions {
        const val ANDROIDX = "1.1.0"
        const val PLAY_SERVICES = "16.0.0"
    }
}

