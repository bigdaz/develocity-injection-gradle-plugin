plugins {
    `java-gradle-plugin`
    groovy
    alias(libs.plugins.plugin.publish)
}

group = "com.gradle.develocity"
version = releaseVersion().get()

repositories {
    mavenCentral()
}

gradlePlugin {
    val greeting by plugins.creating {
        id = "com.gradle.develocity.develocity-injection"
        implementationClass = "com.gradle.develocity.DevelocityInjectionGradlePlugin"
    }
}

val localRepo = rootProject.layout.buildDirectory.dir("local-repo")

publishing {
    repositories {
        maven {
            name = "local"
            url = localRepo.get().asFile.toURI()
        }
    }
}

fun releaseVersion(): Provider<String> {
    val releaseVersionFile = rootProject.layout.projectDirectory.file("release/version.txt")
    return providers.fileContents(releaseVersionFile).asText.map { it.trim() }
}
