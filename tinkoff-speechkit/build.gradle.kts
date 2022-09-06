import com.google.protobuf.gradle.*
import com.justai.gradle.project.configureAndroid
import com.justai.gradle.project.configureProject
import java.util.*
import java.io.File
import java.io.FileInputStream

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

        val properties = Properties()
        properties.load(FileInputStream(File(rootProject.rootDir, "local.properties")))
        buildConfigField("String", "tinkoffApiKey", properties.getProperty("taipkey"))
        buildConfigField("String", "tinkoffSecretKey", properties.getProperty("tsecretkey"))

        testInstrumentationRunner ("androidx.test.runner.AndroidJUnitRunner")
    }

    configurations {
        androidTestImplementation {
            exclude ("io.mockk", "mockk-agent-jvm")
            packagingOptions {
                resources.excludes.add ("META-INF/INDEX.LIST")
            }
        }
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


