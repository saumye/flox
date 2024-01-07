import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("ai.flox.library")
    id("ai.flox.feature")
}

android {
    namespace = "ai.flox.network"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "OPENAI_APIKEY", "\"" + gradleLocalProperties(rootDir).getProperty("OPENAI_APIKEY", "") + "\"")
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.retrofit.converter.gson)
}