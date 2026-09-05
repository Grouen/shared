repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

plugins {
    kotlinMultiplatform()
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
    jvm()
    js {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            coroutines()
            serialization()
        }
        jvmMain.dependencies {
            api(fileTree("libs") {
                include("*.jar")
            })
            sharedJvm()
        }
    }
}
