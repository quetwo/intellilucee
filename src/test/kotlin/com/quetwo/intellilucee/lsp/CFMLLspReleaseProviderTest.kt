package com.quetwo.intellilucee.lsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CFMLLspReleaseProviderTest
{
    @Test
    fun testGetAvailableVersionsStartsWithLatest()
    {
        val versions = CFMLLspReleaseProvider.getAvailableVersions()
        assertTrue("Versions list should not be empty", versions.isNotEmpty())
        assertEquals("First element should always be LATEST", "LATEST", versions.first())
    }

    @Test
    fun testGetAvailableVersionsContainsReleases()
    {
        val versions = CFMLLspReleaseProvider.getAvailableVersions()
        assertTrue("Versions should contain release versions", versions.size > 1)
        assertTrue("Versions should contain v0.2.8 or another release", versions.any { it.startsWith("v") })
    }

    @Test
    fun testGetAvailableVersionsIncludesCurrentSelectedCustomVersion()
    {
        val customVersion = "v99.99.99-custom"
        val versions = CFMLLspReleaseProvider.getAvailableVersions(customVersion)
        assertTrue("Custom selected version should be in the list", versions.contains(customVersion))
    }

    @Test
    fun testGetAvailableVersionsHasNoDuplicates()
    {
        val versions = CFMLLspReleaseProvider.getAvailableVersions("v0.2.8")
        val uniqueVersions = versions.toSet()
        assertEquals("There should be no duplicate versions in the list", uniqueVersions.size, versions.size)
    }

    @Test
    fun testResolveDownloadUrlForLatestAndCustomVersions()
    {
        val archiveName = "clif-windows-amd64.zip"
        val latestUrl = CFMLLspClientDescriptor.resolveDownloadUrl("LATEST", archiveName)
        assertEquals(
            "https://github.com/cfmleditor/clif/releases/latest/download/clif-windows-amd64.zip",
            latestUrl
        )

        val versionedUrl = CFMLLspClientDescriptor.resolveDownloadUrl("v0.2.8", "cfmleditor-lsp-windows-amd64.zip")
        assertEquals(
            "https://github.com/cfmleditor/clif/releases/download/v0.2.8/cfmleditor-lsp-windows-amd64.zip",
            versionedUrl
        )
    }

    @Test
    fun testArchiveCandidatesTryClifThenTheNameBeforeTheRename()
    {
        // A release from before the rename publishes only cfmleditor-lsp archives.
        assertEquals(
            listOf("clif-darwin-arm64.tar.gz" to "clif", "cfmleditor-lsp-darwin-arm64.tar.gz" to "cfmleditor-lsp"),
            CFMLLspClientDescriptor.archiveCandidates("darwin", "arm64", windows = false)
        )
        assertEquals(
            listOf("clif-windows-amd64.zip" to "clif.exe", "cfmleditor-lsp-windows-amd64.zip" to "cfmleditor-lsp.exe"),
            CFMLLspClientDescriptor.archiveCandidates("windows", "amd64", windows = true)
        )
    }
}
