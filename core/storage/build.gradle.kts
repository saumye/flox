@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("ai.flox.library")
    id("ai.flox.feature")
}

android {
    namespace = "ai.flox.storage"
}

dependencies {
    implementation(libs.room)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
}