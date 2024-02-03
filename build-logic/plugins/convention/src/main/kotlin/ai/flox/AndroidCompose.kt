package ai.flox

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Configure Compose-specific options.
 */
internal fun Project.configureAndroidCompose(commonExtension: CommonExtension<*, *, *, *, *>) =
    with(commonExtension) {
        defaultConfig.vectorDrawables.useSupportLibrary = true
        buildFeatures.compose = true
        composeOptions.kotlinCompilerExtensionVersion = "1.5.2"

        dependencies {
            add("implementation", platform(libs.compose.bom))
            add("androidTestImplementation", platform(libs.compose.bom))
            add("implementation", libs.ui)
            add("implementation", libs.ui.graphics)
            add("implementation", libs.ui.tooling)
            add("implementation", libs.ui.tooling.preview)
            add("implementation", libs.ui.test.manifest)
            add("implementation", libs.ui.test.junit4)
            add("implementation", libs.compose.material)
            add("implementation",libs.hilt.navigation.compose)
            add("implementation",libs.lifecycle.runtime.compose)
            add("implementation",libs.androidx.ui.tooling)
            add("implementation",libs.androidx.constraintlayout)
            add("implementation",libs.constraintlayout.compose)
            add("implementation",libs.coil)

        }
    }