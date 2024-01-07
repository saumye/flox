package ai.flox

import com.android.build.api.dsl.CommonExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

/**
 * Configure base Kotlin with Android options.
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *>) =
    with(commonExtension) {
        compileSdk = libs.versions.compilesdk.get().toInt()

        defaultConfig {
            minSdk = libs.versions.minsdk.get().toInt()
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }

private fun CommonExtension<*, *, *, *, *>.kotlinOptions(block: KotlinJvmOptions.() -> Unit) =
    (this as ExtensionAware).extensions.configure("kotlinOptions", block)


/**
 * An extension for easy access to the version catalog.
 */
internal val Project.libs get() = the<LibrariesForLibs>()