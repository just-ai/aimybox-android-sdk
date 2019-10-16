object Plugin {
    val kotlinGradle = "org.jetbrains.kotlin:kotlin-gradle-plugin" version { kotlin }
    val dokka = "org.jetbrains.dokka:dokka-gradle-plugin" version { dokka }

    val androidGradle = "com.android.tools.build:gradle" version { androidPlugin }

    val dexcount = "com.getkeepsafe.dexcount:dexcount-gradle-plugin" version { dexCountPlugin }

    val bintray = "com.jfrog.bintray.gradle:gradle-bintray-plugin" version { bintrayPlugin }
    val buildInfoExtractor = "org.jfrog.buildinfo:build-info-extractor-gradle" version { bintrayBuildInfoPlugin }

    val protobuf = "com.google.protobuf:protobuf-gradle-plugin" version { protobufPlugin }
}