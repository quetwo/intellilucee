package com.quetwo.intellilucee.debugger

import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import java.util.concurrent.CompletableFuture

class CFMLDapBreakpointHandler(
    private val dapClient: CFMLDapClient,
    private val pathTransforms: List<PathTransform>,
    private val project: Project
) : XBreakpointHandler<XLineBreakpoint<CFMLLineBreakpointProperties>>(CFMLLineBreakpointType::class.java) {

    override fun registerBreakpoint(breakpoint: XLineBreakpoint<CFMLLineBreakpointProperties>) {
        if (!dapClient.isConnected) return
        val path = getFilePath(breakpoint)
        if (path.isNotBlank()) {
            sendBreakpointsForFilePath(path)
        }
    }

    override fun unregisterBreakpoint(breakpoint: XLineBreakpoint<CFMLLineBreakpointProperties>, temporary: Boolean) {
        if (!dapClient.isConnected) return
        val path = getFilePath(breakpoint)
        if (path.isNotBlank()) {
            sendBreakpointsForFilePath(path, excludeBreakpoint = if (temporary) null else breakpoint)
        }
    }

    fun sendAllBreakpoints(): CompletableFuture<Unit> {
        val breakpointsByFile = collectAllBreakpointsByFile()
        val futures = mutableListOf<CompletableFuture<*>>()
        for ((filePath, lines) in breakpointsByFile) {
            val serverPath = PathMapper.toServerPath(filePath, pathTransforms)
            val future = dapClient.setBreakpoints(serverPath, lines)
            futures.add(future)
        }
        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply { }
    }

    fun sendBreakpointsForFilePath(
        filePath: String,
        excludeBreakpoint: XLineBreakpoint<CFMLLineBreakpointProperties>? = null
    ): CompletableFuture<List<DapBreakpoint>> {
        val breakpointManager = XDebuggerManager.getInstance(project)?.breakpointManager
        val allBreakpoints = breakpointManager?.getBreakpoints(CFMLLineBreakpointType::class.java) ?: emptyList()

        val lines = mutableListOf<Int>()
        for (bp in allBreakpoints) {
            if (bp == excludeBreakpoint) continue
            if (!bp.isEnabled) continue
            val bpPath = getFilePath(bp)
            if (normalizePath(bpPath) == normalizePath(filePath)) {
                lines.add(bp.line + 1)
            }
        }

        val sortedLines = lines.distinct().sorted()
        val serverPath = PathMapper.toServerPath(filePath, pathTransforms)
        return dapClient.setBreakpoints(serverPath, sortedLines)
    }

    private fun collectAllBreakpointsByFile(): Map<String, List<Int>> {
        val breakpointManager = XDebuggerManager.getInstance(project)?.breakpointManager ?: return emptyMap()
        val breakpoints = breakpointManager.getBreakpoints(CFMLLineBreakpointType::class.java)

        val map = mutableMapOf<String, MutableList<Int>>()
        for (breakpoint in breakpoints) {
            if (!breakpoint.isEnabled) continue
            val path = getFilePath(breakpoint)
            if (path.isBlank()) continue
            val dapLine = breakpoint.line + 1
            map.computeIfAbsent(path) { mutableListOf() }.add(dapLine)
        }

        return map.mapValues { (_, lines) -> lines.distinct().sorted() }
    }

    private fun getFilePath(breakpoint: XLineBreakpoint<CFMLLineBreakpointProperties>): String {
        val url = breakpoint.fileUrl
        val path = com.intellij.openapi.vfs.VfsUtilCore.urlToPath(url)
        return if (path.isNotBlank()) {
            path
        } else if (breakpoint.presentableFilePath.isNotBlank()) {
            breakpoint.presentableFilePath
        } else {
            url
        }
    }

    private fun normalizePath(path: String): String =
        path.replace('\\', '/').lowercase()
}
