pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}


rootProject.name = "Flox-Sample"
include(":app")
include(":features:chat:api")
include(":features:chat:impl")
include(":core:network")
include(":core:storage")
include(":core:base")
include(":features:conversation:api")
include(":features:conversation:impl")
include(":features:home:api")
include(":features:home:impl")

