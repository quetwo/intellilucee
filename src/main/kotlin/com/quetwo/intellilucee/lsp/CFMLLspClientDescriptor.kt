package com.quetwo.intellilucee.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PluginPathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.quetwo.intellilucee.settings.CFMLFormatterSettingsResolver
import com.quetwo.intellilucee.settings.CFMLGlobalSettings
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
        private const val GITHUB_LATEST_DOWNLOAD_PREFIX = "https://github.com/cfmleditor/clif/releases/latest/download/"
        private const val GITHUB_RELEASE_DOWNLOAD_PREFIX = "https://github.com/cfmleditor/clif/releases/download/"

        // The server was renamed from cfmleditor-lsp to clif. Releases from before
        // the rename publish only cfmleditor-lsp archives, and every release since
        // publishes both, so clif is tried first and cfmleditor-lsp after it: a
        // pinned older version still downloads, and an executable already on disk
        // under either name is still used.
        internal val SERVER_NAMES = listOf("clif", "cfmleditor-lsp")

        fun isSupportedExtension(extension: String?): Boolean
        {
            return extension?.lowercase() in SUPPORTED_EXTENSIONS
        }

        internal fun resolveDownloadUrl(version: String, archiveName: String): String
        {
            return if (version.equals(CFMLLspReleaseProvider.LATEST, ignoreCase = true))
            {
                "$GITHUB_LATEST_DOWNLOAD_PREFIX$archiveName"
            }
            else
            {
                "$GITHUB_RELEASE_DOWNLOAD_PREFIX$version/$archiveName"
            }
        }

        internal fun resolveLspExecutablePath(project: Project? = null): Path
        {
            val pluginPath = PluginPathManager.getPluginHome("IntelliLucee").toPath()
            val selectedVersion = CFMLGlobalSettings.getInstance().state.lspReleaseVersion.trim().ifEmpty { CFMLLspReleaseProvider.LATEST }
            val lspDir = pluginPath.resolve("lsp").resolve(selectedVersion)
            LOG.info("Using lsp executable path for version $selectedVersion - $lspDir")

            try
            {
                Files.createDirectories(lspDir)
            }
            catch (exception: Exception)
            {
                LOG.error("Failed to create CFML LSP directory: $lspDir", exception)
                val errorMessage = "Failed to create the directory for CFML Code Plugin at ${lspDir.pathString}: ${exception.localizedMessage ?: exception.message ?: ""}".trimEnd(':', ' ')
                val app = ApplicationManager.getApplication()
                if (app != null && !app.isDispatchThread)
                {
                    app.invokeLater {
                        Messages.showErrorDialog(project, errorMessage, "Lucee CFML Error")
                    }
                }
                else
                {
                    Messages.showErrorDialog(project, errorMessage, "Lucee CFML Error")
                }
                throw exception
            }

            val exeNames = SERVER_NAMES.map { executableName(it, isWindows()) }

            val executableName = try
            {
                downloadServer(selectedVersion, lspDir)
            }
            catch (exception: Exception)
            {
                for (name in exeNames)
                {
                    val cached = lspDir.resolve(name)
                    if (cached.isRegularFile())
                    {
                        LOG.warn("Failed to refresh CFML LSP for version $selectedVersion, using cached executable: ${cached.pathString}", exception)
                        return cached
                    }
                }

                if (selectedVersion.equals(CFMLLspReleaseProvider.LATEST, ignoreCase = true))
                {
                    for (name in exeNames)
                    {
                        val legacyExecutable = pluginPath.resolve("lsp").resolve(name)
                        if (legacyExecutable.isRegularFile())
                        {
                            LOG.warn("Failed to refresh CFML LSP for version $selectedVersion, using legacy cached executable: ${legacyExecutable.pathString}", exception)
                            return legacyExecutable
                        }
                    }
                }

                throw exception
            }

            val extractedExecutable = findExtractedExecutable(lspDir, executableName)
            if (!isWindows())
            {
                extractedExecutable.toFile().setExecutable(true)
            }

            LOG.info("Resolved LSP location as: ${extractedExecutable.pathString}")
            return extractedExecutable
        }

        private fun isWindows(): Boolean = System.getProperty("os.name").contains("Windows", ignoreCase = true)

        private fun isMac(): Boolean = System.getProperty("os.name").contains("Mac", ignoreCase = true)

        internal fun executableName(serverName: String, windows: Boolean): String = if (windows) "$serverName.exe" else serverName

        /**
         * The archives that could carry the server for a platform, best first, each
         * with the executable inside it.
         */
        internal fun archiveCandidates(osPart: String, archPart: String, windows: Boolean): List<Pair<String, String>>
        {
            val extension = if (windows) "zip" else "tar.gz"
            return SERVER_NAMES.map { "$it-$osPart-$archPart.$extension" to executableName(it, windows) }
        }

        /**
         * Downloads and unpacks the first archive the release has, and returns the
         * name of the executable it held. Only a missing archive moves on to the next
         * candidate; any other failure is the network's and would fail again.
         */
        private fun downloadServer(version: String, lspDir: Path): String
        {
            val osPart = when
            {
                isWindows() -> "windows"
                isMac() -> "darwin"
                else -> "linux"
            }

            val arch = System.getProperty("os.arch").lowercase()
            val archPart = if (arch.contains("aarch64") || arch.contains("arm64")) "arm64" else "amd64"

            var notFound: Exception? = null
            for ((archiveName, exeName) in archiveCandidates(osPart, archPart, isWindows()))
            {
                val downloadUrl = resolveDownloadUrl(version, archiveName)
                val archivePath = lspDir.resolve(archiveName)
                try
                {
                    LOG.info("Downloading CFML LSP ($version) from: $downloadUrl")
                    downloadFile(downloadUrl, archivePath)
                }
                catch (exception: HttpStatusException)
                {
                    if (exception.status != 404)
                    {
                        throw exception
                    }
                    notFound = exception
                    continue
                }

                extractArchive(archivePath, lspDir)
                return exeName
            }

            throw notFound ?: IllegalStateException("No CFML LSP archive for $osPart-$archPart in $version")
        }

        internal class HttpStatusException(val status: Int, message: String) : Exception(message)

        private fun downloadFile(url: String, destination: Path)
        {
            val request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build()
            val response = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofInputStream())
            if (response.statusCode() !in 200..299)
            {
                response.body().close()
                throw HttpStatusException(response.statusCode(), "Failed to download CFML LSP from $url. HTTP status: ${response.statusCode()}")
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
                var entry = tar.nextEntry
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
                    entry = tar.nextEntry
                }
            }
        }

        private fun findExtractedExecutable(lspDir: Path, expectedName: String): Path
        {
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
        val globalSettings = ApplicationManager.getApplication()?.getService(CFMLGlobalSettings::class.java)
        val lintingEnabled = globalSettings?.state?.syntaxAndErrorHighlighting ?: true
        val settings = CFMLFormatterSettingsResolver.resolve(module = null)
        return mapOf(
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
                "braceStyle" to settings.braceStyle,
                "parenSpacing" to settings.parenSpacing,
                "indentWidth" to 4),
            "linting" to mapOf(
                "enabled" to lintingEnabled)
        )
    }

    override fun createCommandLine(): GeneralCommandLine
    {
        return GeneralCommandLine(resolveLspExecutablePath(project).toString())
    }

}