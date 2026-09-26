package com.quetwo.intellilucee.lsp

import com.intellij.openapi.diagnostic.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object CFMLLspReleaseProvider
{
    private val LOG: Logger = Logger.getInstance(CFMLLspReleaseProvider::class.java)

    const val LATEST = "LATEST"
    private const val GITHUB_RELEASES_API_URL = "https://api.github.com/repos/cfmleditor/cfmleditor-lsp/releases"

    private val FALLBACK_VERSIONS = listOf(
        "v0.3.6",
        "v0.3.5",
        "v0.3.4",
        "v0.3.3",
        "v0.3.2",
        "v0.3.1",
        "v0.3.0",
        "v0.2.8",
        "v0.2.7",
        "v0.2.6",
        "v0.2.5"
    )

    @Volatile
    private var cachedReleases: List<String>? = null

    fun getAvailableVersions(currentSelected: String? = null): List<String>
    {
        val versions = mutableListOf(LATEST)
        val fetched = getOrFetchReleases()
        val releaseList = fetched.ifEmpty { FALLBACK_VERSIONS }

        for (version in releaseList)
        {
            if (!versions.contains(version))
            {
                versions.add(version)
            }
        }

        if (!currentSelected.isNullOrBlank() && !versions.contains(currentSelected))
        {
            versions.add(currentSelected)
        }

        return versions
    }

    private fun getOrFetchReleases(): List<String>
    {
        cachedReleases?.let { return it }

        val fetched = fetchReleasesFromGitHub()
        if (fetched.isNotEmpty())
        {
            cachedReleases = fetched
            return fetched
        }

        return emptyList()
    }

    fun fetchReleasesFromGitHub(): List<String>
    {
        try
        {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_RELEASES_API_URL))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "IntelliLucee")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()

            val client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299)
            {
                val tagRegex = """"tag_name"\s*:\s*"([^"]+)"""".toRegex()
                val tags = tagRegex.findAll(response.body())
                    .map { it.groupValues[1].trim() }
                    .filter { it.isNotEmpty() }
                    .toList()
                if (tags.isNotEmpty())
                {
                    return tags
                }
            }
            else
            {
                LOG.warn("Failed to fetch releases from GitHub: HTTP ${response.statusCode()}")
            }
        }
        catch (exception: Exception)
        {
            LOG.warn("Failed to fetch CFML LSP releases from GitHub", exception)
        }
        return emptyList()
    }
}
