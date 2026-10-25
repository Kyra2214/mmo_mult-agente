plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.kyra2214.mmomultiagente"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.kyra2214.mmomultiagente"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all {
                it.testLogging.events = setOf(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
                it.testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                it.testLogging.showStandardStreams = true
            }
        }
    }
}
dependencies { testImplementation("junit:junit:4.13.2") }
