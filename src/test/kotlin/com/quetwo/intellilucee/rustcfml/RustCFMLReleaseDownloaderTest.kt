package com.quetwo.intellilucee.rustcfml

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

class RustCFMLReleaseDownloaderTest
{
    @Test
    fun testMapsSupportedPlatformsToPublishedAssets()
    {
        assertEquals("rustcfml-windows-x86_64.exe", RustCFMLReleaseDownloader.assetNameFor("Windows 11", "amd64"))
        assertEquals("rustcfml-linux-x86_64", RustCFMLReleaseDownloader.assetNameFor("Linux", "x86_64"))
        assertEquals("rustcfml-linux-aarch64", RustCFMLReleaseDownloader.assetNameFor("Linux", "aarch64"))
        assertEquals("rustcfml-macos-aarch64", RustCFMLReleaseDownloader.assetNameFor("Mac OS X", "arm64"))
        assertThrows(RustCFMLDownloadException::class.java) {
            RustCFMLReleaseDownloader.assetNameFor("Mac OS X", "x86_64")
        }
    }

    @Test
    fun testParsesStableReleaseAndRejectsPrereleaseAndMalformedMetadata()
    {
        val release = RustCFMLReleaseDownloader.parseLatestRelease(releaseMetadata())
        assertEquals("v1.2.3", release.tag)
        assertEquals("rustcfml-linux-x86_64", release.assets.single().name)
        assertThrows(RustCFMLDownloadException::class.java) {
            RustCFMLReleaseDownloader.parseLatestRelease(releaseMetadata(prerelease = true))
        }
        assertThrows(RustCFMLDownloadException::class.java) {
            RustCFMLReleaseDownloader.parseLatestRelease("not json")
        }
    }

    @Test
    fun testDownloadsToVersionedDirectoryAndSetsExecutablePermission()
    {
        val directory = Files.createTempDirectory("rustcfml-download-test")
        val transport = FakeTransport()
        val downloader = RustCFMLReleaseDownloader(transport, "Linux", "x86_64", directory)

        val executable = downloader.downloadLatest()

        assertEquals(directory.resolve("v1.2.3/rustcfml-linux-x86_64"), executable)
        assertEquals("binary-content", Files.readString(executable))
        assertEquals("https://github.com/RustCFML/RustCFML/releases/download/v1.2.3/rustcfml-linux-x86_64", transport.downloadedUrl)
        try
        {
            assertTrue(Files.getPosixFilePermissions(executable).contains(PosixFilePermission.OWNER_EXECUTE))
        }
        catch (_: UnsupportedOperationException)
        {
            assertTrue(executable.toFile().canExecute())
        }
    }

    @Test
    fun testUnsupportedPlatformDoesNotContactTransport()
    {
        val transport = FakeTransport()
        val downloader = RustCFMLReleaseDownloader(transport, "Mac OS X", "x86_64", Files.createTempDirectory("rustcfml-unsupported"))

        assertThrows(RustCFMLDownloadException::class.java) { downloader.downloadLatest() }
        assertFalse(transport.metadataRequested)
    }

    @Test
    fun testHttpFailureAndCancellationCleanPartialFiles()
    {
        val directory = Files.createTempDirectory("rustcfml-download-failure")
        val failingTransport = FakeTransport().apply { downloadFailure = IOException("HTTP 503") }
        val failingDownloader = RustCFMLReleaseDownloader(failingTransport, "Linux", "x86_64", directory)
        assertThrows(RustCFMLDownloadException::class.java) { failingDownloader.downloadLatest() }

        val cancelingTransport = FakeTransport().apply { cancelAfterPartialDownload = true }
        val cancelingDownloader = RustCFMLReleaseDownloader(cancelingTransport, "Linux", "x86_64", directory)
        assertThrows(ProcessCanceledException::class.java) { cancelingDownloader.downloadLatest() }

        val versionDirectory = directory.resolve("v1.2.3")
        assertTrue(Files.isDirectory(versionDirectory))
        Files.list(versionDirectory).use { files -> assertEquals(emptyList<Path>(), files.toList()) }
    }

    @Test
    fun testMissingPlatformAssetReportsError()
    {
        val transport = FakeTransport().apply { metadata = releaseMetadata(assetName = "another-binary") }
        val downloader = RustCFMLReleaseDownloader(transport, "Linux", "x86_64", Files.createTempDirectory("rustcfml-missing-asset"))

        val error = assertThrows(RustCFMLDownloadException::class.java) { downloader.downloadLatest() }
        assertTrue(error.message!!.contains("rustcfml-linux-x86_64"))
    }

    private fun releaseMetadata(prerelease: Boolean = false, assetName: String = "rustcfml-linux-x86_64"): String =
        """{"tag_name":"v1.2.3","prerelease":$prerelease,"assets":[{"name":"$assetName","browser_download_url":"https://github.com/RustCFML/RustCFML/releases/download/v1.2.3/$assetName"}]}"""

    private class FakeTransport : RustCFMLHttpTransport
    {
        var metadata = """{"tag_name":"v1.2.3","prerelease":false,"assets":[{"name":"rustcfml-linux-x86_64","browser_download_url":"https://github.com/RustCFML/RustCFML/releases/download/v1.2.3/rustcfml-linux-x86_64"}]}"""
        var metadataRequested = false
        var downloadedUrl: String? = null
        var downloadFailure: Exception? = null
        var cancelAfterPartialDownload = false

        override fun getText(url: String, indicator: ProgressIndicator?): String
        {
            metadataRequested = true
            return metadata
        }

        override fun download(url: String, destination: Path, indicator: ProgressIndicator?)
        {
            downloadedUrl = url
            Files.writeString(destination, "partial")
            if (cancelAfterPartialDownload)
            {
                throw ProcessCanceledException()
            }
            downloadFailure?.let { throw it }
            Files.writeString(destination, "binary-content")
        }
    }
}