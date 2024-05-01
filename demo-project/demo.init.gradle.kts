initscript {
    repositories {
        maven {
            url = uri("../build/local-repo")
        }
    }
    dependencies {
        classpath("com.gradle:develocity-injection-gradle-plugin:+")
    }
}
apply<com.gradle.DevelocityInjectionGradlePlugin>()
