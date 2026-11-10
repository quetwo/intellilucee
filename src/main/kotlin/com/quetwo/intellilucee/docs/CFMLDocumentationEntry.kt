package com.quetwo.intellilucee.docs

import com.google.gson.JsonElement
import com.google.gson.JsonObject

data class CFMLDocumentationEntry(
    val name: String,
    val type: String,
    val description: String,
    val syntax: String,
    val script: String,
    val member: String,
    val returns: String,
    val memberDetails: MemberDocumentation?,
    val engines: Map<String, EngineDocumentation>,
    val parameters: List<ParameterDocumentation>,
    val examples: List<DocumentationExample>,
    val links: List<DocumentationLink>
) {
    companion object {
        fun fromJson(json: JsonObject): CFMLDocumentationEntry? {
            val name = json.string("name").trim()
            val type = json.string("type").trim().lowercase()
            if (name.isEmpty() || type !in setOf("function", "tag")) return null

            val engines = json.objectValue("engines")?.entrySet()
                ?.mapNotNull { (engine, value) ->
                    value.takeIf { it.isJsonObject }
                        ?.asJsonObject
                        ?.let { engine.lowercase() to it.toEngineDocumentation() }
                }
                ?.toMap()
                .orEmpty()

            return CFMLDocumentationEntry(
                name = name,
                type = type,
                description = json.string("description"),
                syntax = json.string("syntax"),
                script = json.string("script"),
                member = json.string("member"),
                returns = json.string("returns"),
                memberDetails = json.objectValue("member_details")?.let {
                    MemberDocumentation(
                        name = it.string("name"),
                        type = it.string("type"),
                        returns = it.string("returns")
                    )
                },
                engines = engines,
                parameters = json.arrayValue("params").mapNotNull { element ->
                    element.takeIf { it.isJsonObject }?.asJsonObject?.toParameterDocumentation()
                },
                examples = json.arrayValue("examples").mapNotNull { element ->
                    element.takeIf { it.isJsonObject }?.asJsonObject?.toDocumentationExample()
                },
                links = json.arrayValue("links").mapNotNull { element ->
                    element.takeIf { it.isJsonObject }?.asJsonObject?.toDocumentationLink()
                }
            )
        }
    }
}

data class EngineDocumentation(
    val minimumVersion: String,
    val deprecatedVersion: String,
    val removedVersion: String,
    val notes: String,
    val docs: String
)

data class MemberDocumentation(
    val name: String,
    val type: String,
    val returns: String
)

data class ParameterDocumentation(
    val name: String,
    val description: String,
    val type: String,
    val required: Boolean,
    val defaultValue: String,
    val values: List<String>
)

data class DocumentationExample(
    val title: String,
    val description: String,
    val code: String,
    val result: String
)

data class DocumentationLink(
    val title: String,
    val url: String,
    val description: String
)

private fun JsonObject.toEngineDocumentation() = EngineDocumentation(
    minimumVersion = string("minimum_version"),
    deprecatedVersion = string("deprecated"),
    removedVersion = string("removed"),
    notes = string("notes"),
    docs = string("docs")
)

private fun JsonObject.toParameterDocumentation() = ParameterDocumentation(
    name = string("name"),
    description = string("description"),
    type = string("type"),
    required = get("required")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
    defaultValue = get("default")?.primitiveText().orEmpty(),
    values = arrayValue("values").mapNotNull { it.primitiveText() }
)

private fun JsonObject.toDocumentationExample() = DocumentationExample(
    title = string("title"),
    description = string("description"),
    code = string("code"),
    result = string("result")
)

private fun JsonObject.toDocumentationLink() = DocumentationLink(
    title = string("title"),
    url = string("url"),
    description = string("description")
)

private fun JsonObject.string(name: String): String = get(name)
    ?.takeUnless { it.isJsonNull }
    ?.takeIf { it.isJsonPrimitive }
    ?.asString
    .orEmpty()

private fun JsonObject.objectValue(name: String): JsonObject? = get(name)
    ?.takeIf { it.isJsonObject }
    ?.asJsonObject

private fun JsonObject.arrayValue(name: String): List<JsonElement> = get(name)
    ?.takeIf { it.isJsonArray }
    ?.asJsonArray
    ?.map { it }
    .orEmpty()

private fun JsonElement.primitiveText(): String? = takeIf { it.isJsonPrimitive }
    ?.asString