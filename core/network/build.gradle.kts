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
        buildConfigField("String", "OPENAIAPI_KEY", "\"" + "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" + "\"") // gradleLocalProperties(rootDir).getProperty("OPENAIAPI_KEY", "") + "\"")
        buildConfigField("String", "NEWSAPI_KEY", "\"" + "\"") // gradleLocalProperties(rootDir).getProperty("NEWSAPI_KEY", "") + "\"")
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.moshi)
    implementation(libs.retrofit.converter.moshi)
    kapt(libs.moshi.codegen)
}