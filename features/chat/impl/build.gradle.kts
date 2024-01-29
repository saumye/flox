@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("ai.flox.library")
    id("ai.flox.feature")
    id("ai.flox.compose")
}

android {
    namespace = "ai.flox.chat"
}

dependencies {
    implementation(project(":features:chat:api"))
    implementation(project(":features:conversation:api"))
    implementation(project(":core:storage"))
    implementation(project(":core:network"))
    implementation(project(":core:base"))
}