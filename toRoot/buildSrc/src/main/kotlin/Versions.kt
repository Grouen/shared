import org.gradle.api.JavaVersion

object Versions {
    const val kotlin = "1.9.10"

    val jvmTarget = JavaVersion.VERSION_17.toString()
    val encoding = Charsets.UTF_8.toString()
}
