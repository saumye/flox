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
        buildConfigField("String", "OPENAIAPI_KEY", "\"" + gradleLocalProperties(rootDir).getProperty("OPENAIAPI_KEY", "") + "\"")
        buildConfigField("String", "NEWSAPI_KEY", "\"" + gradleLocalProperties(rootDir).getProperty("NEWSAPI_KEY", "") + "\"")
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.retrofit.converter.gson)
}