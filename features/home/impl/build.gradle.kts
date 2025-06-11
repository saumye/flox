@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("ai.flox.library")
    id("ai.flox.feature")
    id("ai.flox.compose")
}

android {
    namespace = "ai.flox.home"
}

dependencies {
    implementation(project(":features:home:api"))
    implementation(project(":core:storage"))
    implementation(project(":core:network"))
    implementation(project(":core:base"))
    implementation(project(":core:tts"))
    
    // Add accompanist pager for swipeable tabs
    implementation("com.google.accompanist:accompanist-pager:0.28.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.28.0")
}