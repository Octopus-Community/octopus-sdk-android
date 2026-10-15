plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.play.publisher) apply false
}

// Preserve the mirror-owned configurator while explaining its retired previews.
// These adapters belong to tools only; they are never SDK or sample API.
project(":tools") {
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.gradle.LibraryExtension> {
            sourceSets.getByName("main").java.srcDir(rootProject.file("theme-preview-compat"))
        }
    }
}
