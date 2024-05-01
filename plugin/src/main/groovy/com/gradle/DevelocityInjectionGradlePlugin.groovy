package com.gradle;

import com.gradle.develocity.agent.gradle.DevelocityPlugin
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.util.GradleVersion

/**
 * A simple 'hello world' plugin.
 */
class DevelocityInjectionGradlePlugin implements Plugin<Gradle> {
    void apply(Gradle gradle) {
        println "Applying DEVELOCITY!"
        gradle.beforeSettings { settings ->
            configureDevelocity(settings)
        }
    }

    private void configureDevelocity(Settings settings) {
        settings.pluginManager.apply(DevelocityPlugin)
        settings.pluginManager.apply(CommonCustomUserDataGradlePlugin)

        eachDevelocitySettingsExtension(settings) { ext ->
            ext.server = "https://ge.solutions-team.gradle.com"
            ext.allowUntrustedServer = false
        }
    }

    /**
     * Apply the `dvAction` to all 'develocity' extensions.
     * If no 'develocity' extensions are found, apply the `geAction` to all 'gradleEnterprise' extensions.
     * (The develocity plugin creates both extensions, and we want to prefer configuring 'develocity').
     */
    static def eachDevelocitySettingsExtension(def settings, def dvAction, def geAction = dvAction) {
        def GRADLE_ENTERPRISE_EXTENSION_CLASS = 'com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension'
        def DEVELOCITY_CONFIGURATION_CLASS = 'com.gradle.develocity.agent.gradle.DevelocityConfiguration'

        def dvExtensions = settings.extensions.extensionsSchema.elements
            .findAll { it.publicType.concreteClass.name == DEVELOCITY_CONFIGURATION_CLASS }
            .collect { settings[it.name] }
        if (!dvExtensions.empty) {
            dvExtensions.each(dvAction)
        } else {
            def geExtensions = settings.extensions.extensionsSchema.elements
                .findAll { it.publicType.concreteClass.name == GRADLE_ENTERPRISE_EXTENSION_CLASS }
                .collect { settings[it.name] }
            geExtensions.each(geAction)
        }
    }

    static boolean isAtLeast(String versionUnderTest, String referenceVersion) {
        GradleVersion.version(versionUnderTest) >= GradleVersion.version(referenceVersion)
    }

    static boolean isNotAtLeast(String versionUnderTest, String referenceVersion) {
        !isAtLeast(versionUnderTest, referenceVersion)
    }
}
