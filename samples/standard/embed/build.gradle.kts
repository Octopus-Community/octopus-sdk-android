import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Load Local properties
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.octopuscommunity.sample.embed"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.octopuscommunity.sample.embed"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField(
            type = "String",
            name = "OCTOPUS_API_KEY",
            value = "\"${localProperties.getProperty("OCTOPUS_API_KEY") ?: ""}\""
        )

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Octopus
    implementation(libs.octopus.sdk)
    implementation(libs.octopus.sdk.ui)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    debugImplementation(libs.ui.tooling)
    implementation(libs.ui.tooling.preview)

    // Material3
    implementation(libs.androidx.material3)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
}