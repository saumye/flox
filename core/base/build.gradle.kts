@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("ai.flox.library")
    id("ai.flox.feature")
}

android {
    namespace = "ai.flox.base"
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.retrofit.converter.gson)
}