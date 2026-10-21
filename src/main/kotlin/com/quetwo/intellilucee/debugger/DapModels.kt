package com.quetwo.intellilucee.debugger

import com.google.gson.JsonElement
import com.google.gson.JsonObject

data class DapResponse(
    val seq: Int,
    val requestSeq: Int,
    val command: String,
    val success: Boolean,
    val message: String? = null,
    val body: JsonObject? = null
)

data class DapEvent(
    val seq: Int,
    val event: String,
    val body: JsonObject? = null
)

data class DapThread(
    val id: Long,
    val name: String
) {
    companion object {
        fun fromJson(json: JsonObject): DapThread {
            val id = json.get("id")?.asLong ?: 0L
            val name = json.get("name")?.asString ?: "Thread $id"
            return DapThread(id, name)
        }
    }
}

data class DapSource(
    val name: String? = null,
    val path: String? = null,
    val sourceReference: Long? = null
) {
    companion object {
        fun fromJson(json: JsonObject?): DapSource? {
            if (json == null) return null
            val name = json.get("name")?.asStringOrNull()
            val path = json.get("path")?.asStringOrNull()
            val ref = json.get("sourceReference")?.asLongOrNull()
            return DapSource(name, path, ref)
        }
    }
}

data class DapStackFrame(
    val id: Long,
    val name: String,
    val source: DapSource? = null,
    val line: Int,
    val column: Int = 0,
    val endLine: Int? = null,
    val endColumn: Int? = null
) {
    companion object {
        fun fromJson(json: JsonObject): DapStackFrame {
            val id = json.get("id")?.asLong ?: 0L
            val name = json.get("name")?.asString ?: "frame"
            val source = DapSource.fromJson(json.getAsJsonObject("source"))
            val line = json.get("line")?.asInt ?: 1
            val col = json.get("column")?.asInt ?: 0
            val endLine = json.get("endLine")?.asIntOrNull()
            val endCol = json.get("endColumn")?.asIntOrNull()
            return DapStackFrame(id, name, source, line, col, endLine, endCol)
        }
    }
}

data class DapScope(
    val name: String,
    val variablesReference: Long,
    val expensive: Boolean = false,
    val namedVariables: Int? = null,
    val indexedVariables: Int? = null,
    val source: DapSource? = null,
    val line: Int? = null,
    val column: Int? = null
) {
    companion object {
        fun fromJson(json: JsonObject): DapScope {
            val name = json.get("name")?.asString ?: "Scope"
            val ref = json.get("variablesReference")?.asLong ?: 0L
            val expensive = json.get("expensive")?.asBoolean ?: false
            val named = json.get("namedVariables")?.asIntOrNull()
            val indexed = json.get("indexedVariables")?.asIntOrNull()
            val source = DapSource.fromJson(json.getAsJsonObject("source"))
            val line = json.get("line")?.asIntOrNull()
            val col = json.get("column")?.asIntOrNull()
            return DapScope(name, ref, expensive, named, indexed, source, line, col)
        }
    }
}

data class DapVariable(
    val name: String,
    val value: String,
    val type: String? = null,
    val variablesReference: Long = 0L,
    val namedVariables: Int? = null,
    val indexedVariables: Int? = null,
    val evaluateName: String? = null
) {
    companion object {
        fun fromJson(json: JsonObject): DapVariable {
            val name = json.get("name")?.asString ?: ""
            val value = json.get("value")?.asString ?: ""
            val type = json.get("type")?.asStringOrNull()
            val ref = json.get("variablesReference")?.asLong ?: 0L
            val named = json.get("namedVariables")?.asIntOrNull()
            val indexed = json.get("indexedVariables")?.asIntOrNull()
            val evalName = json.get("evaluateName")?.asStringOrNull()
            return DapVariable(name, value, type, ref, named, indexed, evalName)
        }
    }
}

data class DapBreakpoint(
    val id: Long? = null,
    val verified: Boolean = false,
    val message: String? = null,
    val source: DapSource? = null,
    val line: Int? = null,
    val column: Int? = null
) {
    companion object {
        fun fromJson(json: JsonObject): DapBreakpoint {
            val id = json.get("id")?.asLongOrNull()
            val verified = json.get("verified")?.asBoolean ?: false
            val message = json.get("message")?.asStringOrNull()
            val source = DapSource.fromJson(json.getAsJsonObject("source"))
            val line = json.get("line")?.asIntOrNull()
            val column = json.get("column")?.asIntOrNull()
            return DapBreakpoint(id, verified, message, source, line, column)
        }
    }
}

data class DapStoppedEventBody(
    val reason: String,
    val description: String? = null,
    val threadId: Long? = null,
    val text: String? = null,
    val allThreadsStopped: Boolean? = null
) {
    companion object {
        fun fromJson(json: JsonObject?): DapStoppedEventBody {
            if (json == null) return DapStoppedEventBody(reason = "unknown")
            val reason = json.get("reason")?.asString ?: "breakpoint"
            val desc = json.get("description")?.asStringOrNull()
            val threadId = json.get("threadId")?.asLongOrNull()
            val text = json.get("text")?.asStringOrNull()
            val allStopped = json.get("allThreadsStopped")?.asBooleanOrNull()
            return DapStoppedEventBody(reason, desc, threadId, text, allStopped)
        }
    }
}

data class DapOutputEventBody(
    val category: String? = null,
    val output: String,
    val variablesReference: Long? = null,
    val source: DapSource? = null,
    val line: Int? = null,
    val column: Int? = null
) {
    companion object {
        fun fromJson(json: JsonObject?): DapOutputEventBody {
            if (json == null) return DapOutputEventBody(output = "")
            val category = json.get("category")?.asStringOrNull()
            val output = json.get("output")?.asString ?: ""
            val ref = json.get("variablesReference")?.asLongOrNull()
            val source = DapSource.fromJson(json.getAsJsonObject("source"))
            val line = json.get("line")?.asIntOrNull()
            val column = json.get("column")?.asIntOrNull()
            return DapOutputEventBody(category, output, ref, source, line, column)
        }
    }
}

data class DapEvaluateResult(
    val result: String,
    val type: String? = null,
    val variablesReference: Long = 0L,
    val namedVariables: Int? = null,
    val indexedVariables: Int? = null
) {
    companion object {
        fun fromJson(json: JsonObject?): DapEvaluateResult {
            if (json == null) return DapEvaluateResult(result = "")
            val result = json.get("result")?.asString ?: ""
            val type = json.get("type")?.asStringOrNull()
            val ref = json.get("variablesReference")?.asLong ?: 0L
            val named = json.get("namedVariables")?.asIntOrNull()
            val indexed = json.get("indexedVariables")?.asIntOrNull()
            return DapEvaluateResult(result, type, ref, named, indexed)
        }
    }
}

// Extension helpers for JsonElement null safety
internal fun JsonElement.asStringOrNull(): String? = if (isJsonNull) null else asString
internal fun JsonElement.asLongOrNull(): Long? = if (isJsonNull) null else try { asLong } catch (_: Exception) { null }
internal fun JsonElement.asIntOrNull(): Int? = if (isJsonNull) null else try { asInt } catch (_: Exception) { null }
internal fun JsonElement.asBooleanOrNull(): Boolean? = if (isJsonNull) null else try { asBoolean } catch (_: Exception) { null }
