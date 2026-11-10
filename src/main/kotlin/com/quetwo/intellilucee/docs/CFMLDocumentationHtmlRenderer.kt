package com.quetwo.intellilucee.docs

import java.awt.Color
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.swing.UIManager

object CFMLDocumentationHtmlRenderer {
    fun render(
        entry: CFMLDocumentationEntry,
        foreground: Color = UIManager.getColor("EditorPane.foreground") ?: Color.BLACK,
        background: Color = UIManager.getColor("EditorPane.background") ?: Color.WHITE
    ): String = renderDocument(foreground, background) {
        append("<h1>").append(entry.name.htmlEscape()).append("</h1>")
        append("<p class=\"muted\">").append(if (entry.type == "tag") "Tag" else "Function")
            .append(" · <a href=\"https://cfdocs.org/")
            .append(URLEncoder.encode(entry.name, StandardCharsets.UTF_8).replace("+", "%20"))
            .append("\">CFDocs documentation</a></p>")
        appendParagraph(entry.description)

        appendSection("Syntax") {
            appendLabeledValue("Tag/function syntax", entry.syntax)
            appendLabeledValue("Script syntax", entry.script)
            appendLabeledValue("Member syntax", entry.member)
            entry.memberDetails?.let { memberDetails ->
                val summary = listOfNotNull(
                    memberDetails.name.takeIf(String::isNotBlank),
                    memberDetails.type.takeIf(String::isNotBlank)?.let { "for $it" },
                    memberDetails.returns.takeIf(String::isNotBlank)?.let { "returns $it" }
                ).joinToString(" · ")
                appendLabeledValue("Member details", summary)
            }
            appendLabeledValue("Returns", entry.returns)
        }

        appendSection("Engine compatibility") {
            if (entry.engines.isEmpty()) {
                appendParagraph("No engine compatibility data is listed.")
            } else {
                append("<table><tr><th>Engine</th><th>Versions and notes</th></tr>")
                entry.engines.toSortedMap().forEach { (engineId, details) ->
                    append("<tr><td>").append(engineName(engineId).htmlEscape()).append("</td><td>")
                    val versionText = when {
                        details.minimumVersion.isNotBlank() -> "Available from ${details.minimumVersion}"
                        else -> "Minimum version not specified"
                    }
                    append(versionText.htmlEscape())
                    if (details.deprecatedVersion.isNotBlank()) {
                        append("<br>Deprecated in ").append(details.deprecatedVersion.htmlEscape())
                    }
                    if (details.removedVersion.isNotBlank()) {
                        append("<br>Removed in ").append(details.removedVersion.htmlEscape())
                    }
                    if (details.notes.isNotBlank()) append("<br>").append(details.notes.toHtmlText())
                    if (details.docs.isNotBlank()) {
                        append("<br>").append(link(details.docs, "Engine documentation"))
                    }
                    append("</td></tr>")
                }
                append("</table>")
            }
        }

        appendSection("Parameters and attributes") {
            if (entry.parameters.isEmpty()) {
                appendParagraph("No parameter or attribute details are listed.")
            } else {
                append("<table><tr><th>Name</th><th>Type</th><th>Required</th><th>Description</th></tr>")
                entry.parameters.forEach { parameter ->
                    append("<tr><td><code>").append(parameter.name.htmlEscape()).append("</code></td>")
                    append("<td>").append(parameter.type.ifBlank { "—" }.htmlEscape()).append("</td>")
                    append("<td>").append(if (parameter.required) "Yes" else "No").append("</td>")
                    append("<td>").append(parameter.description.toHtmlText())
                    if (parameter.defaultValue.isNotBlank()) {
                        append("<br><span class=\"label\">Default:</span> ")
                            .append(parameter.defaultValue.htmlEscape())
                    }
                    if (parameter.values.isNotEmpty()) {
                        append("<br><span class=\"label\">Values:</span> ")
                            .append(parameter.values.joinToString(", ") { it.htmlEscape() })
                    }
                    append("</td></tr>")
                }
                append("</table>")
            }
        }

        appendSection("Examples") {
            if (entry.examples.isEmpty()) {
                appendParagraph("No examples are listed.")
            } else {
                entry.examples.forEach { example ->
                    if (example.title.isNotBlank()) {
                        append("<p class=\"label\">").append(example.title.htmlEscape()).append("</p>")
                    }
                    appendParagraph(example.description)
                    if (example.code.isNotBlank()) {
                        append("<pre><code>").append(example.code.htmlEscape()).append("</code></pre>")
                    }
                    if (example.result.isNotBlank()) {
                        append("<p><span class=\"label\">Result:</span> ")
                            .append(example.result.htmlEscape()).append("</p>")
                    }
                }
            }
        }

        if (entry.links.isNotEmpty()) {
            appendSection("Related documentation") {
                entry.links.forEach { docLink ->
                    val title = docLink.title.ifBlank { docLink.url }
                    append("<p>").append(link(docLink.url, title))
                    if (docLink.description.isNotBlank()) {
                        append("<br>").append(docLink.description.toHtmlText())
                    }
                    append("</p>")
                }
            }
        }
    }

