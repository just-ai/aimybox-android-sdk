//plugins {
//    id("com.android.library")
//    id("org.jetbrains.kotlin.android")
//}
//
//android {
//    compileSdk = 32
//
//    defaultConfig {
//        minSdk = 21
//        targetSdk = 32
//
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        consumerProguardFiles("consumer-rules.pro")
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_1_8
//        targetCompatibility = JavaVersion.VERSION_1_8
//    }
//    kotlinOptions {
//        jvmTarget = "1.8"
//    }
//}
//
//dependencies {
//
//    implementation("androidx.core:core-ktx:1.7.0")
//    implementation("androidx.appcompat:appcompat:1.5.0")
//    implementation("com.google.android.material:material:1.6.1")
//    testImplementation("junit:junit:4.13.2")
//    androidTestImplementation("androidx.test.ext:junit:1.1.3")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
//}

import com.google.protobuf.gradle.*
import com.justai.gradle.project.configureAndroid
import com.justai.gradle.project.configureProject

plugins {
    id("com.android.library")
    id("com.google.protobuf") version "0.8.18"
    `aimybox-publish`
}

project.configureProject {
    isLibrary = true
    createMavenPublication = true
}

project.configureAndroid {
    defaultConfig {
        buildConfigField(
            "String",
            "TOKEN_API_URL",
            "\"https://iam.api.cloud.yandex.net/iam/v1/tokens\""
        )
    }

    sourceSets{
        getByName("main"){
            proto {
                srcDir("src/main/proto")
            }
        }
    }
}

rootProject.tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {

    implementation(project(":core"))

    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)
    implementation(Library.Android.appCompat)

    implementation("com.squareup.okhttp3:okhttp" version { okHttp })
    implementation("com.squareup.okhttp3:logging-interceptor" version { okHttp })

    implementation ("com.google.code.gson:gson:2.8.9")
    compileOnly ("org.apache.tomcat:annotations-api:6.0.53")

    implementation ("com.auth0:java-jwt:4.0.0")
    implementation ("commons-codec:commons-codec:1.15")


    for (notation in (Library.Test.instrumentedTest + Library.Test.unitTest)) {
        androidTestImplementation(notation)
        testImplementation(notation)
    }

}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.19.4"

    }
    plugins {
        id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java" version { grpc } }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }
            }
            task.plugins {
                id("grpc") {
                    option("lite")
                }
            }
        }
    }
}


