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
                srcDir("src/main/protobuf")
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

    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)

    implementation("com.squareup.okhttp3:okhttp" version { okHttp })
    implementation("com.squareup.okhttp3:logging-interceptor" version { okHttp })

    implementation("io.grpc:grpc-okhttp" version { grpc })
    implementation("io.grpc:grpc-protobuf" version { grpc })
    implementation("io.grpc:grpc-stub" version { grpc })

    implementation ("com.google.code.gson:gson:2.8.9")
    compileOnly ("org.apache.tomcat:annotations-api:6.0.53")


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


