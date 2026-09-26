import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

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
    }
}
