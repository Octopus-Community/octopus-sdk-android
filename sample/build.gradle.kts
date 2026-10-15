import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.play.publisher) apply false
}

val sdkVersionCode = (findProperty("SDK_VERSION_CODE") as String).toInt()
val sdkVersionName = findProperty("SDK_VERSION_NAME") as String
val ciVersionCode = findProperty("ciVersionCode")?.toString()?.toIntOrNull()

// Load Local properties
val localProperties = getProperties("local.properties")

// Load Octopus properties
val octopusProperties = getProperties("octopus.properties")

// Load keystore.properties file
val keystoreProperties = getProperties("keystore.properties")

// Named demo API keys use the `id~label~key` format (see `data/ApiKeyRegistry.kt`).
// When no blob is supplied, compose it from the per-slot `OCTOPUS_SSO_*_API_KEY`
// properties, each falling back to `OCTOPUS_API_KEY` in local build configuration.
val namedApiKeys =
    getLocalProperty("OCTOPUS_NAMED_API_KEYS").ifEmpty { composeNamedApiKeysFromSlotProperties() }

android {
    namespace = "com.octopuscommunity.sdk.sample"
    // Android 16 / API 36. Play refuses any bundle targeting 35 or lower since the
    // 2026-08-31 target-API deadline ("Target SDK of artifact is too low"), so the
    // Play upload on `main` cannot land at 35 any more. Only the sample
    // is bumped — the published library modules keep their own compileSdk, which the
    // deadline does not touch.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.octopuscommunity.sdk.sample"
        minSdk = 23
        targetSdk = 36
        versionCode = ciVersionCode ?: sdkVersionCode
        versionName = sdkVersionName

        // The SDK version this sample embeds. Not derivable from `VERSION_NAME` at runtime: on a
        // CI build `versionCode` is a run-derived number, and the About / Debug info screens are
        // expected to name the SDK version, not the sample's build.
        buildConfigField(
            type = "String",
            name = "OCTOPUS_SDK_VERSION",
            value = "\"$sdkVersionName\"",
        )

        buildConfigField(
            type = "String",
            name = "OCTOPUS_SSO_CLIENT_USER_TOKEN_SECRET",
            value = "\"${getLocalProperty("OCTOPUS_SSO_CLIENT_USER_TOKEN_SECRET")}\"",
        )

        // Seed user id surfaced as the first chip in the LoginScreen quick-pick.
        // Read at build time so a launcher / local tooling wiring can pin the
        // identity the bundled SSO JWT (if any) is signed for. Empty default →
        // the picker just lists the hardcoded predefined ids.
        buildConfigField(
            type = "String",
            name = "OCTOPUS_USER_ID",
            value = "\"${getLocalProperty("OCTOPUS_USER_ID")}\"",
        )

        // Keep the API-key property order; caller environment is the final optional fallback.
        listOf(
            "OCTOPUS_FIXTURE_POST_ID",
            "OCTOPUS_FIXTURE_REACTION_POST_ID",
            "OCTOPUS_FIXTURE_COMMENT_ID",
            "OCTOPUS_FIXTURE_USER_ID",
            "OCTOPUS_FIXTURE_TOPIC_ID",
        ).forEach { key ->
            buildConfigField(
                type = "String",
                name = key,
                value =
                    groovy.json.JsonOutput.toJson(
                        getLocalProperty(key, fallback = providers.environmentVariable(key).orNull.orEmpty()),
                    ),
            )
        }

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    if (!file("internal/build.gradle").exists()) {
        sourceSets.getByName("main").java.srcDir("public/java")
    }

    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        // Robolectric provides real framework behavior for the JVM tests.
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildTypes {
        debug {
//            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
//            isShrinkResources = true
        }
        release {
            signingConfigs {
                create("octopus") {
                    storeFile = rootProject.file("keystore.jks")
                    storePassword = (keystoreProperties["KEYSTORE_PASSWORD"] as? String?) ?: ""
                    keyAlias = (keystoreProperties["KEYSTORE_KEY_ALIAS"] as? String?) ?: ""
                    keyPassword = (keystoreProperties["KEYSTORE_KEY_PASSWORD"] as String?) ?: ""
                }
            }

            isMinifyEnabled = false
//            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("octopus")
        }
        create("ciMinified") {
            // Exercise the libraries' consumer rules without changing Play or local debug builds.
            initWith(getByName("release"))
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            versionNameSuffix = "-ci"
            matchingFallbacks += "release"
        }
    }

    flavorDimensions += listOf("environment")

    productFlavors {
        create("demo") {
            dimension = "environment"
            buildConfigField("Boolean", "IS_PROD_FLAVOR", "false")

            // Wire-format set of named demo API keys for the Config-screen picker and the
            // Switch-Community scenario presets. Mirrors the Flutter
            // `--dart-define=OCTOPUS_NAMED_API_KEYS` injection: entries joined by `;`, fields by
            // `~`, in `id~label~key` order. Empty default -> no presets, the scenario falls back to
            // the free-text key field.
            //
            // Declared on this flavor alone, like the other demo-only settings: the blob holds API
            // keys, and a `buildConfigField` value is readable in the packaged dex of any variant
            // that declares it. `prod` declares the field empty so `src/main` still compiles.
            buildConfigField(
                type = "String",
                name = "OCTOPUS_NAMED_API_KEYS",
                value = "\"$namedApiKeys\"",
            )
        }
        create("prod") {
            dimension = "environment"
            matchingFallbacks += "prod"
            buildConfigField("Boolean", "IS_PROD_FLAVOR", "true")
            // No named demo keys in a prod artifact (see the demo flavor).
            buildConfigField("String", "OCTOPUS_NAMED_API_KEYS", "\"\"")
        }
    }

    compileOptions {
//        isCoreLibraryDesugaringEnabled = true
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

androidComponents {
    beforeVariants(selector().withBuildType("ciMinified")) { variant ->
        // Opt in only for the CI smoke build; do not double the normal build/test matrix.
        variant.enable =
            providers.gradleProperty("ciMinified").orNull == "true" &&
            variant.productFlavors.any { it.second == "demo" }
    }
}

if (providers.gradleProperty("ciMinified").orNull == "true") {
    tasks.register("verifyDemoCiMinifiedConsumerRules") {
        group = "verification"
        description = "Verify the four SDK consumer rules and kept sentinels in R8's reports."
        dependsOn("assembleDemoCiMinified")

        doLast {
            val reports =
                layout.buildDirectory
                    .dir("outputs/mapping/demoCiMinified")
                    .get()
                    .asFile
            val configuration = reports.resolve("configuration.txt").readText()
            val sections = configuration.split("# The proguard configuration file for the following section is ").drop(1)

            // Ignore formatting and comments, but require every module's complete rule body.
            fun normalizeRules(text: String) =
                text.lineSequence().joinToString("") { it.substringBefore('#').filterNot(Char::isWhitespace) }

            listOf("octopus-sdk-domain", "octopus-sdk-data", "octopus-sdk", "octopus-sdk-ui").forEach { module ->
                // Match only the module-relative path: Gradle can restore reports from another checkout's cache.
                val section =
                    sections.singleOrNull {
                        it.substringBefore('\n').replace('\\', '/').contains("/$module/build/intermediates/consumer_proguard_dir/")
                    }
                check(section != null) { "R8 configuration is missing consumer rules from $module" }
                val rules = normalizeRules(rootProject.file("$module/consumer-rules.pro").readText())
                check(rules.isNotEmpty() && normalizeRules(section.substringAfter('\n')).contains(rules)) {
                    "R8 configuration does not contain the complete consumer rules from $module"
                }
            }

            // Exact class-only seed lines, not member lines or a name mentioned in a rule.
            // CommonProto.Empty is a generated protobuf GeneratedMessageLite subclass.
            val seeds = reports.resolve("seeds.txt").readLines().toSet()
            listOf(
                "com.octopuscommunity.CommonProto\$Empty",
                "com.google.common.base.Strings",
                "io.grpc.internal.GrpcUtil",
                "com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase",
                "com.octopuscommunity.sdk.ui.OctopusDestination\$PostDetails",
            ).forEach { sentinel ->
                check(sentinel in seeds) { "R8 seeds are missing kept sentinel: $sentinel" }
            }
            logger.lifecycle("R8 consumer rules verified: all four SDK modules and all five kept sentinels")
        }
    }
}

dependencies {
    // Octopus
    implementation(libs.octopus.sdk)
    implementation(libs.octopus.sdk.ui)

    // AndroidX
    implementation(libs.androidx.core.ktx)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.ui.tooling)
    implementation(libs.ui.tooling.preview)

    // Activity Compose
    implementation(libs.activity.compose)

    // Navigation Compose
    implementation(libs.navigation.compose)

    // Material3
    implementation(libs.androidx.material3)

    // Icons
    implementation(libs.androidx.material.icons.extended)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // Play In-App Update
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    // Unit tests (JVM) — named-key parser, Config-screen preset resolution, feedback URL budget,
    // and the Live community config panel's state machine (coroutines-test drives its poll loop on
    // virtual time, so the tests never actually wait a second per tick)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.ui.test.junit4)

//    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}

fun getProperties(path: String) =
    Properties().apply {
        val file = rootProject.file(path)
        if (file.exists()) {
            // Read as UTF-8 (Properties.load(InputStream) would assume ISO-8859-1 and
            // mangle non-ASCII values — e.g. the "·" in the Switch-Community preset
            // labels packed into OCTOPUS_NAMED_API_KEYS when octopus.properties is the
            // source, as in the CI/Internal-Testing build).
            file.reader(Charsets.UTF_8).use { load(it) }
        }
    }

fun getLocalProperty(
    key: String,
    fallback: String = "",
): String =
    findProperty(key)?.toString()?.ifEmpty { null }
        ?: localProperties.getProperty(key)?.ifEmpty { null }
        ?: octopusProperties.getProperty(key)?.ifEmpty { null }
        ?: fallback

/**
 * Rebuilds the `OCTOPUS_NAMED_API_KEYS` wire blob from the per-slot properties, for the local
 * `octopus.properties` setup. Slot ids mirror `AuthPreset.Companion`.
 *
 * Labels are deliberately emitted EMPTY (`id~~key`): `AuthPreset.label` already falls back to its
 * committed label when the injected one is blank, so this path never becomes a second naming
 * source that could drift from the launcher's.
 */
fun composeNamedApiKeysFromSlotProperties(): String {
    val defaultApiKey = getLocalProperty("OCTOPUS_API_KEY")
    return listOf(
        "ssoNoManagedNoForced" to "OCTOPUS_SSO_NO_MANAGED_FIELDS_NO_FORCED_LOGIN_API_KEY",
        "ssoNoManagedForced" to "OCTOPUS_SSO_NO_MANAGED_FIELDS_FORCED_LOGIN_API_KEY",
        "ssoAllManagedForced" to "OCTOPUS_SSO_ALL_MANAGED_FIELDS_FORCED_LOGIN_API_KEY",
        "ssoSomeManagedForced" to "OCTOPUS_SSO_SOME_MANAGED_FIELDS_FORCED_LOGIN_API_KEY",
    ).mapNotNull { (slotId, property) ->
        val key = getLocalProperty(key = property, fallback = defaultApiKey)
        // `;` and `~` are the wire delimiters and `"`/`\` would break the generated Java string
        // literal. A base64url-shaped key contains none of them, so skipping is never expected to
        // fire — it just makes a malformed value drop its own slot instead of corrupting the blob.
        when {
            key.isEmpty() -> null
            key.any { it in ";~\"\\" } -> {
                logger.warn("Skipping named API key slot '$slotId': unsupported delimiter or string escape character.")
                null
            }
            else -> "$slotId~~$key"
        }
    }.joinToString(";")
}

// The standalone sample uses the default implementations in main.
if (file("internal/build.gradle").exists()) {
    apply(from = "internal/build.gradle")
} else if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Source-level injection assertions must rerun when the Gradle injection path changes.
tasks.withType<Test>().configureEach {
    inputs
        .file(file("build.gradle.kts"))
        .withPropertyName("apiKeyInjectionGradleSource")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
