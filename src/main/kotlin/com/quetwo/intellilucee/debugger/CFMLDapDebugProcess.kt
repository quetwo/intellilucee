package com.quetwo.intellilucee.debugger

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import java.net.InetSocketAddress
import java.net.Socket

class CFMLDapDebugProcess(
    session: XDebugSession,
    val configuration: CFMLDapRemoteConfiguration,
    private val processHandler: ProcessHandler,
    val dapClient: CFMLDapClient
) : XDebugProcess(session) {

    companion object {
        private val LOG: Logger = Logger.getInstance(CFMLDapDebugProcess::class.java)

        fun create(session: XDebugSession, configuration: CFMLDapRemoteConfiguration): CFMLDapDebugProcess {
            val socket = Socket()
            socket.connect(InetSocketAddress(configuration.host, configuration.port), 5000)
            val dapClient = CFMLDapClient(socket)
            val processHandler = CFMLDapProcessHandler(socket, dapClient)
            val debugProcess = CFMLDapDebugProcess(session, configuration, processHandler, dapClient)

            dapClient.startListening()
            dapClient.initialize("cfml").thenCompose {
                dapClient.attach(configuration.dapSecret, configuration.getEffectivePathTransforms())
            }.thenCompose {
                debugProcess.breakpointHandler.sendAllBreakpoints()
            }.thenCompose {
                dapClient.configurationDone()
            }.exceptionally { ex ->
                LOG.warn("DAP initialization or attach encountered error", ex)
                null
            }

            processHandler.startNotify()
            return debugProcess
        }
    }

    val breakpointHandler = CFMLDapBreakpointHandler(
        dapClient,
        configuration.getEffectivePathTransforms(),
        session.project
    )

    private val editorsProvider = CFMLDebuggerEditorsProvider()

    init {
        dapClient.onStopped = { stoppedEvent ->
            val suspendContext = CFMLSuspendContext(
                dapClient,
                stoppedEvent,
                configuration.getEffectivePathTransforms()
            )
            session.positionReached(suspendContext)
        }

        dapClient.onTerminated = {
            session.stop()
        }

        dapClient.onExited = {
            session.stop()
        }
    }

    override fun getEditorsProvider(): XDebuggerEditorsProvider = editorsProvider

    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> =
        arrayOf(breakpointHandler)

    override fun doGetProcessHandler(): ProcessHandler? = processHandler

    override fun createConsole(): ConsoleView {
        val console = TextConsoleBuilderFactory.getInstance().createBuilder(session.project).console
        console.attachToProcess(processHandler)
        return console
    }

    override fun sessionInitialized() {
        super.sessionInitialized()
        breakpointHandler.sendAllBreakpoints()
    }

    override fun startStepOver(context: XSuspendContext?) {
        val threadId = extractThreadId(context)
        dapClient.next(threadId)
    }

    override fun startStepInto(context: XSuspendContext?) {
        val threadId = extractThreadId(context)
        dapClient.stepIn(threadId)
    }

    override fun startStepOut(context: XSuspendContext?) {
        val threadId = extractThreadId(context)
        dapClient.stepOut(threadId)
    }

    override fun startPausing() {
        val activeStack = session.suspendContext?.activeExecutionStack as? CFMLExecutionStack
        val threadId = activeStack?.thread?.id ?: 1L
        dapClient.pause(threadId)
    }

    override fun resume(context: XSuspendContext?) {
        val threadId = extractThreadId(context)
        dapClient.continueThread(threadId)
    }

    override fun stop() {
        try {
            dapClient.disconnect()
        } catch (_: Exception) {
        }
        dapClient.close()
        processHandler.destroyProcess()
    }

    private fun extractThreadId(context: XSuspendContext?): Long {
        val suspendContext = context as? CFMLSuspendContext
            ?: session.suspendContext as? CFMLSuspendContext
        val activeStack = suspendContext?.activeExecutionStack as? CFMLExecutionStack
        return activeStack?.thread?.id
            ?: suspendContext?.stoppedEvent?.threadId
            ?: 1L
    }
}
