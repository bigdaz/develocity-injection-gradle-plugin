package com.gradle;

import com.gradle.develocity.agent.gradle.DevelocityPlugin
import org.gradle.api.Plugin
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.util.GradleVersion

/**
 * A simple 'hello world' plugin.
 */
class DevelocityInjectionGradlePlugin implements Plugin<Gradle> {
    void apply(Gradle gradle) {
        println "Applying DEVELOCITY!"
        configureDevelocity(gradle)
    }

    private void configureDevelocity(Gradle gradle) {
        def BUILD_SCAN_PLUGIN_ID = 'com.gradle.build-scan'
        def BUILD_SCAN_PLUGIN_CLASS = 'com.gradle.scan.plugin.BuildScanPlugin'

        def GRADLE_ENTERPRISE_PLUGIN_ID = 'com.gradle.enterprise'
        def GRADLE_ENTERPRISE_PLUGIN_CLASS = 'com.gradle.enterprise.gradleplugin.GradleEnterprisePlugin'

        def DEVELOCITY_PLUGIN_ID = 'com.gradle.develocity'
        def DEVELOCITY_PLUGIN_CLASS = 'com.gradle.develocity.agent.gradle.DevelocityPlugin'

        def CI_AUTO_INJECTION_CUSTOM_VALUE_NAME = 'CI auto injection'
        def CCUD_PLUGIN_ID = 'com.gradle.common-custom-user-data-gradle-plugin'
        def CCUD_PLUGIN_CLASS = 'com.gradle.CommonCustomUserDataGradlePlugin'

        def isTopLevelBuild = !gradle.parent
        if (!isTopLevelBuild) {
            return
        }

        def getInputParam = { String name ->
            def envVarName = name.toUpperCase().replace('.', '_').replace('-', '_')
            return System.getProperty(name) ?: System.getenv(envVarName)
        }

        // TODO: decide if we need this
//        def requestedInitScriptName = getInputParam('develocity.injection.init-script-name')
//        def initScriptName = buildscript.sourceFile.name
//        if (requestedInitScriptName != initScriptName) {
//            logger.quiet("Ignoring init script '${initScriptName}' as requested name '${requestedInitScriptName}' does not match")
//            return
//        }

// finish early if injection is disabled
        def gradleInjectionEnabled = getInputParam("develocity.injection-enabled")
        if (gradleInjectionEnabled != "true") {
            return
        }

        def develocityUrl = getInputParam('develocity.url')
        def develocityAllowUntrustedServer = Boolean.parseBoolean(getInputParam('develocity.allow-untrusted-server'))
        def develocityEnforceUrl = Boolean.parseBoolean(getInputParam('develocity.enforce-url'))
        def buildScanUploadInBackground = Boolean.parseBoolean(getInputParam('develocity.build-scan.upload-in-background'))
        def develocityCaptureFileFingerprints = getInputParam('develocity.capture-file-fingerprints') ? Boolean.parseBoolean(getInputParam('develocity.capture-file-fingerprints')) : true
        def develocityPluginVersion = getInputParam('develocity.plugin.version')
        def ccudPluginVersion = getInputParam('develocity.ccud-plugin.version')
        def buildScanTermsOfUseUrl = getInputParam('develocity.terms-of-use.url')
        def buildScanTermsOfUseAgree = getInputParam('develocity.terms-of-use.agree')
        def ciAutoInjectionCustomValueValue = getInputParam('develocity.auto-injection.custom-value')

        def atLeastGradle5 = GradleVersion.current() >= GradleVersion.version('5.0')
        def atLeastGradle4 = GradleVersion.current() >= GradleVersion.version('4.0')
        def shouldApplyDevelocityPlugin = atLeastGradle5 && develocityPluginVersion && isAtLeast(develocityPluginVersion, '3.17')

        def dvOrGe = { def dvValue, def geValue ->
            if (shouldApplyDevelocityPlugin) {
                return dvValue instanceof Closure<?> ? dvValue() : dvValue
            }
            return geValue instanceof Closure<?> ? geValue() : geValue
        }

// finish early if configuration parameters passed in via system properties are not valid/supported
        if (ccudPluginVersion && isNotAtLeast(ccudPluginVersion, '1.7')) {
            println("Common Custom User Data Gradle plugin must be at least 1.7. Configured version is $ccudPluginVersion.")
            return
        }

// register buildScanPublished listener and optionally apply the Develocity plugin
        if (GradleVersion.current() < GradleVersion.version('6.0')) {
            rootProject {
                buildscript.configurations.getByName("classpath").incoming.afterResolve { ResolvableDependencies incoming ->
                    def resolutionResult = incoming.resolutionResult

                    if (develocityPluginVersion) {
                        def scanPluginComponent = resolutionResult.allComponents.find {
                            it.moduleVersion.with { group == "com.gradle" && ['build-scan-plugin', 'gradle-enterprise-gradle-plugin', 'develocity-gradle-plugin'].contains(name) }
                        }
                        if (!scanPluginComponent) {
                            def pluginClass = dvOrGe(DEVELOCITY_PLUGIN_CLASS, BUILD_SCAN_PLUGIN_CLASS)
                            println("Applying $pluginClass via init script")
                            applyPluginExternally(pluginManager, pluginClass)
                            def rootExtension = dvOrGe(
                                    { develocity },
                                    { buildScan }
                            )
                            def buildScanExtension = dvOrGe(
                                    { rootExtension.buildScan },
                                    { rootExtension }
                            )
                            if (develocityUrl) {
                                println("Connection to Develocity: $develocityUrl, allowUntrustedServer: $develocityAllowUntrustedServer, captureFileFingerprints: $develocityCaptureFileFingerprints")
                                rootExtension.server = develocityUrl
                                rootExtension.allowUntrustedServer = develocityAllowUntrustedServer
                            }
                            if (!shouldApplyDevelocityPlugin) {
                                // Develocity plugin publishes scans by default
                                buildScanExtension.publishAlways()
                            }
                            // uploadInBackground not available for build-scan-plugin 1.16
                            if (buildScanExtension.metaClass.respondsTo(buildScanExtension, 'setUploadInBackground', Boolean)) buildScanExtension.uploadInBackground = buildScanUploadInBackground
                            buildScanExtension.value CI_AUTO_INJECTION_CUSTOM_VALUE_NAME, ciAutoInjectionCustomValueValue
                            if (isAtLeast(develocityPluginVersion, '2.1') && atLeastGradle5) {
                                println("Setting captureFileFingerprints: $develocityCaptureFileFingerprints")
                                if (isAtLeast(develocityPluginVersion, '3.17')) {
                                    buildScanExtension.capture.fileFingerprints.set(develocityCaptureFileFingerprints)
                                } else if (isAtLeast(develocityPluginVersion, '3.7')) {
                                    buildScanExtension.capture.taskInputFiles = develocityCaptureFileFingerprints
                                } else {
                                    buildScanExtension.captureTaskInputFiles = develocityCaptureFileFingerprints
                                }
                            }
                        }

                        if (develocityUrl && develocityEnforceUrl) {
                            println("Enforcing Develocity: $develocityUrl, allowUntrustedServer: $develocityAllowUntrustedServer, captureFileFingerprints: $develocityCaptureFileFingerprints")
                        }

                        pluginManager.withPlugin(BUILD_SCAN_PLUGIN_ID) {
                            // Only execute if develocity plugin isn't applied.
                            if (gradle.rootProject.extensions.findByName("develocity")) return
                            afterEvaluate {
                                if (develocityUrl && develocityEnforceUrl) {
                                    buildScan.server = develocityUrl
                                    buildScan.allowUntrustedServer = develocityAllowUntrustedServer
                                }
                            }

                            if (buildScanTermsOfUseUrl && buildScanTermsOfUseAgree) {
                                buildScan.termsOfServiceUrl = buildScanTermsOfUseUrl
                                buildScan.termsOfServiceAgree = buildScanTermsOfUseAgree
                            }
                        }

                        pluginManager.withPlugin(DEVELOCITY_PLUGIN_ID) {
                            afterEvaluate {
                                if (develocityUrl && develocityEnforceUrl) {
                                    develocity.server = develocityUrl
                                    develocity.allowUntrustedServer = develocityAllowUntrustedServer
                                }
                            }

                            if (buildScanTermsOfUseUrl && buildScanTermsOfUseAgree) {
                                develocity.buildScan.termsOfUseUrl = buildScanTermsOfUseUrl
                                develocity.buildScan.termsOfUseAgree = buildScanTermsOfUseAgree
                            }
                        }
                    }

                    if (ccudPluginVersion && atLeastGradle4) {
                        def ccudPluginComponent = resolutionResult.allComponents.find {
                            it.moduleVersion.with { group == "com.gradle" && name == "common-custom-user-data-gradle-plugin" }
                        }
                        if (!ccudPluginComponent) {
                            println("Applying $CCUD_PLUGIN_CLASS via init script")
                            // TODO: Make sure this works
//                            pluginManager.apply(gradle.initscript.classLoader.loadClass(CCUD_PLUGIN_CLASS))
                            settings.pluginManager.apply(CommonCustomUserDataGradlePlugin)
                        }
                    }
                }
            }
        } else {
            gradle.settingsEvaluated { settings ->
                if (develocityPluginVersion) {
                    if (!settings.pluginManager.hasPlugin(GRADLE_ENTERPRISE_PLUGIN_ID) && !settings.pluginManager.hasPlugin(DEVELOCITY_PLUGIN_ID)) {
                        def pluginClass = dvOrGe(DEVELOCITY_PLUGIN_CLASS, GRADLE_ENTERPRISE_PLUGIN_CLASS)
                        println("Applying $pluginClass via init script")
                        applyPluginExternally(settings.pluginManager, pluginClass)
                        if (develocityUrl) {
                            println("Connection to Develocity: $develocityUrl, allowUntrustedServer: $develocityAllowUntrustedServer, captureFileFingerprints: $develocityCaptureFileFingerprints")
                            eachDevelocitySettingsExtension(settings) { ext ->
                                ext.server = develocityUrl
                                ext.allowUntrustedServer = develocityAllowUntrustedServer
                            }
                        }

                        eachDevelocitySettingsExtension(settings) { ext ->
                            ext.buildScan.uploadInBackground = buildScanUploadInBackground
                            ext.buildScan.value CI_AUTO_INJECTION_CUSTOM_VALUE_NAME, ciAutoInjectionCustomValueValue
                        }

                        eachDevelocitySettingsExtension(settings,
                                { develocity ->
                                    println("Setting captureFileFingerprints: $develocityCaptureFileFingerprints")
                                    develocity.buildScan.capture.fileFingerprints = develocityCaptureFileFingerprints
                                },
                                { gradleEnterprise ->
                                    gradleEnterprise.buildScan.publishAlways()
                                    if (isAtLeast(develocityPluginVersion, '2.1')) {
                                        println("Setting captureFileFingerprints: $develocityCaptureFileFingerprints")
                                        if (isAtLeast(develocityPluginVersion, '3.7')) {
                                            gradleEnterprise.buildScan.capture.taskInputFiles = develocityCaptureFileFingerprints
                                        } else {
                                            gradleEnterprise.buildScan.captureTaskInputFiles = develocityCaptureFileFingerprints
                                        }
                                    }
                                }
                        )
                    }

                    if (develocityUrl && develocityEnforceUrl) {
                        println("Enforcing Develocity: $develocityUrl, allowUntrustedServer: $develocityAllowUntrustedServer, captureFileFingerprints: $develocityCaptureFileFingerprints")
                    }

                    eachDevelocitySettingsExtension(settings,
                            { develocity ->
                                if (develocityUrl && develocityEnforceUrl) {
                                    develocity.server = develocityUrl
                                    develocity.allowUntrustedServer = develocityAllowUntrustedServer
                                }

                                if (buildScanTermsOfUseUrl && buildScanTermsOfUseAgree) {
                                    develocity.buildScan.termsOfUseUrl = buildScanTermsOfUseUrl
                                    develocity.buildScan.termsOfUseAgree = buildScanTermsOfUseAgree
                                }
                            },
                            { gradleEnterprise ->
                                if (develocityUrl && develocityEnforceUrl) {
                                    gradleEnterprise.server = develocityUrl
                                    gradleEnterprise.allowUntrustedServer = develocityAllowUntrustedServer
                                }

                                if (buildScanTermsOfUseUrl && buildScanTermsOfUseAgree) {
                                    gradleEnterprise.buildScan.termsOfServiceUrl = buildScanTermsOfUseUrl
                                    gradleEnterprise.buildScan.termsOfServiceAgree = buildScanTermsOfUseAgree
                                }
                            }
                    )
                }

                if (ccudPluginVersion) {
                    if (!settings.pluginManager.hasPlugin(CCUD_PLUGIN_ID)) {
                        println("Applying $CCUD_PLUGIN_CLASS via init script")
                        // TODO: Make sure this works
//                        settings.pluginManager.apply(initscript.classLoader.loadClass(CCUD_PLUGIN_CLASS))
                        settings.pluginManager.apply(CommonCustomUserDataGradlePlugin)
                    }
                }
            }
        }
    }

    void applyPluginExternally(def pluginManager, String pluginClassName) {
        def externallyApplied = 'develocity.externally-applied'
        def externallyAppliedDeprecated = 'gradle.enterprise.externally-applied'
        def oldValue = System.getProperty(externallyApplied)
        def oldValueDeprecated = System.getProperty(externallyAppliedDeprecated)
        System.setProperty(externallyApplied, 'true')
        System.setProperty(externallyAppliedDeprecated, 'true')
        try {
            // TODO: Make sure this works
//            pluginManager.apply(initscript.classLoader.loadClass(pluginClassName))
            pluginManager.apply(DevelocityPlugin)

        } finally {
            if (oldValue == null) {
                System.clearProperty(externallyApplied)
            } else {
                System.setProperty(externallyApplied, oldValue)
            }
            if (oldValueDeprecated == null) {
                System.clearProperty(externallyAppliedDeprecated)
            } else {
                System.setProperty(externallyAppliedDeprecated, oldValueDeprecated)
            }
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
