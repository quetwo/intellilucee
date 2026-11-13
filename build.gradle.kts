import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.UntrackedTask
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

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

@UntrackedTask(because = "Downloads latest CFDocs repository from remote GitHub URL")
abstract class SyncCfDocs : DefaultTask() {
    @get:Input
    abstract val repositoryUrl: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun sync() {
        val url = repositoryUrl.get()
        logger.lifecycle("Downloading CFDocs from $url...")

        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() !in 200..299) {
            throw GradleException("Failed to download CFDocs from $url: HTTP ${response.statusCode()}")
        }

        val destinationDir = outputDirectory.get().asFile.toPath().normalize()
        Files.createDirectories(destinationDir)

        var count = 0
        ZipInputStream(response.body()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.replace('\\', '/')
                val firstSlash = name.indexOf('/')
                val relativePath = if (name.startsWith("data/") || name == "data") {
                    name
                } else if (firstSlash != -1 && (name.substring(firstSlash + 1).startsWith("data/") || name.substring(firstSlash + 1) == "data")) {
                    name.substring(firstSlash + 1)
                } else {
                    null
                }

                if (relativePath != null) {
                    val targetPath = destinationDir.resolve(relativePath).normalize()
                    if (!targetPath.startsWith(destinationDir)) {
                        throw GradleException("Zip entry is outside of destination directory: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        Files.createDirectories(targetPath)
                    } else {
                        targetPath.parent?.let { Files.createDirectories(it) }
                        Files.copy(zip, targetPath, StandardCopyOption.REPLACE_EXISTING)
                        count++
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        logger.lifecycle("Successfully extracted $count CFDocs data files to $destinationDir")
    }
}

val syncCfDocs = tasks.register<SyncCfDocs>("Sync CFDocs") {
    group = "documentation"
    description = "Downloads the CFDocs repository from GitHub and extracts the data folder to resources/cfdocs"
    repositoryUrl.convention("https://github.com/foundeo/cfdocs/archive/refs/heads/master.zip")
    outputDirectory.convention(layout.projectDirectory.dir("src/main/resources/cfdocs"))
}

tasks.register("syncCfDocs") {
    group = "documentation"
    description = "Alias for 'Sync CFDocs'"
    dependsOn(syncCfDocs)
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
