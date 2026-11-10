package com.quetwo.intellilucee.docs

import java.math.BigInteger

enum class DocumentationTypeFilter(val label: String) {
    ALL("All types"),
    FUNCTIONS("Functions"),
    TAGS("Tags")
}

object CFMLDocumentationFilter {
    private val versionPattern = Regex("\\d+(?:\\.\\d+)*")

    fun filter(
        entries: List<CFMLDocumentationEntry>,
        query: String = "",
        type: DocumentationTypeFilter = DocumentationTypeFilter.ALL,
        engineId: String? = null,
        version: String = ""
    ): List<CFMLDocumentationEntry> {
        val requestedVersion = versionParts(version) ?: if (version.isBlank()) null else return emptyList()
        val normalizedQuery = query.trim().lowercase()
        val normalizedEngine = engineId?.trim()?.lowercase().orEmpty()

        return entries.asSequence()
            .filter { entry ->
                when (type) {
                    DocumentationTypeFilter.ALL -> true
                    DocumentationTypeFilter.FUNCTIONS -> entry.type == "function"
                    DocumentationTypeFilter.TAGS -> entry.type == "tag"
                }
            }
            .filter { entry ->
                normalizedQuery.isEmpty() || entry.name.lowercase().contains(normalizedQuery) ||
                    entry.description.lowercase().contains(normalizedQuery)
            }
            .filter { entry ->
                if (normalizedEngine.isEmpty()) return@filter true
                val engine = entry.engines[normalizedEngine] ?: return@filter false
                if (requestedVersion == null) return@filter true

                val minimum = versionParts(engine.minimumVersion)
                if (minimum != null && compareVersions(requestedVersion, minimum) < 0) return@filter false

                val removed = versionParts(engine.removedVersion)
                removed == null || compareVersions(requestedVersion, removed) < 0
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    fun versionError(version: String): String? = when {
        version.isBlank() -> null
        versionParts(version) == null -> "Enter a numeric version such as 5 or 5.4.3."
        else -> null
    }

    fun compareVersions(left: String, right: String): Int? {
        val leftParts = versionParts(left) ?: return null
        val rightParts = versionParts(right) ?: return null
        return compareVersions(leftParts, rightParts)
    }

    private fun compareVersions(left: List<BigInteger>, right: List<BigInteger>): Int {
        val parts = maxOf(left.size, right.size)
        for (index in 0 until parts) {
            val comparison = (left.getOrNull(index) ?: BigInteger.ZERO)
                .compareTo(right.getOrNull(index) ?: BigInteger.ZERO)
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun versionParts(version: String): List<BigInteger>? {
        val normalized = version.trim()
        if (normalized.isEmpty() || !versionPattern.matches(normalized)) return null
        return normalized.split('.').map { it.toBigIntegerOrNull() ?: return null }
    }
}