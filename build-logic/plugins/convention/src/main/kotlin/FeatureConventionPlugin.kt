import ai.flox.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.plugins.kapt.get().pluginId)
            apply(libs.plugins.hiltPlugin.get().pluginId)
        }

        dependencies {
            // AndroidX
            add("implementation",libs.core.ktx)
            add("implementation",libs.appcompat)
            add("implementation",libs.lifecycle.runtime.ktx)

            // Hilt
            add("implementation",libs.hilt)
            add("implementation",libs.hilt.android.compiler)
            add("kapt", libs.hilt.android.compiler)

            // Test
            add("testImplementation",libs.junit)
            add("androidTestImplementation",libs.androidx.test.ext.junit)
            add("androidTestImplementation",libs.espresso.core)

        }
    }
}