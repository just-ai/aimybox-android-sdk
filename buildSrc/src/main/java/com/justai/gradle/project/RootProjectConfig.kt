package com.justai.gradle.project

import kotlin.properties.Delegates

class RootProjectConfig {
    var kotlinVersion: String by Delegates.notNull()
    var version: String by Delegates.notNull()
    var versionCode: Int by Delegates.notNull()
    var minSdk: Int by Delegates.notNull()
    var compileSdk: Int by Delegates.notNull()
}