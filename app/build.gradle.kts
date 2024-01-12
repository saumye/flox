@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("ai.flox.application")
    id("ai.flox.feature")
    id("ai.flox.compose")
}

android {
    namespace = "ai.flox"

    defaultConfig {
        applicationId = "ai.flox"
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures.buildConfig = true

    configurations {
        all {
            exclude(group = "com.intellij", module = "annotations")
        }
    }
}

dependencies {
    implementation(project(":features:chat:api"))
    implementation(project(":features:conversation:api"))
    implementation(project(":core:storage"))
    implementation(project(":core:base"))
}