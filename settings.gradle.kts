pluginManagement {
    repositories {
//        mavenLocal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
//        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "Octopus SDK Samples"
include(
    ":samples:octopus-auth:fullscreen",
    ":samples:octopus-auth:embed",
    ":samples:sso:octopus-profile",
    ":samples:sso:hybrid-profile",
    ":samples:sso:client-profile"
)
 