initscript {
    repositories {
        maven {
            url = uri("../build/local-repo")
        }
    }
    dependencies {
        classpath("develocity-injection-gradle-plugin:plugin:+")
    }
}
apply<com.gradle.DevelocityInjectionGradlePlugin>()
