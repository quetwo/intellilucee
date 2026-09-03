package com.quetwo.intellilucee.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.quetwo.intellilucee.settings.CFMLFormatterSettingsResolver
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.pathString

@Suppress("DEPRECATION")
class CFMLLspClientDescriptor(project: Project) : ProjectWideLspClientDescriptor(project, "CFML")
{

    private val LOG: Logger = Logger.getInstance(CFMLLspClientDescriptor::class.java)

    companion object
    {
        private val SUPPORTED_EXTENSIONS = setOf("cfm", "cfc", "cfs", "cfml")
        private const val PLUGIN_ID = "com.quetwo.IntelliLucee"
        private const val GITHUB_LATEST_DOWNLOAD_PREFIX = "https://github.com/cfmleditor/cfmleditor-lsp/releases/latest/download/"
        private const val WINDOWS_EXE_NAME = "cfmleditor-lsp.exe"
        private const val UNIX_EXE_NAME = "cfmleditor-lsp"

        fun isSupportedExtension(extension: String?): Boolean
        {
            return extension?.lowercase() in SUPPORTED_EXTENSIONS
        }

        private fun resolveLspExecutablePath(): Path
        {
            val pluginPath = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))?.pluginPath
                ?: error("Unable to resolve IntelliLucee plugin path")

            val lspDir = pluginPath.resolve("lsp")
            Files.createDirectories(lspDir)

            val expectedExecutable = lspDir.resolve(if (isWindows()) WINDOWS_EXE_NAME else UNIX_EXE_NAME)
            val archiveName = resolveArchiveName()
            val archivePath = lspDir.resolve(archiveName)
            val downloadUrl = "$GITHUB_LATEST_DOWNLOAD_PREFIX$archiveName"

            try
            {
                LOG.info("Downloading CFML LSP from: $downloadUrl")
                downloadFile(downloadUrl, archivePath)
                extractArchive(archivePath, lspDir)
            }
            catch (exception: Exception)
            {
                if (expectedExecutable.isRegularFile())
                {
                    LOG.warn("Failed to refresh CFML LSP from latest release, using cached executable: ${expectedExecutable.pathString}", exception)
                    return expectedExecutable
                }

                throw exception
            }

            val extractedExecutable = findExtractedExecutable(lspDir)
            if (!isWindows())
            {
                extractedExecutable.toFile().setExecutable(true)
            }

            LOG.info("Resolved LSP location as: ${extractedExecutable.pathString}")
            return extractedExecutable
        }

        private fun isWindows(): Boolean = System.getProperty("os.name").contains("Windows", ignoreCase = true)

        private fun isMac(): Boolean = System.getProperty("os.name").contains("Mac", ignoreCase = true)

        private fun resolveArchiveName(): String
        {
            val osPart = when
            {
                isWindows() -> "windows"
                isMac() -> "darwin"
                else -> "linux"
            }

            val arch = System.getProperty("os.arch").lowercase()
            val archPart = if (arch.contains("aarch64") || arch.contains("arm64")) "arm64" else "amd64"
            val extension = if (isWindows()) "zip" else "tar.gz"
            return "cfmleditor-lsp-$osPart-$archPart.$extension"
        }

        private fun downloadFile(url: String, destination: Path)
        {
            val request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build()
            val response = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofInputStream())
            if (response.statusCode() !in 200..299)
            {
                error("Failed to download CFML LSP from $url. HTTP status: ${response.statusCode()}")
            }

            response.body().use { body ->
                Files.copy(body, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }

        private fun extractArchive(archivePath: Path, destination: Path)
        {
            val fileName = archivePath.name.lowercase()
            if (fileName.endsWith(".zip"))
            {
                extractZipArchive(archivePath, destination)
                return
            }

            if (fileName.endsWith(".tar.gz"))
            {
                extractTarGzArchive(archivePath, destination)
                return
            }

            error("Unsupported archive format for CFML LSP: ${archivePath.fileName}")
        }

        private fun extractZipArchive(archivePath: Path, destination: Path)
        {
            ZipInputStream(Files.newInputStream(archivePath)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null)
                {
                    val target = destination.resolve(entry.name).normalize()
                    if (!target.startsWith(destination))
                    {
                        error("Blocked archive entry outside destination: ${entry.name}")
                    }
                    if (entry.isDirectory)
                    {
                        Files.createDirectories(target)
                    }
                    else
                    {
                        target.parent?.let { Files.createDirectories(it) }
                        Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        private fun extractTarGzArchive(archivePath: Path, destination: Path)
        {
            TarArchiveInputStream(GZIPInputStream(Files.newInputStream(archivePath))).use { tar ->
                var entry = tar.nextTarEntry
                while (entry != null)
                {
                    val target = destination.resolve(entry.name).normalize()
                    if (!target.startsWith(destination))
                    {
                        error("Blocked archive entry outside destination: ${entry.name}")
                    }
                    if (entry.isDirectory)
                    {
                        Files.createDirectories(target)
                    }
                    else
                    {
                        target.parent?.let { Files.createDirectories(it) }
                        Files.copy(tar, target, StandardCopyOption.REPLACE_EXISTING)
                    }
                    entry = tar.nextTarEntry
                }
            }
        }

        private fun findExtractedExecutable(lspDir: Path): Path
        {
            val expectedName = if (isWindows()) WINDOWS_EXE_NAME else UNIX_EXE_NAME
            val direct = lspDir.resolve(expectedName)
            if (direct.exists())
            {
                return direct
            }

            Files.walk(lspDir).use { stream ->
                val found = stream
                    .filter { it.isRegularFile() }
                    .filter { it.fileName.toString().equals(expectedName, ignoreCase = isWindows()) }
                    .findFirst()
                    .orElse(null)
                if (found != null)
                {
                    return found
                }
            }

            error("CFML LSP executable was not found after extraction in ${lspDir.pathString}")
        }
    }

    override fun isSupportedFile(file: VirtualFile): Boolean
    {
        return isSupportedExtension(file.extension)
    }

    override fun createInitializationOptions(): Any
    {
        val settings = CFMLFormatterSettingsResolver.resolve(module = null)
        return mapOf(
            "debug" to true,
            "formatting" to mapOf(
                "enabled" to settings.formatterEnabled,
                "queryFormat" to settings.formatWithinQueryTags,
                "whitespaceOnly" to settings.ignoreWhiteSpaceInFormatter,
                "lowercaseTags" to settings.updateCfTagsToLowercase,
                "lowercaseAttributes" to settings.updateAttributesToLowercase,
                "doubleQuoteAttributes" to settings.normalizeAttributeValuesToDoubleQuotes,
                "queryUppercaseKeywords" to settings.normalizeSqlKeywordsToUppercase,
                "scopeCase" to settings.normalizeCfmlScopeNames,
                "commaPosition" to settings.commaPlacementInMultilineArgumentLists,
                "queryCommaPosition" to "preserve",
                "lineWidth" to settings.lineWidth,
                "attrBreakThreshold" to settings.numberOfAttributesPerLine,
                "indentWidth" to 4)
        )
    }

    override fun createCommandLine(): GeneralCommandLine
    {
        return GeneralCommandLine(resolveLspExecutablePath().toString())
    }

}