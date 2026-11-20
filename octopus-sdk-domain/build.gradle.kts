import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.binary.compatibility.validator)
}

val sdkVersionName = findProperty("SDK_VERSION_NAME") as String

android {
    namespace = "com.octopuscommunity.sdk.domain"
    compileSdk = 36

    defaultConfig {
        minSdk = 23

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    mavenPublishing {
        configure(
            AndroidSingleVariantLibrary(
                // the published variant
                variant = "release",
                sourcesJar = true,
                publishJavadocJar = true,
            ),
        )
        publishToMavenCentral()
        coordinates(
            groupId = "com.octopuscommunity",
            artifactId = "octopus-sdk-domain",
            version = sdkVersionName,
        )

        pom {
            name.set("OctopusSDK Domain")
            description.set("Octopus Community SDK Domain for Android")
            inceptionYear.set("2025")
            url.set("https://github.com/octopus-community/octopus-sdk-android")
            licenses {
                license {
                    name.set("Proprietary Commercial License")
                    url.set("https://github.com/Octopus-Community/octopus-sdk-android/blob/main/LICENSE.md")
                    distribution.set("manual")
                    comments.set("This software requires a paid license agreement. Contact sales for details.")
                }
            }
            developers {
                developer {
                    id.set("octopus-community")
                    name.set("Octopus-Community")
                    url.set("https://github.com/octopus-community")
                }
            }
            scm {
                url.set("https://github.com/octopus-community/octopus-sdk-android")
                connection.set("scm:git:git://github.com/octopus-community/octopus-sdk-android.git")
                developerConnection.set("scm:git:ssh://git@github.com/octopus-community/octopus-sdk-android.git")
            }
        }

        signAllPublications()
    }
}

dependencies {
    // Ktx
    implementation(libs.androidx.core.ktx)

    // Test
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
