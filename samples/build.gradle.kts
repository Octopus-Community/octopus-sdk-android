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
    namespace = "com.octopuscommunity.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.octopuscommunity.sample"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField(
            type = "String",
            name = "OCTOPUS_API_KEY",
            value = "\"${localProperties.getProperty("OCTOPUS_API_KEY") ?: ""}\""
        )

        buildConfigField(
            type = "String",
            name = "OCTOPUS_SSO_CLIENT_USER_TOKEN_SECRET",
            value = "\"${localProperties.getProperty("OCTOPUS_SSO_CLIENT_USER_TOKEN_SECRET") ?: ""}\""
        )

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    flavorDimensions += "mode"
    productFlavors {
        create("bottomnavigationbar") {
            dimension = "mode"
            versionNameSuffix = "-bottomnavigationbar"
        }
        create("contentpadding") {
            dimension = "mode"
            versionNameSuffix = "-contentpadding"
        }
        create("fullscreen") {
            dimension = "mode"
            versionNameSuffix = "-fullscreen"
        }
        create("modalbottomsheet") {
            dimension = "mode"
            versionNameSuffix = "-modalbottomsheet"
        }
        create("singleactivity") {
            dimension = "mode"
            versionNameSuffix = "-singleactivity"
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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
    implementation(libs.ui.tooling)
    implementation(libs.ui.tooling.preview)
    // Material3
    implementation(libs.androidx.material3)

    // Activity Compose
    implementation(libs.activity.compose)

    // Navigation Compose
    implementation(libs.navigation.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation(libs.androidx.datastore)

    // Coil
    implementation(libs.coil.network)
    implementation(libs.coil.compose)
}