    fun renderMessage(message: String, foreground: Color, background: Color): String =
        renderDocument(foreground, background) {
            append("<p>").append(message.htmlEscape()).append("</p>")
        }

    private fun renderDocument(foreground: Color, background: Color, content: StringBuilder.() -> Unit): String =
        buildString {
            val muted = foreground.mix(background, 0.55)
            val border = foreground.mix(background, 0.3)
            val codeBackground = background.mix(foreground, 0.08)
            append("<html><head><style>")
            append("body{font-family:sans-serif;font-size:12px;margin:12px;color:${foreground.toHtmlColor()};background-color:${background.toHtmlColor()};}")
            append("h1{font-size:20px;margin:0 0 4px;}h2{font-size:15px;margin:18px 0 6px;}")
            append("p{margin:5px 0;}table{border-collapse:collapse;width:100%;}")
            append("th,td{border:1px solid ${border.toHtmlColor()};padding:5px;vertical-align:top;text-align:left;}")
            append("a{color:${foreground.toHtmlColor()};text-decoration:underline;}")
            append("pre{white-space:pre-wrap;background-color:${codeBackground.toHtmlColor()};padding:8px;}")
            append(".muted{color:${muted.toHtmlColor()};}.label{font-weight:bold;}")
            append("</style></head><body>")
            content()
            append("</body></html>")
        }

    private fun StringBuilder.appendSection(title: String, content: StringBuilder.() -> Unit) {
        append("<h2>").append(title.htmlEscape()).append("</h2>")
        content()
    }

    private fun StringBuilder.appendLabeledValue(label: String, value: String) {
        if (value.isBlank()) return
        append("<p><span class=\"label\">").append(label.htmlEscape()).append(":</span> <code>")
            .append(value.htmlEscape()).append("</code></p>")
    }

    private fun StringBuilder.appendParagraph(text: String) {
        if (text.isNotBlank()) append("<p>").append(text.toHtmlText()).append("</p>")
    }

    private fun link(url: String, title: String): String {
        val uri = runCatching { URI(url) }.getOrNull()
        if (uri == null || uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            return title.htmlEscape()
        }
        return "<a href=\"${url.htmlEscape()}\">${title.htmlEscape()}</a>"
    }

    private fun engineName(engineId: String): String = when (engineId) {
        "coldfusion" -> "Adobe ColdFusion"
        "lucee" -> "Lucee"
        "railo" -> "Railo"
        "rustcfml" -> "RustCFML"
        "boxlang" -> "BoxLang"
        "openbd" -> "OpenBD"
        else -> engineId
    }

    private fun String.toHtmlText(): String = htmlEscape().replace("\r\n", "\n").replace("\n", "<br>")

    private fun Color.mix(other: Color, otherWeight: Double): Color {
        val thisWeight = 1.0 - otherWeight
        return Color(
            (red * thisWeight + other.red * otherWeight).toInt(),
            (green * thisWeight + other.green * otherWeight).toInt(),
            (blue * thisWeight + other.blue * otherWeight).toInt()
        )
    }

    private fun Color.toHtmlColor(): String = "#%02x%02x%02x".format(red, green, blue)

    private fun String.htmlEscape(): String = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}