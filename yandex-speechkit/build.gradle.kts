import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.google.protobuf")
    kotlin("kapt")
}

android {
    defaultConfig {
        minSdkVersion(21)
        buildConfigField("String", "TOKEN_API_URL", "\"https://iam.api.cloud.yandex.net/iam/v1/tokens\"")
    }
}

dependencies {
    implementation("com.justai.aimybox:core:${Versions.aimybox}")

    implementation(Libraries.Android.appCompat)
    implementation(Libraries.Kotlin.stdLib)
    batchImplementation(Libraries.Kotlin.coroutines)

    implementation("com.squareup.okhttp3:okhttp:${Versions.okHttp}")
    implementation("com.squareup.okhttp3:logging-interceptor:${Versions.okHttp}")

    implementation("io.grpc:grpc-okhttp:1.19.0")
    implementation("io.grpc:grpc-protobuf-lite:1.19.0")
    implementation("io.grpc:grpc-stub:1.19.0")
    compileOnly("javax.annotation:javax.annotation-api:1.2")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.0.0"
    }
    plugins {
        id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.19.0" }
        id("javalite") { artifact = "com.google.protobuf:protoc-gen-javalite:3.0.0" }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("javalite")
                id("grpc") { option("lite") }
            }
            task.builtins {
//                remove(id("java"))
            }
        }
    }
}