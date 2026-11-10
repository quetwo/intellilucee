package com.quetwo.intellilucee.docs

object CFMLDocumentationSnippetFormatter {
    fun name(entry: CFMLDocumentationEntry): String = entry.name

    fun withParameters(entry: CFMLDocumentationEntry): String = when (entry.type) {
        "function" -> "${entry.name}(${entry.parameters.mapNotNull { it.name.asParameterName() }.joinToString(", ")})"
        "tag" -> formatTag(entry)
        else -> entry.name
    }

    private fun formatTag(entry: CFMLDocumentationEntry): String {
        val attributes = entry.parameters.mapNotNull { it.name.asParameterName() }
        if (attributes.isEmpty()) {
            val documentedSyntax = entry.syntax.trim()
                .replace(Regex("\\s*\\[[^]]*]"), "")
                .trim()
            if (documentedSyntax.isNotEmpty()) return documentedSyntax
        }

        val name = entry.name
        val openingTag = buildString {
            append('<').append(name)
            attributes.forEach { append(' ').append(it).append("=\"\"") }
            append('>')
        }
        val syntax = entry.syntax
        return when {
            syntax.contains(Regex("</\\s*${Regex.escape(name)}\\s*>", RegexOption.IGNORE_CASE)) ->
                "$openingTag\n\n</$name>"
            syntax.contains(Regex("<\\s*${Regex.escape(name)}\\b[^>]*?/\\s*>", RegexOption.IGNORE_CASE)) ->
                openingTag.dropLast(1) + "/>"
            else -> openingTag
        }
    }

    private fun String.asParameterName(): String? = trim().takeIf(String::isNotEmpty)
}