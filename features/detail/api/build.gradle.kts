plugins {
    id("ai.flox.library")
    id("ai.flox.feature")
    id("ai.flox.compose")
}

android {
    namespace = "ai.flox.detail"
}

dependencies {
    implementation(project(":core:base"))
    implementation(project(":core:tts"))
    implementation(project(":features:home:api"))
} 