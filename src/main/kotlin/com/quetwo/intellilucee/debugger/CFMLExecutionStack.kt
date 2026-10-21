package com.quetwo.intellilucee.debugger

import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame

class CFMLExecutionStack(
    private val dapClient: CFMLDapClient,
    val thread: DapThread,
    private val pathTransforms: List<PathTransform> = emptyList()
) : XExecutionStack(thread.name) {

    private var topFrame: XStackFrame? = null

    override fun getTopFrame(): XStackFrame? = topFrame

    fun setTopFrame(frame: XStackFrame?) {
        this.topFrame = frame
    }

    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer) {
        dapClient.stackTrace(thread.id, firstFrameIndex, 50).thenAccept { frames ->
            val stackFrames = mutableListOf<CFMLStackFrame>()
            for (dapFrame in frames) {
                val sourcePos = resolveSourcePosition(dapFrame)
                val stackFrame = CFMLStackFrame(dapClient, dapFrame, sourcePos)
                stackFrames.add(stackFrame)
            }
            if (firstFrameIndex == 0 && stackFrames.isNotEmpty()) {
                topFrame = stackFrames.first()
            }
            container.addStackFrames(stackFrames, true)
        }.exceptionally { ex ->
            container.errorOccurred(ex.localizedMessage ?: "Failed to retrieve stack frames")
            null
        }
    }

    private fun resolveSourcePosition(frame: DapStackFrame): XSourcePosition? {
        val serverPath = frame.source?.path ?: return null
        val idePath = PathMapper.toIdePath(serverPath, pathTransforms)
        val virtualFile = PathMapper.findVirtualFile(idePath) ?: return null
        val lineIndex = maxOf(0, frame.line - 1)
        val colIndex = maxOf(0, frame.column - 1)
        return XDebuggerUtil.getInstance().createPosition(virtualFile, lineIndex, colIndex)
    }
}
