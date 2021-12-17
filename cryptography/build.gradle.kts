plugins {
    Plugins.androidLibrary(this)
    Plugins.multiplatform(this)
    Plugins.serialization(this)
}

group = "com.wire.kalium"
version = "0.0.1-SNAPSHOT"

android {
    compileSdk = Android.Sdk.compile
    defaultConfig {
        minSdk = Android.Sdk.min
        targetSdk = Android.Sdk.target
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    // Remove Android Unit tests, as it's currently impossible to run native-through-NDK code on simple Unit tests.
    sourceSets.remove(sourceSets["test"])
    externalNativeBuild {
        cmake {
            version = Android.Ndk.cMakeVersion
        }
        ndkBuild {
            ndkVersion = Android.Ndk.version
            path(File("src/androidMain/jni/Android.mk"))
        }
    }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
            kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()

            if (System.getProperty("os.name").contains("Mac", true)) {
                jvmArgs = jvmArgs?.plus(listOf("-Djava.library.path=/usr/local/lib/:../native/libs"))
            }
        }
    }
    android()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // coroutines
                implementation(Dependencies.Coroutines.core)

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(Dependencies.Cryptography.cryptobox4j)
            }
        }
        val jvmTest by getting
        val androidMain by getting {
            dependencies {
                implementation(Dependencies.Cryptography.cryptoboxAndroid)
            }
        }
        val androidAndroidTest by getting {
            dependencies {
                implementation(Dependencies.AndroidInstruments.androidTestRunner)
                implementation(Dependencies.AndroidInstruments.androidTestRules)
            }
        }
    }
}
