repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

plugins {
    //id("com.github.ben-manes.versions") version "0.47.0"
    kotlin("multiplatform") version Versions.kotlin apply true
    kotlin("plugin.serialization") version Versions.kotlin apply true
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
    jvm {
        jvmToolchain(Versions.jvmTarget.toInt())
    }
    js(IR) {
        browser {

        }
    }
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
            }
        }
        val jvmMain by getting {
            dependencies {
                api(fileTree("libs") {
                    include("*.jar")
                })
                api("com.squareup.okio:okio:3.6.0")
                api("com.github.ajalt.mordant:mordant:2.2.0")
            }
        }
        val jsMain by getting
        val nativeMain by getting
    }
}
