/*
 * Copyright 2022 Afig Aliyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.android.build.gradle.LibraryExtension
import ai.flox.configureKotlinAndroid
import ai.flox.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class LibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.plugins.androidLibrary.get().pluginId)
            apply(libs.plugins.kotlinAndroid.get().pluginId)
            apply(libs.plugins.kapt.get().pluginId)
            apply(libs.plugins.hiltPlugin.get().pluginId)
        }

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)

            defaultConfig {
                buildTypes {
                    release {
                        isMinifyEnabled = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }
            }

            dependencies {
                add("implementation", libs.hilt)
                add("kapt", libs.hilt.android.compiler)
            }

            packagingOptions.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
            packagingOptions.resources.pickFirsts+="META-INF/gradle/incremental.annotation.processors"

        }
    }
}