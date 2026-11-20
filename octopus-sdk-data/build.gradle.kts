import com.google.protobuf.gradle.id
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import org.gradle.process.CommandLineArgumentProvider

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.maven.publish)
}

val sdkVersionCode = (findProperty("SDK_VERSION_CODE") as String).toInt()
val sdkVersionName = findProperty("SDK_VERSION_NAME") as String

android {
    namespace = "com.octopuscommunity.sdk.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 23

        consumerProguardFiles("consumer-rules.pro")

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
            "int",
            "DATABASE_VERSION",
            "121",
        )
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
    buildFeatures {
        buildConfig = true
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
            artifactId = "octopus-sdk-data",
            version = sdkVersionName,
        )

        pom {
            name.set("OctopusSDK Data")
            description.set("Octopus Community SDK Data for Android")
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

val mockitoAgent =
    configurations.create("mockitoAgent") {
        isCanBeConsumed = false
    }

// Room tests can initialize Mockito inside Robolectric before any plain JVM test does. Explicit
// instrumentation avoids relying on Byte Buddy's dynamic self-attachment in that test JVM.
abstract class MockitoAgentProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val agentJar: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> = listOf("-javaagent:${agentJar.singleFile.absolutePath}")
}

tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(objects.newInstance<MockitoAgentProvider>().apply { agentJar.from(mockitoAgent) })
}

dependencies {
    api(project(":octopus-sdk-domain"))

    // Ktx
    implementation(libs.androidx.core.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Protobuf
    implementation(libs.protobuf.kotlin.lite)

    // gRPC
    implementation(libs.grpc.stub)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.grpc.protobuf.lite)
    implementation(libs.grpc.android)
    implementation(libs.grpc.okhttp)

    // Security
    implementation(libs.androidx.security.crypto)

    // Coil
    implementation(libs.coil.core)
    implementation(libs.coil.network)

    // Exif
    implementation(libs.androidx.exifinterface)

    // Test
    testImplementation(libs.robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    mockitoAgent(libs.mockito.core) { isTransitive = false }
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.grpc.inprocess)
}

// --- Binary Compatibility Validator is intentionally NOT applied to this module -----------
// ~89% of any dump here is generated protobuf/gRPC code in the raw `com.octopuscommunity.*`
// package (produced below), which re-dumps by the thousand lines on every proto regen and
// buries real hand-written changes. Its signal is otherwise thin: this is an implementation
// module, consumed only as a runtime-only (`implementation`) transitive dependency, so its
// types are off a plain integrator's compile classpath. The one public exception is
// `OctopusGrpcClient` (via `OctopusSDK.grpcClient`) — dropping BCV leaves its 8 public members
// (constructor, getUnaryChannel, shutdown, hasAccessToCommunity, and the userRepository /
// preferencesRepository / sessionRepository / languageRepository getters) ungated. Accepted:
// that surface has no known consumer (only internal Koin wiring references it — the owned
// bridges use the separate `OctopusSDK.hasAccessToCommunity` Flow, not `grpcClient`), the
// property itself stays gated by octopus-sdk's dump, and the repository getters return domain
// interfaces gated by octopus-sdk-domain (though the getter members themselves — their names and
// signatures on OctopusGrpcClient — are, like the rest of this surface, now ungated). The
// published API gate lives on octopus-sdk-domain and octopus-sdk.
protobuf {
    protoc {
        artifact =
            libs.protoc
                .asProvider()
                .get()
                .toString()
    }
    plugins {
        create("java") {
            artifact =
                libs.protoc.gen.grpc.java
                    .get()
                    .toString()
        }
        create("grpc") {
            artifact =
                libs.protoc.gen.grpc.java
                    .get()
                    .toString()
        }
        create("grpckt") {
            artifact = libs.protoc.gen.grpc.kotlin
                .get()
                .toString() + ":jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("java") {
                    option("lite")
                }
                id("grpc") {
                    option("lite")
                }
                id("grpckt") {
                    option("lite")
                }
            }
            task.builtins {
                id("kotlin")
            }
        }
    }
}
