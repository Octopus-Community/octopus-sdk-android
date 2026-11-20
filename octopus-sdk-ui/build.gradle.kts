import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.screenshot)
}

val sdkVersionCode = (findProperty("SDK_VERSION_CODE") as String).toInt()
val sdkVersionName = findProperty("SDK_VERSION_NAME") as String

android {
    namespace = "com.octopuscommunity.sdk.ui"
    compileSdk = 36

    // Host-side Compose Preview Screenshot Testing — enables the screenshotTest source set.
    // Required in addition to the `android.experimental.enableScreenshotTest` gradle.properties flag.
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    defaultConfig {
        minSdk = 23

        buildConfigField(
            "int",
            "SDK_VERSION_CODE",
            "$sdkVersionCode",
        )

        buildConfigField(
            "String",
            "SDK_VERSION_NAME",
            "\"$sdkVersionName\"",
        )

        buildConfigField(
            "String",
            "CONTACT_EMAIL",
            "\"contact@octopuscommunity.com\"",
        )

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
    flavorDimensions += listOf("environment")

    productFlavors {
        create("demo") {
            dimension = "environment"
        }
        create("prod") {
            dimension = "environment"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        // Small tolerance to absorb sub-pixel anti-aliasing / font-rendering jitter across hosts.
        screenshotTests {
            imageDifferenceThreshold = 0.0001f // 0.01%
        }
        // Robolectric needs the merged manifest and resources to host a real ComponentActivity in
        // a JVM unit test (OctopusLocaleWrapperTest composes against one).
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    mavenPublishing {
        configure(
            AndroidSingleVariantLibrary(
                // the published variant
                variant = "prodRelease",
                sourcesJar = true,
                publishJavadocJar = true,
            ),
        )
        publishToMavenCentral()
        coordinates(
            groupId = "com.octopuscommunity",
            artifactId = "octopus-sdk-ui",
            version = sdkVersionName,
        )
        pom {
            name.set("OctopusSDK UI")
            description.set("Octopus Community SDK UI for Android")
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
    // Octopus
    implementation(project(":octopus-sdk"))
    implementation(project(":octopus-sdk-data"))

    // Ktx
    implementation(libs.androidx.core.ktx)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.ui.tooling)
    implementation(libs.ui.tooling.preview)
    // Material3
    implementation(libs.androidx.material3)
    // Icons
    implementation(libs.androidx.material.icons.extended)

    // Activity Compose
    implementation(libs.activity.compose)

    // Navigation Compose
    implementation(libs.navigation.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)

    // Coil
    implementation(libs.coil.compose.core)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.media3.ui.compose.material3)

    // Chrome Custom Tabs
    implementation(libs.androidx.browser)

    // Zoomable image
    implementation(libs.zoomable.image.coil3)
    // Flick-to-dismiss for the full-screen image viewer (same author and version as telephoto)
    implementation(libs.telephoto.flick)

    // Markdown
    implementation(libs.compose.richtext)
    implementation(libs.compose.richtext.material3)

    // Test
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    // JUnit4 rule support (MainDispatcherRule) — kotlin.test alone doesn't provide TestWatcher/@Rule.
    testImplementation(libs.junit)
    // Compose composition inside a real Activity on the JVM — the context-chain contract of
    // OctopusLocaleWrapper (internal ticket) is only observable from a composition hosted by an Activity.
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.ui.test.junit4)
    testImplementation(libs.ui.test.manifest)

    // Compose Preview Screenshot Testing (host-side, screenshotTest source set only — NOT published)
    screenshotTestImplementation(platform(libs.compose.bom))
    screenshotTestImplementation(libs.ui.tooling)
    screenshotTestImplementation(libs.screenshot.validation.api)
}

// Self-contained HTML gallery of the screenshot suite (references vs the images the last validate
// run rendered), built by a stdlib-only python script so it runs on any host with python3. Wired
// as a finalizer of validateDemoDebugScreenshotTest so it ALSO runs when validation fails — which
// is exactly when a reviewer needs the baseline | actual | diff triptychs. It is a report, not a
// gate: statuses (UNCHANGED/CHANGED/NEW/REMOVED/ERROR) never fail the build — an unreadable PNG
// becomes an ERROR entry in the report, exit code 0. Only a setup error (missing/empty input
// trees, refused --output: exit 2) fails the task, deliberately loudly, alongside validation
// itself.
val generateScreenshotGallery by tasks.registering(Exec::class) {
    group = "verification"
    description =
        "Builds build/reports/screenshotGallery/index.html from the last screenshot validate run"

    val renderedDir =
        layout.buildDirectory.dir("outputs/screenshotTest-results/preview/debug/demo/rendered")
    // Skip (instead of failing) when validate died before rendering anything — e.g. a
    // screenshotTest compile error — since there is nothing to report on in that case.
    // ⚠ CI freshness depends on pr.yml's Checkout step keeping actions/checkout's default
    // `clean: true` (git clean -ffdx wipes the gitignored build/ between runs on the persistent
    // self-hosted workspace). With `clean: false` this onlyIf could see a PREVIOUS run's rendered
    // tree and report stale images as this run's. Do not disable clean without adding a
    // same-run freshness check here.
    onlyIf("the validate run rendered at least one image") { renderedDir.get().asFile.isDirectory }

    workingDir = rootDir
    commandLine(
        "python3",
        rootProject.file("scripts/build-screenshot-gallery.py").absolutePath,
        "--reference",
        layout.projectDirectory
            .dir("src/screenshotTestDemoDebug/reference")
            .asFile.absolutePath,
        "--rendered",
        renderedDir.get().asFile.absolutePath,
        "--diffs",
        layout.buildDirectory
            .dir("outputs/screenshotTest-results/preview/debug/demo/diffs")
            .get()
            .asFile.absolutePath,
        "--output",
        layout.buildDirectory
            .dir("reports/screenshotGallery")
            .get()
            .asFile.absolutePath,
    )
}

tasks.matching { it.name == "validateDemoDebugScreenshotTest" }.configureEach {
    finalizedBy(generateScreenshotGallery)
}
