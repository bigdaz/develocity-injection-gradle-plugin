initscript {
    repositories {
        maven {
            url = uri("../build/local-repo")
        }
    }
    dependencies {
        classpath("com.gradle.develocity:plugin:+")
    }
}
apply<com.gradle.develocity.DevelocityInjectionGradlePlugin>()
