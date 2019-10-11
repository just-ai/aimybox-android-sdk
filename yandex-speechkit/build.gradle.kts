import com.google.protobuf.gradle.*
import com.justai.gradle.project.configureProject

plugins {
    id("com.android.library")
    id("com.google.protobuf")
}

configureProject {
    isLibrary = true
    isMavenPublication = true
    publishToBintray = true
}

android {
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
        artifact = "com.google.protobuf:protoc:3.9.1"
    }
    plugins {
        id("javalite") { artifact = "com.google.protobuf:protoc-gen-javalite:3.0.0" }
        id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.24.0" }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc") { option("lite") }
                id("javalite")
            }
        }
    }
}