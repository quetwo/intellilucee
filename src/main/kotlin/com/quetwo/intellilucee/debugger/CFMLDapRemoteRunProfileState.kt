package com.quetwo.intellilucee.debugger

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

class CFMLDapRemoteRunProfileState(
    private val configuration: CFMLDapRemoteConfiguration
) : RunProfileState
{
    @Throws(ExecutionException::class)
    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult? =
        connectToDapServer(
            configuration.host,
            configuration.port,
            configuration.dapSecret,
            configuration.getEffectivePathTransforms(),
            configuration.project
        )

    @Throws(ExecutionException::class)
    private fun connectToDapServer(
        host: String,
        port: Int,
        secret: String,
        pathTransforms: List<PathTransform>,
        project: Project
    ): ExecutionResult?
    {
        var socket: Socket? = null
        try
        {
            val connectedSocket = Socket()
            socket = connectedSocket
            connectedSocket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            val output = connectedSocket.getOutputStream()
            var seq = 1

            val initializePayload = buildInitializePayload(seq++)
            sendDapMessage(output, initializePayload)

            val attachPayload = buildAttachPayload(seq++, secret, pathTransforms)
            sendDapMessage(output, attachPayload)

            val breakpointsByFile = collectLineBreakpoints(project)
            for ((filePath, lines) in breakpointsByFile)
            {
                val setBreakpointsPayload = buildSetBreakpointsPayload(seq++, filePath, lines)
                sendDapMessage(output, setBreakpointsPayload)
            }

            output.flush()

            val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
            val processHandler = CFMLDapProcessHandler(connectedSocket)
            console.attachToProcess(processHandler)
            processHandler.startNotify()

            return DefaultExecutionResult(console, processHandler)
        }
        catch (exception: Exception)
        {
            try
            {
                socket?.close()
            }
            catch (_: Exception)
            {
            }
            throw ExecutionException("Failed to connect to DAP server at $host:$port", exception)
        }
    }

    companion object
    {
        private const val CONNECT_TIMEOUT_MS = 5000

        fun escapeJson(value: String): String =
            value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")

        fun buildInitializePayload(seq: Int): String =
            """{"seq":$seq,"type":"request","command":"initialize","arguments":{"adapterID":"cfml"}}"""

        @JvmOverloads
        fun buildAttachPayload(
            seq: Int,
            secret: String,
            pathTransforms: List<PathTransform> = emptyList()
        ): String
        {
            val validTransforms = pathTransforms.filter { it.idePrefix.isNotBlank() || it.serverPrefix.isNotBlank() }
            val pathTransformsPart = if (validTransforms.isNotEmpty())
            {
                val transformsJson = validTransforms.joinToString(",") { transform ->
                    """{"idePrefix":"${escapeJson(transform.idePrefix)}","serverPrefix":"${escapeJson(transform.serverPrefix)}"}"""
                }
                ""","pathTransforms":[$transformsJson]"""
            }
            else
            {
                ""
            }
            return """{"seq":$seq,"type":"request","command":"attach","arguments":{"request":"attach","secret":"${escapeJson(secret)}"$pathTransformsPart}}"""
        }

        fun buildSetBreakpointsPayload(seq: Int, filePath: String, lines: List<Int>): String
        {
            val linesJson = lines.joinToString(",")
            val breakpointsJson = lines.joinToString(",") { """{"line":$it}""" }
            return """{"seq":$seq,"type":"request","command":"setBreakpoints","arguments":{"source":{"path":"${escapeJson(filePath)}"},"lines":[$linesJson],"breakpoints":[$breakpointsJson]}}"""
        }

        fun sendDapMessage(output: OutputStream, payload: String)
        {
            val bytes = payload.toByteArray(StandardCharsets.UTF_8)
            val header = "Content-Length: ${bytes.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
            output.write(header)
            output.write(bytes)

        }

        fun collectLineBreakpoints(project: Project): Map<String, List<Int>>
        {
            val breakpointManager = XDebuggerManager.getInstance(project)?.breakpointManager ?: return emptyMap()
            val breakpoints = breakpointManager.getBreakpoints(CFMLLineBreakpointType::class.java)

            val map = mutableMapOf<String, MutableList<Int>>()
            for (breakpoint in breakpoints)
            {
                if (!breakpoint.isEnabled) continue
                val url = breakpoint.fileUrl
                val path = if (url.startsWith("file://")) {
                    url.removePrefix("file://")
                } else if (breakpoint.presentableFilePath.isNotBlank()) {
                    breakpoint.presentableFilePath
                } else {
                    url
                }
                if (path.isBlank()) continue

                // IntelliJ line numbers are 0-based; DAP expects 1-based line numbers.
                val dapLine = breakpoint.line + 1
                map.computeIfAbsent(path) { mutableListOf() }.add(dapLine)
            }

            return map.mapValues { (_, lines) -> lines.distinct().sorted() }
        }
    }
}