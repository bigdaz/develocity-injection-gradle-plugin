plugins {
    `java-gradle-plugin`
    groovy
    alias(libs.plugins.plugin.publish)
}

group = "com.gradle"
version = releaseVersion().get()

repositories {
    mavenCentral()
}

gradlePlugin {
    val greeting by plugins.creating {
        id = "com.gradle.develocity-injection-gradle-plugin"
        implementationClass = "com.gradle.DevelocityInjectionGradlePlugin"
    }
}

val localRepo = rootProject.layout.buildDirectory.dir("local-repo")

publishing {
    publications {
        withType<MavenPublication>() {
            artifactId = "develocity-injection-gradle-plugin"
        }
    }
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
