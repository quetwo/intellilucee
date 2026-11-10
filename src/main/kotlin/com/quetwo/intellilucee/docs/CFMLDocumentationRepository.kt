package com.quetwo.intellilucee.docs

import com.google.gson.JsonParser
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object CFMLDocumentationRepository {
    fun loadEntries(classLoader: ClassLoader = CFMLDocumentationRepository::class.java.classLoader): List<CFMLDocumentationEntry> {
        val indexStream = classLoader.getResourceAsStream("cfdocs/index.txt")
            ?: error("CFDocs index resource is missing")
        val filenames = InputStreamReader(indexStream, StandardCharsets.UTF_8).use { reader ->
            reader.buffered().readLines()
                .map(String::trim)
                .filter { it.isNotEmpty() && it.endsWith(".json") && '/' !in it && '\\' !in it }
        }

        return filenames.mapNotNull { filename ->
            val recordStream = classLoader.getResourceAsStream("cfdocs/data/en/$filename")
                ?: error("CFDocs record resource is missing: $filename")
            InputStreamReader(recordStream, StandardCharsets.UTF_8).use { reader ->
                JsonParser.parseReader(reader)
                    .takeIf { it.isJsonObject }
                    ?.asJsonObject
                    ?.let(CFMLDocumentationEntry::fromJson)
            }
        }.sortedBy { it.name.lowercase() }
    }
}