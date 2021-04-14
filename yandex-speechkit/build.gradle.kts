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

dependencies {
    implementation(project(":core"))

    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)

    implementation("com.squareup.okhttp3:okhttp" version { okHttp })

    implementation("com.squareup.okhttp3:logging-interceptor" version { okHttp })
    implementation("io.grpc:grpc-okhttp" version { grpc })
    implementation("io.grpc:grpc-protobuf-lite" version { grpc })
    implementation("io.grpc:grpc-stub" version { grpc })

    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
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