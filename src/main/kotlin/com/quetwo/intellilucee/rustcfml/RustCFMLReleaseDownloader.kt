package com.quetwo.intellilucee.rustcfml

import com.google.gson.JsonParser
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission

class RustCFMLReleaseDownloader(
    private val transport: RustCFMLHttpTransport = UrlConnectionRustCFMLHttpTransport(),
    private val osName: String = System.getProperty("os.name"),
    private val architecture: String = System.getProperty("os.arch"),
    private val installDirectory: Path = Path.of(PathManager.getSystemPath(), "rustcfml")
)
{
    fun downloadLatest(indicator: ProgressIndicator? = null): Path
    {
        indicator?.checkCanceled()
        val assetName = assetNameFor(osName, architecture)
        val metadata = try
        {
            transport.getText(LATEST_RELEASE_URL, indicator)
        }
        catch (exception: ProcessCanceledException)
        {
            throw exception
        }
        catch (exception: Exception)
        {
            throw RustCFMLDownloadException("Failed to fetch the latest stable RustCFML release: ${exception.message ?: "unknown error"}", exception)
        }
        val release = parseLatestRelease(metadata)
        val asset = release.assets.firstOrNull { it.name == assetName }
            ?: throw RustCFMLDownloadException("RustCFML ${release.tag} does not publish the $assetName asset")

        val versionDirectory = installDirectory.resolve(release.tag).normalize()
        if (!versionDirectory.startsWith(installDirectory.normalize()))
        {
            throw RustCFMLDownloadException("The RustCFML release has an invalid version tag")
        }

        try
        {
            Files.createDirectories(versionDirectory)
        }
        catch (exception: IOException)
        {
            throw RustCFMLDownloadException("Could not create the RustCFML installation directory", exception)
        }
        if (!Files.isDirectory(versionDirectory) || !Files.isWritable(versionDirectory))
        {
            throw RustCFMLDownloadException("RustCFML installation directory is not writable: $versionDirectory")
        }

        val executable = versionDirectory.resolve(assetName)
        val temporaryFile = try
        {
            Files.createTempFile(versionDirectory, ".$assetName.", ".part")
        }
        catch (exception: IOException)
        {
            throw RustCFMLDownloadException("Could not create a temporary RustCFML download file", exception)
        }

        try
        {
            indicator?.text = "Downloading RustCFML ${release.tag}"
            transport.download(asset.downloadUrl, temporaryFile, indicator)
            indicator?.checkCanceled()
            if (Files.size(temporaryFile) == 0L)
            {
                throw RustCFMLDownloadException("The RustCFML download was empty")
            }
            if (!assetName.endsWith(".exe", ignoreCase = true))
            {
                makeExecutable(temporaryFile)
            }
            try
            {
                Files.move(temporaryFile, executable, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }
            catch (_: AtomicMoveNotSupportedException)
            {
                Files.move(temporaryFile, executable, StandardCopyOption.REPLACE_EXISTING)
            }
            return executable
        }
        catch (exception: ProcessCanceledException)
        {
            throw exception
        }
        catch (exception: RustCFMLDownloadException)
        {
            throw exception
        }
        catch (exception: Exception)
        {
            throw RustCFMLDownloadException("Failed to download RustCFML ${release.tag}: ${exception.message ?: "unknown error"}", exception)
        }
        finally
        {
            Files.deleteIfExists(temporaryFile)
        }
    }

    private fun makeExecutable(path: Path)
    {
        try
        {
            val permissions = Files.getPosixFilePermissions(path).toMutableSet()
            permissions.addAll(setOf(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE))
            Files.setPosixFilePermissions(path, permissions)
        }
        catch (_: UnsupportedOperationException)
        {
            if (!path.toFile().setExecutable(true))
            {
                throw RustCFMLDownloadException("Could not mark the downloaded RustCFML binary as executable")
            }
        }
    }

    companion object
    {
        internal const val LATEST_RELEASE_URL = "https://api.github.com/repos/RustCFML/RustCFML/releases/latest"
        private val VERSION_PATTERN = Regex("[A-Za-z0-9._-]+")

        internal fun assetNameFor(osName: String, architecture: String): String
        {
            val os = osName.lowercase()
            val arch = architecture.lowercase()
            return when
            {
                os.startsWith("windows") && arch in setOf("amd64", "x86_64", "x64") -> "rustcfml-windows-x86_64.exe"
                os.startsWith("linux") && arch in setOf("amd64", "x86_64", "x64") -> "rustcfml-linux-x86_64"
                os.startsWith("linux") && arch in setOf("aarch64", "arm64") -> "rustcfml-linux-aarch64"
                (os.startsWith("mac") || os.startsWith("darwin")) && arch in setOf("aarch64", "arm64") -> "rustcfml-macos-aarch64"
                else -> throw RustCFMLDownloadException(
                    "Latest RustCFML download is not available for $osName ($architecture); select an executable manually"
                )
            }
        }

        internal fun parseLatestRelease(json: String): RustCFMLRelease
        {
            try
            {
                val release = JsonParser.parseString(json).asJsonObject
                val tag = release.get("tag_name")?.asString?.takeIf { VERSION_PATTERN.matches(it) }
                    ?: throw RustCFMLDownloadException("GitHub returned RustCFML release metadata without a valid version")
                if (release.get("prerelease")?.asBoolean != false)
                {
                    throw RustCFMLDownloadException("GitHub did not return a stable RustCFML release")
                }
                val assets = release.getAsJsonArray("assets")?.mapNotNull { element ->
                    val asset = element.asJsonObject
                    val name = asset.get("name")?.asString
                    val url = asset.get("browser_download_url")?.asString
                    if (name.isNullOrBlank() || url.isNullOrBlank()) null else RustCFMLReleaseAsset(name, url)
                }.orEmpty()
                return RustCFMLRelease(tag, assets)
            }
            catch (exception: RustCFMLDownloadException)
            {
                throw exception
            }
            catch (exception: Exception)
            {
                throw RustCFMLDownloadException("Could not parse RustCFML release metadata", exception)
            }
        }
    }
}

internal data class RustCFMLRelease(val tag: String, val assets: List<RustCFMLReleaseAsset>)

internal data class RustCFMLReleaseAsset(val name: String, val downloadUrl: String)

interface RustCFMLHttpTransport
{
    fun getText(url: String, indicator: ProgressIndicator?): String
    fun download(url: String, destination: Path, indicator: ProgressIndicator?)
}

private class UrlConnectionRustCFMLHttpTransport : RustCFMLHttpTransport
{
    override fun getText(url: String, indicator: ProgressIndicator?): String
    {
        indicator?.checkCanceled()
        val connection = openConnection(url)
        try
        {
            val status = connection.responseCode
            if (status !in 200..299)
            {
                throw IOException("GitHub returned HTTP $status")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        finally
        {
            connection.disconnect()
        }
    }

    override fun download(url: String, destination: Path, indicator: ProgressIndicator?)
    {
        indicator?.checkCanceled()
        val connection = openConnection(url)
        try
        {
            val status = connection.responseCode
            if (status !in 200..299)
            {
                throw IOException("GitHub returned HTTP $status while downloading the release")
            }
            connection.inputStream.use { input ->
                Files.newOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true)
                    {
                        indicator?.checkCanceled()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                }
            }
        }
        finally
        {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 60_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "IntelliLucee")
        setRequestProperty("Accept", "application/vnd.github+json")
    }
}

class RustCFMLDownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)