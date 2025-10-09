plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose)
}

android {
    namespace = "com.octopuscommunity.tools"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Octopus
    implementation(libs.octopus.sdk)
    implementation(libs.octopus.sdk.ui)

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
}