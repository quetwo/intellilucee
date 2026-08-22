import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2026.2.1")
        testFramework(TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:
        bundledPlugin("com.intellij.java")
        bundledPlugin("JavaScript")
        bundledPlugin("com.intellij.modules.json")
        bundledPlugin("com.intellij.properties")
    }
}

tasks {
    processResources {
        exclude("lsp/cfmleditor-lsp.exe")
    }

    prepareSandbox {
        from("src/main/resources/lsp/cfmleditor-lsp.exe") {
            into("${project.name}/lsp")
        }
    }
}
