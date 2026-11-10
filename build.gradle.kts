import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.nio.file.Files

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

@CacheableTask
abstract class GenerateCfDocsIndex : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val docsDirectory: DirectoryProperty

    @get:OutputFile
    abstract val indexFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val source = docsDirectory.get().asFile.toPath()
        val filenames = Files.list(source).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .map { it.fileName.toString() }
                .filter { it.endsWith(".json") }
                .sorted()
                .toList()
        }
        val output = indexFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(filenames.joinToString(separator = "\n", postfix = "\n"))
    }
}

val generateCfDocsIndex = tasks.register<GenerateCfDocsIndex>("generateCfDocsIndex") {
    docsDirectory.set(layout.projectDirectory.dir("src/main/resources/cfdocs/data/en"))
    indexFile.set(layout.buildDirectory.file("generated/cfdocs/index.txt"))
}

tasks.processResources {
    dependsOn(generateCfDocsIndex)
    from(generateCfDocsIndex.flatMap { it.indexFile }) {
        into("cfdocs")
    }
}
