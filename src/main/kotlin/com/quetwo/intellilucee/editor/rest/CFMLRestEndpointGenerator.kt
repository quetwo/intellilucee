package com.quetwo.intellilucee.editor.rest

import com.intellij.openapi.editor.Document
import java.util.regex.Pattern

object CFMLRestEndpointGenerator {

    private val TAG_COMPONENT_PATTERN = Pattern.compile("(?i)<cfcomponent\\b([^>]*)>")
    private val SCRIPT_COMPONENT_PATTERN = Pattern.compile("(?i)(?:^|[\\s;{}])(?:(?:abstract|final)\\s+)?\\bcomponent\\b\\s*([^{;]*)")
    private val DOC_REST_PATTERN = Pattern.compile("(?i)/\\*\\*[\\s\\S]*?@rest(?:\\s*=\\s*|\\s+)(?:\"([^\"]*)\"|'([^']*)'|([^\\s*]+))[\\s\\S]*?\\*/\\s*(?:(?:abstract|final)\\s+)?\\bcomponent\\b")
    private val REST_ATTR_PATTERN = Pattern.compile("(?i)\\brest\\s*(?:=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s;{}>]+)))?")

    fun isRestComponent(text: String): Boolean {
        // 1. Check tag-based component: <cfcomponent ...>
        val tagMatcher = TAG_COMPONENT_PATTERN.matcher(text)
        if (tagMatcher.find()) {
            val attributes = tagMatcher.group(1) ?: ""
            val attrMatcher = REST_ATTR_PATTERN.matcher(attributes)
            if (attrMatcher.find()) {
                val value = (attrMatcher.group(1) ?: attrMatcher.group(2) ?: attrMatcher.group(3))?.trim()?.lowercase()
                return when (value) {
                    null, "", "true", "yes" -> true
                    "false", "no" -> false
                    else -> false
                }
            }
        }

        // 2. Check doc comment annotation before script component: /** @rest true */ component
        val docMatcher = DOC_REST_PATTERN.matcher(text)
        if (docMatcher.find()) {
            val value = (docMatcher.group(1) ?: docMatcher.group(2) ?: docMatcher.group(3))?.trim()?.lowercase()
            return value == "true" || value == "yes"
        }

        // 3. Check script-based component: component rest="true" ... {
        val scriptMatcher = SCRIPT_COMPONENT_PATTERN.matcher(text)
        if (scriptMatcher.find()) {
            val attributes = scriptMatcher.group(1) ?: ""
            val attrMatcher = REST_ATTR_PATTERN.matcher(attributes)
            if (attrMatcher.find()) {
                val value = (attrMatcher.group(1) ?: attrMatcher.group(2) ?: attrMatcher.group(3))?.trim()?.lowercase()
                return when (value) {
                    null, "", "true", "yes" -> true
                    "false", "no" -> false
                    else -> false
                }
            }
        }

        return false
    }

    fun generateFunction(descriptor: RestEndpointDescriptor): String {
        val argsString = descriptor.arguments
            .filter { it.argumentName.trim().isNotEmpty() }
            .joinToString(", ") { arg ->
                val type = arg.argumentType.trim()
                val name = arg.argumentName.trim()
                val source = arg.argumentSource.trim()
                val typePrefix = if (type.isNotEmpty()) "$type " else ""
                val sourceSuffix = if (source.isNotEmpty()) " restArgSource=\"$source\"" else ""
                "$typePrefix$name$sourceSuffix"
            }

        val sb = StringBuilder()
        sb.append("remote function ").append(descriptor.functionName.trim()).append("(").append(argsString).append(")")
        sb.append(" httpMethod=\"").append(descriptor.httpMethod.trim()).append("\"")
        if (descriptor.restPath.trim().isNotEmpty()) {
            sb.append(" restPath=\"").append(descriptor.restPath.trim()).append("\"")
        }
        if (descriptor.produces.trim().isNotEmpty()) {
            sb.append(" produces=\"").append(descriptor.produces.trim()).append("\"")
        }
        if (descriptor.consumes.trim().isNotEmpty()) {
            sb.append(" consumes=\"").append(descriptor.consumes.trim()).append("\"")
        }
        sb.append("\n")
        sb.append("    {\n")
        sb.append("\n")
        sb.append("        return {};\n")
        sb.append("    }")
        return sb.toString()
    }

    fun findInsertOffset(text: String): Int {
        val cfcomponentClose = text.lastIndexOf("</cfcomponent>", ignoreCase = true)
        if (cfcomponentClose >= 0) {
            return cfcomponentClose
        }

        val scriptClose = text.lastIndexOf('}')
        if (scriptClose >= 0) {
            return scriptClose
        }

        return text.length
    }

    fun insertEndpoint(document: Document, descriptor: RestEndpointDescriptor) {
        val text = document.text
        val functionText = generateFunction(descriptor)
        val insertOffset = findInsertOffset(text)
        val prefix = getLeadingSpacing(text, insertOffset)
        val suffix = getTrailingSpacing(text, insertOffset)
        val indented = "    $functionText"
        document.insertString(insertOffset, prefix + indented + suffix)
    }

    private fun getLeadingSpacing(text: String, insertOffset: Int): String {
        if (insertOffset == 0) return ""
        val before = text.substring(0, insertOffset)
        if (before.endsWith("\n\n") || before.endsWith("\r\n\r\n")) return ""
        if (before.endsWith("\n") || before.endsWith("\r\n")) return "\n"
        return "\n\n"
    }

    private fun getTrailingSpacing(text: String, insertOffset: Int): String {
        if (insertOffset >= text.length) return "\n"
        val after = text.substring(insertOffset)
        if (after.startsWith("\n\n") || after.startsWith("\r\n\r\n")) return ""
        if (after.startsWith("\n") || after.startsWith("\r\n")) return "\n"
        return "\n"
    }
}
