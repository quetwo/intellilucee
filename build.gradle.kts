import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdea("2026.2.1")
        testFramework(TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:
        bundledPlugin("com.intellij.java")
        bundledPlugin("JavaScript")
        bundledPlugin("com.intellij.modules.json")
        bundledPlugin("com.intellij.properties")
        bundledPlugin("org.jetbrains.plugins.textmate")
    }
}

tasks {
    processResources {
        exclude("grammar/*.json")
    }

    prepareSandbox {
        from("src/main/resources/grammar/cfml.tmLanguage.json") {
            into("${project.name}/grammar")
        }
        from("src/main/resources/grammar/cfml-cfs.tmLanguage.json") {
            into("${project.name}/grammar")
        }
        from("src/main/resources/grammar/language-configuration.json") {
            into("${project.name}/grammar")
        }
        from("src/main/resources/grammar/package.json") {
            into("${project.name}/grammar")
        }
    }
}
