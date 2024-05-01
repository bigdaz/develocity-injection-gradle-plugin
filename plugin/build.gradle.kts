plugins {
    `java-gradle-plugin`
    groovy
    alias(libs.plugins.plugin.publish)
}

repositories {
    mavenCentral()
}

gradlePlugin {
    val greeting by plugins.creating {
        id = "com.gradle.develocity-injection"
        implementationClass = "com.gradle.DevelocityInjectionGradlePlugin"
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
