package com.quetwo.intellilucee.debugger

import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XSuspendContext

class CFMLSuspendContext(
    private val dapClient: CFMLDapClient,
    val stoppedEvent: DapStoppedEventBody,
    private val pathTransforms: List<PathTransform> = emptyList()
) : XSuspendContext() {

    private val executionStacks = mutableListOf<CFMLExecutionStack>()
    private var activeStack: CFMLExecutionStack? = null

    init {
        val threadId = stoppedEvent.threadId ?: 1L
        val thread = DapThread(threadId, "Thread $threadId")
        val stack = CFMLExecutionStack(dapClient, thread, pathTransforms)
        activeStack = stack
        executionStacks.add(stack)
    }

    override fun getActiveExecutionStack(): XExecutionStack? = activeStack

    override fun getExecutionStacks(): Array<XExecutionStack> = executionStacks.toTypedArray()

    override fun computeExecutionStacks(container: XExecutionStackContainer) {
        dapClient.threads().thenAccept { threads ->
            val stacks = mutableListOf<CFMLExecutionStack>()
            for (t in threads) {
                val existing = executionStacks.find { it.thread.id == t.id }
                val stack = existing ?: CFMLExecutionStack(dapClient, t, pathTransforms)
                stacks.add(stack)
            }
            if (stacks.isEmpty() && activeStack != null) {
                stacks.add(activeStack!!)
            }
            container.addExecutionStack(stacks, true)
        }.exceptionally {
            if (activeStack != null) {
                container.addExecutionStack(listOf(activeStack!!), true)
            } else {
                container.addExecutionStack(emptyList(), true)
            }
            null
        }
    }
}
