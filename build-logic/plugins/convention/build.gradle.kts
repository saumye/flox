@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    `kotlin-dsl`
}

group = "ai.floxbuildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "ai.flox.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "ai.flox.library"
            implementationClass = "LibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "ai.flox.feature"
            implementationClass = "FeatureConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "ai.flox.compose"
            implementationClass = "ComposeConventionPlugin"
        }

    }
}