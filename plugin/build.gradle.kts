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
