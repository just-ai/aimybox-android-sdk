import com.google.protobuf.gradle.*
import com.justai.gradle.project.configureAndroid
import com.justai.gradle.project.configureProject

plugins {
    id("com.android.library")
    id("com.google.protobuf")
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
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":core"))

    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)

    implementation("com.squareup.okhttp3:okhttp" version { okHttp })
    implementation("com.squareup.okhttp3:logging-interceptor" version { okHttp })

    implementation("io.grpc:grpc-okhttp" version { grpc })
    implementation("io.grpc:grpc-protobuf" version { grpc })
    implementation("io.grpc:grpc-stub" version { grpc })

    implementation ("com.google.code.gson:gson:2.8.5")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    implementation ("com.google.protobuf:protobuf-java:3.17.2")
}


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.15.2"
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

//    implementation("io.grpc:grpc-okhttp:1.42.1")
//    //implementation("io.grpc:grpc-protobuf-lite:1.42.1")
//    implementation("io.grpc:grpc-stub:1.42.1")
//
//    implementation("io.grpc:grpc-kotlin-stub:1.2.0")
//    implementation("io.grpc:grpc-protobuf:1.42.1")
//    implementation("com.google.protobuf:protobuf-kotlin:3.19.1")
//}
//
//tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
//    kotlinOptions {
//        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
//    }
//}
//
//protobuf {
//    protoc {
//        artifact = "com.google.protobuf:protoc:3.19.1"
//    }
//    plugins {
////        id("java") {
////            artifact = "io.grpc:protoc-gen-grpc-java:1.42.1"
////        }
//        id("grpc") {
//            artifact = "io.grpc:protoc-gen-grpc-java:1.42.1"
//        }
//        id("grpckt") {
//            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.2.0:jdk7@jar"
//        }
//    }
//    generateProtoTasks {
//        all().forEach {
//            it.plugins {
//                id("java") {
//                    option("lite")
//                }
//                id("grpc") {
//                    option("lite")
//                }
//                id("grpckt") {
//                    option("lite")
//                }
//            }
//            it.builtins {
//                id("kotlin") {
//                    option("lite")
//                }
//            }
//        }
//    }
//}