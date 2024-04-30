package com.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.invocation.Gradle;

/**
 * A simple 'hello world' plugin.
 */
class DevelocityInjectionGradlePlugin implements Plugin<Gradle> {
    void apply(Gradle gradle) {
        println "Applying DEVELOCITY!"
    }
}
