plugins {
    id("com.android.application")
}

// Where the two sites live, set at build time so the source names no servers:
//   ./gradlew assembleRelease -Psignal.tvUrl=https://tv.example.com/ -Psignal.radioUrl=https://radio.example.com/
// The release is signed with your own key (-Psignal.keystore=… -Psignal.storePassword=…);
// keep it, since Android only installs an update signed with the same key.
fun prop(name: String): String? = providers.gradleProperty("signal.$name").orNull

android {
    namespace = "io.github.alanheinen.signal"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.alanheinen.signal"
        minSdk = 26
        targetSdk = 35
        versionCode = (prop("versionCode") ?: "1").toInt()
        versionName = prop("versionName") ?: "1.0"
        buildConfigField("String", "TV_URL", "\"${prop("tvUrl") ?: "http://tv.example.lan/"}\"")
        buildConfigField("String", "RADIO_URL", "\"${prop("radioUrl") ?: "http://radio.example.lan/"}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            prop("keystore")?.let {
                storeFile = file(it)
                storePassword = prop("storePassword")
                keyAlias = "signal"
                keyPassword = prop("storePassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (prop("keystore") != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
