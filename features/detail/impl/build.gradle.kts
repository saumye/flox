import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("ai.flox.library")
    id("ai.flox.feature")
    id("ai.flox.compose")
}

android {
    namespace = "ai.flox.detail"
}

dependencies {
    implementation(project(":core:storage"))
    implementation(project(":core:network"))
    implementation(project(":core:base"))
    implementation(project(":features:detail:api"))
    implementation(project(":features:home:api"))
} 