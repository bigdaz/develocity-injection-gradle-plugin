plugins {
    `java-gradle-plugin`
    groovy
    alias(libs.plugins.plugin.publish)
}

group = "com.gradle"
version = releaseVersion().get()

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    compileOnly("com.gradle:develocity-gradle-plugin:3.17.2")
    compileOnly("com.gradle:common-custom-user-data-gradle-plugin:2.0.1")

    testImplementation(gradleTestKit())
    testImplementation("org.spockframework:spock-core:2.3-groovy-3.0")
    testImplementation("org.spockframework:spock-junit4:2.3-groovy-3.0")

    testImplementation("io.ratpack:ratpack-groovy-test:1.9.0") {
        exclude(group = "org.codehaus.groovy", module = "groovy-all")
    }
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile:2.17.0")
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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.named<Test>("test") {
    dependsOn(tasks.publish)
    useJUnitPlatform()
    workingDir = projectDir
    systemProperty("local.repo", localRepo.get().asFile.absolutePath)
    systemProperty("initScript", rootProject.layout.projectDirectory.file("reference/configure-develocity.gradle").asFile.absolutePath)
    systemProperty("pluginVersion", version)
}
