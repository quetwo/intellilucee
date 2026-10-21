package com.quetwo.intellilucee.debugger

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class DapException(message: String, val response: DapResponse? = null) : RuntimeException(message)

class CFMLDapClient(
    private val socket: Socket? = null,
    private val customInputStream: InputStream? = null,
    private val customOutputStream: OutputStream? = null
) : Closeable {

    companion object {
        private val LOG: Logger = Logger.getInstance(CFMLDapClient::class.java)
    }

    private val inputStream: InputStream?
        get() = customInputStream ?: if (socket != null && !socket.isClosed) socket.getInputStream() else null

    private val outputStream: OutputStream?
        get() = customOutputStream ?: if (socket != null && !socket.isClosed) socket.getOutputStream() else null

    private val seqGenerator = AtomicInteger(1)
    private val pendingRequests = ConcurrentHashMap<Int, CompletableFuture<DapResponse>>()
    private val outputLock = Any()

    @Volatile
    private var running = false

    @Volatile
    private var closed = false

    // Event listeners
    var onStopped: ((DapStoppedEventBody) -> Unit)? = null
    var onContinued: ((Long?, Boolean?) -> Unit)? = null
    var onOutput: ((DapOutputEventBody) -> Unit)? = null
    var onBreakpoint: ((String, DapBreakpoint) -> Unit)? = null
    var onThread: ((String, Long) -> Unit)? = null
    var onTerminated: (() -> Unit)? = null
    var onExited: ((Int) -> Unit)? = null
    var onRawOutput: ((String) -> Unit)? = null

    val isConnected: Boolean
        get() = !closed && (socket == null || (socket.isConnected && !socket.isClosed))

    fun startListening() {
        if (running) return
        running = true
        val thread = Thread({
            try {
                val input = inputStream ?: return@Thread
                var contentLength = -1
                val lineBuffer = ByteArrayOutputStream()

                while (running && !closed) {
                    val b = input.read()
                    if (b == -1) break

                    if (b == '\n'.code) {
                        var lineBytes = lineBuffer.toByteArray()
                        lineBuffer.reset()
                        if (lineBytes.isNotEmpty() && lineBytes.last() == '\r'.code.toByte()) {
                            lineBytes = lineBytes.copyOf(lineBytes.size - 1)
                        }
                        val line = String(lineBytes, StandardCharsets.UTF_8)

                        if (line.isEmpty()) {
                            // Empty line marks end of headers
                            if (contentLength > 0) {
                                val bodyBytes = ByteArray(contentLength)
                                var total = 0
                                while (total < contentLength) {
                                    val read = input.read(bodyBytes, total, contentLength - total)
                                    if (read == -1) break
                                    total += read
                                }
                                contentLength = -1
                                if (total == bodyBytes.size) {
                                    val bodyJson = String(bodyBytes, StandardCharsets.UTF_8)
                                    handleIncomingMessage(bodyJson)
                                }
                            }
                        } else if (line.startsWith("Content-Length:", ignoreCase = true)) {
                            contentLength = line.substringAfter("Content-Length:").trim().toIntOrNull() ?: -1
                        } else if (line.startsWith("Content-Type:", ignoreCase = true)) {
                            // ignore optional Content-Type header
                        } else {
                            // Raw non-DAP output line
                            onRawOutput?.invoke(line + "\n")
                        }
                    } else {
                        lineBuffer.write(b)
                    }
                }

                if (lineBuffer.size() > 0) {
                    val remaining = lineBuffer.toString(StandardCharsets.UTF_8.name())
                    onRawOutput?.invoke(remaining)
                }
            } catch (_: Exception) {
                // Connection closed or interrupted
            } finally {
                running = false
                for ((_, pending) in pendingRequests) {
                    pending.completeExceptionally(IOException("DAP connection closed"))
                }
                pendingRequests.clear()
                onTerminated?.invoke()
            }
        }, "CFMLDapClient-Reader")
        thread.isDaemon = true
        thread.start()
    }

    private fun handleIncomingMessage(jsonString: String) {
        LOG.debug("[LUCEE-DAP] <= $jsonString")
        val json = try {
            JsonParser.parseString(jsonString).asJsonObject
        } catch (e: Exception) {
            LOG.warn("Failed to parse DAP JSON: $jsonString", e)
            return
        }

        val type = json.get("type")?.asString ?: return
        when (type) {
            "response" -> {
                val seq = json.get("seq")?.asInt ?: 0
                val reqSeq = json.get("request_seq")?.asInt ?: 0
                val command = json.get("command")?.asString ?: ""
                val success = json.get("success")?.asBoolean ?: false
                val message = json.get("message")?.asStringOrNull()
                val body = json.getAsJsonObject("body")
                val response = DapResponse(seq, reqSeq, command, success, message, body)

                val pending = pendingRequests.remove(reqSeq)
                if (pending != null) {
                    if (success) {
                        pending.complete(response)
                    } else {
                        pending.completeExceptionally(
                            DapException(message ?: "DAP command '$command' failed", response)
                        )
                    }
                }
            }
            "event" -> {
                val seq = json.get("seq")?.asInt ?: 0
                val event = json.get("event")?.asString ?: ""
                val body = json.getAsJsonObject("body")
                val dapEvent = DapEvent(seq, event, body)
                handleEvent(dapEvent)
            }
        }
    }

    private fun handleEvent(event: DapEvent) {
        when (event.event) {
            "stopped" -> {
                val stoppedBody = DapStoppedEventBody.fromJson(event.body)
                onStopped?.invoke(stoppedBody)
            }
            "continued" -> {
                val threadId = event.body?.get("threadId")?.asLongOrNull()
                val allContinued = event.body?.get("allThreadsContinued")?.asBooleanOrNull()
                onContinued?.invoke(threadId, allContinued)
            }
            "output" -> {
                val outputBody = DapOutputEventBody.fromJson(event.body)
                onOutput?.invoke(outputBody)
            }
            "breakpoint" -> {
                val reason = event.body?.get("reason")?.asString ?: "changed"
                val bpJson = event.body?.getAsJsonObject("breakpoint")
                if (bpJson != null) {
                    onBreakpoint?.invoke(reason, DapBreakpoint.fromJson(bpJson))
                }
            }
            "thread" -> {
                val reason = event.body?.get("reason")?.asString ?: "started"
                val threadId = event.body?.get("threadId")?.asLong ?: 0L
                onThread?.invoke(reason, threadId)
            }
            "terminated" -> {
                onTerminated?.invoke()
            }
            "exited" -> {
                val exitCode = event.body?.get("exitCode")?.asInt ?: 0
                onExited?.invoke(exitCode)
            }
        }
    }

    fun sendRequest(command: String, arguments: JsonObject? = null): CompletableFuture<DapResponse> {
        val seq = seqGenerator.getAndIncrement()
        val future = CompletableFuture<DapResponse>()
        pendingRequests[seq] = future

        val requestObj = JsonObject().apply {
            addProperty("seq", seq)
            addProperty("type", "request")
            addProperty("command", command)
            if (arguments != null) {
                add("arguments", arguments)
            }
        }
        val jsonString = requestObj.toString()
        sendRawMessage(jsonString)
        return future
    }

    fun sendRawMessage(payload: String) {
        LOG.debug("[LUCEE-DAP] => $payload")
        val bytes = payload.toByteArray(StandardCharsets.UTF_8)
        val header = "Content-Length: ${bytes.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
        synchronized(outputLock) {
            val out = outputStream ?: throw IOException("Output stream is not available")
            out.write(header)
            out.write(bytes)
            out.flush()
        }
    }

    fun initialize(adapterId: String = "cfml"): CompletableFuture<DapResponse> {
        val args = JsonObject().apply {
            addProperty("adapterID", adapterId)
            addProperty("pathFormat", "path")
            addProperty("linesStartAt1", true)
            addProperty("columnsStartAt1", true)
            addProperty("supportsVariableType", true)
            addProperty("supportsVariablePaging", false)
            addProperty("supportsRunInTerminalRequest", false)
        }
        return sendRequest("initialize", args)
    }

    fun attach(secret: String, pathTransforms: List<PathTransform> = emptyList()): CompletableFuture<DapResponse> {
        val args = JsonObject().apply {
            addProperty("request", "attach")
            addProperty("secret", secret)
            val validTransforms = pathTransforms.filter { it.idePrefix.isNotBlank() || it.serverPrefix.isNotBlank() }
            if (validTransforms.isNotEmpty()) {
                val arr = JsonArray()
                for (t in validTransforms) {
                    val obj = JsonObject().apply {
                        addProperty("idePrefix", t.idePrefix)
                        addProperty("serverPrefix", t.serverPrefix)
                    }
                    arr.add(obj)
                }
                add("pathTransforms", arr)
            }
        }
        return sendRequest("attach", args)
    }

    fun setBreakpoints(sourcePath: String, lines: List<Int>): CompletableFuture<List<DapBreakpoint>> {
        val args = JsonObject().apply {
            val sourceObj = JsonObject().apply {
                addProperty("path", sourcePath)
            }
            add("source", sourceObj)

            val linesArray = JsonArray()
            val bpArray = JsonArray()
            for (line in lines) {
                linesArray.add(line)
                val bpObj = JsonObject().apply {
                    addProperty("line", line)
                }
                bpArray.add(bpObj)
            }
            add("lines", linesArray)
            add("breakpoints", bpArray)
        }

        return sendRequest("setBreakpoints", args).thenApply { response ->
            val breakpoints = mutableListOf<DapBreakpoint>()
            val bpArr = response.body?.getAsJsonArray("breakpoints")
            if (bpArr != null) {
                for (element in bpArr) {
                    if (element.isJsonObject) {
                        breakpoints.add(DapBreakpoint.fromJson(element.asJsonObject))
                    }
                }
            }
            breakpoints
        }
    }

    fun configurationDone(): CompletableFuture<DapResponse> {
        return sendRequest("configurationDone")
    }

    fun threads(): CompletableFuture<List<DapThread>> {
        return sendRequest("threads").thenApply { response ->
            val list = mutableListOf<DapThread>()
            val arr = response.body?.getAsJsonArray("threads")
            if (arr != null) {
                for (element in arr) {
                    if (element.isJsonObject) {
                        list.add(DapThread.fromJson(element.asJsonObject))
                    }
                }
            }
            list
        }
    }

    fun stackTrace(threadId: Long, startFrame: Int = 0, levels: Int = 50): CompletableFuture<List<DapStackFrame>> {
        val args = JsonObject().apply {
            addProperty("threadId", threadId)
            addProperty("startFrame", startFrame)
            addProperty("levels", levels)
        }
        return sendRequest("stackTrace", args).thenApply { response ->
            val list = mutableListOf<DapStackFrame>()
            val arr = response.body?.getAsJsonArray("stackFrames")
            if (arr != null) {
                for (element in arr) {
                    if (element.isJsonObject) {
                        list.add(DapStackFrame.fromJson(element.asJsonObject))
                    }
                }
            }
            list
        }
    }

    fun scopes(frameId: Long): CompletableFuture<List<DapScope>> {
        val args = JsonObject().apply {
            addProperty("frameId", frameId)
        }
        return sendRequest("scopes", args).thenApply { response ->
            val list = mutableListOf<DapScope>()
            val arr = response.body?.getAsJsonArray("scopes")
            if (arr != null) {
                for (element in arr) {
                    if (element.isJsonObject) {
                        list.add(DapScope.fromJson(element.asJsonObject))
                    }
                }
            }
            list
        }
    }

    fun variables(variablesReference: Long): CompletableFuture<List<DapVariable>> {
        val args = JsonObject().apply {
            addProperty("variablesReference", variablesReference)
        }
        return sendRequest("variables", args).thenApply { response ->
            val list = mutableListOf<DapVariable>()
            val arr = response.body?.getAsJsonArray("variables")
            if (arr != null) {
                for (element in arr) {
                    if (element.isJsonObject) {
                        list.add(DapVariable.fromJson(element.asJsonObject))
                    }
                }
            }
            list
        }
    }

    fun evaluate(expression: String, frameId: Long? = null, context: String? = null): CompletableFuture<DapEvaluateResult> {
        val args = JsonObject().apply {
            addProperty("expression", expression)
            if (frameId != null) {
                addProperty("frameId", frameId)
            }
            if (context != null) {
                addProperty("context", context)
            }
        }
        return sendRequest("evaluate", args).thenApply { response ->
            DapEvaluateResult.fromJson(response.body)
        }
    }

    fun setVariable(variablesReference: Long, name: String, value: String): CompletableFuture<DapVariable> {
        val args = JsonObject().apply {
            addProperty("variablesReference", variablesReference)
            addProperty("name", name)
            addProperty("value", value)
        }
        return sendRequest("setVariable", args).thenApply { response ->
            DapVariable.fromJson(response.body ?: JsonObject())
        }
    }

    fun next(threadId: Long): CompletableFuture<DapResponse> {
        val args = JsonObject().apply {
            addProperty("threadId", threadId)
        }
        return sendRequest("next", args)
    }

    fun stepIn(threadId: Long): CompletableFuture<DapResponse> {
        val args = JsonObject().apply {
            addProperty("threadId", threadId)
        }
        return sendRequest("stepIn", args)
    }

    fun stepOut(threadId: Long): CompletableFuture<DapResponse> {
        val args = JsonObject().apply {
            addProperty("threadId", threadId)
        }
        return sendRequest("stepOut", args)
    }

    fun continueThread(threadId: Long): CompletableFuture<DapResponse> {
        val args = JsonObject().apply {
            addProperty("threadId", threadId)
        }
        return sendRequest("continue", args)
    }

    fun pause(threadId: Long): CompletableFuture<DapResponse> {
        val args = JsonObject().apply {
            addProperty("threadId", threadId)
        }
        return sendRequest("pause", args)
    }

    fun disconnect(terminateDebuggee: Boolean = false): CompletableFuture<DapResponse> {
        val args = JsonObject().apply {
            addProperty("restart", false)
            addProperty("terminateDebuggee", terminateDebuggee)
        }
        return sendRequest("disconnect", args)
    }

    fun terminate(): CompletableFuture<DapResponse> {
        val args = JsonObject().apply {
            addProperty("restart", false)
        }
        return sendRequest("terminate", args)
    }

    override fun close() {
        closed = true
        running = false
        try {
            if (socket != null && !socket.isClosed) {
                socket.close()
            }
        } catch (_: Exception) {
        }
    }
}
