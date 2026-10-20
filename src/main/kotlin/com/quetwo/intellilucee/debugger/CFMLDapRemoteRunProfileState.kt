package com.quetwo.intellilucee.debugger

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
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
        connectToDapServer(configuration.host, configuration.port, configuration.dapSecret, configuration.project)

    @Throws(ExecutionException::class)
    private fun connectToDapServer(host: String, port: Int, secret: String, project: Project): ExecutionResult?
    {
        try
        {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                val output = socket.getOutputStream()
                var seq = 1

                if (secret.isNotBlank())
                {
                    val initializePayload = buildInitializePayload(seq++, secret)
                    sendDapMessage(output, initializePayload)
                }

                val breakpointsByFile = collectLineBreakpoints(project)
                for ((filePath, lines) in breakpointsByFile)
                {
                    val setBreakpointsPayload = buildSetBreakpointsPayload(seq++, filePath, lines)
                    sendDapMessage(output, setBreakpointsPayload)
                }

                output.flush()
            }
        }
        catch (exception: Exception)
        {
            throw ExecutionException("Failed to connect to DAP server at $host:$port", exception)
        }

        return null
    }

    companion object
    {
        private const val CONNECT_TIMEOUT_MS = 5000

        fun escapeJson(value: String): String =
            value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")

        fun buildInitializePayload(seq: Int, secret: String): String =
            """{"seq":$seq,"type":"request","command":"initialize","arguments":{"adapterID":"cfml","dapSecret":"${escapeJson(secret)}"}}"""

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