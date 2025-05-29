repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

plugins {
    //id("com.github.ben-manes.versions") version "0.47.0"
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

/*tasks.withType<DependencyUpdatesTask> {
    fun isNonStable(version: String): Boolean {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(version)
        return isStable.not()
    }

    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}*/

group = "shared"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(Versions.jvmTarget.toInt())

    jvm()
    js(IR) {
        browser {

        }
    }

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
        }
        jvmMain.dependencies {
            api(fileTree("libs") {
                include("*.jar")
            })
            api("com.squareup.okio:okio:3.12.0")
            api("com.github.ajalt.mordant:mordant:3.0.2")
            api("net.openhft:zero-allocation-hashing:0.16")
            api("org.slf4j:slf4j-api:2.0.17")
        }
    }
}
