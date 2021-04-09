package com.justai.gradle.project

import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.KotlinBuildScript
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

fun Project.configureAndroid(closure: BaseExtension.() -> Unit) {
    apply(plugin = "kotlin-android")
    apply(plugin = "kotlin-kapt")
//    apply(plugin = "com.getkeepsafe.dexcount")

    val config = rootProjectConfig

    configure<BaseExtension> {
        compileSdkVersion(config.compileSdk)

        defaultConfig {
            minSdkVersion(config.minSdk)
            targetSdkVersion(config.compileSdk)

            versionName = config.version
            versionCode = config.versionCode
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        lintOptions {
            isCheckAllWarnings = true
            isAbortOnError = false
        }

        buildTypes {
            getByName("debug") {
                lintOptions {
                    isWarningsAsErrors = false
                }
            }
            getByName("release") {
                lintOptions {
                    isWarningsAsErrors = true
                }
            }
        }
        closure(this)
    }
}