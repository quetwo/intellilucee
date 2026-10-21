package com.quetwo.intellilucee.debugger

import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.xdebugger.XDebuggerManager
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.ServerSocket
import java.util.concurrent.Executors

@RunWith(JUnit4::class)
class CFMLDapDebuggerRunnerTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            val debugExecutor = DefaultDebugExecutor.getDebugExecutorInstance()
            val runContentManager = com.intellij.execution.ui.RunContentManager.getInstance(project)
            for (desc in runContentManager.allDescriptors.toList()) {
                runContentManager.removeRunContent(debugExecutor, desc)
            }
            val editorFactory = com.intellij.openapi.editor.EditorFactory.getInstance()
            for (editor in editorFactory.allEditors) {
                if (!editor.isDisposed) {
                    try {
                        editorFactory.releaseEditor(editor)
                    } catch (_: Exception) {
                    }
                }
            }
        } finally {
            super.tearDown()
        }
    }

    @Test
    fun testRunnerId() {
        val runner = CFMLDapDebuggerRunner()
        assertEquals("CFMLDapDebuggerRunner", runner.runnerId)
    }

    @Test
    fun testCanRunForDebugAndRunExecutors() {
        val runner = CFMLDapDebuggerRunner()
        val configType = CFMLDapRemoteConfigurationType()
        val factory = configType.configurationFactories[0]
        val configuration = CFMLDapRemoteConfiguration(project, factory, "Test DAP")

        assertTrue(runner.canRun(DefaultDebugExecutor.EXECUTOR_ID, configuration))
        assertTrue(runner.canRun(DefaultRunExecutor.EXECUTOR_ID, configuration))
        assertFalse(runner.canRun("UnknownExecutor", configuration))
    }

    @Test
    fun testCanRunReturnsFalseForOtherConfigurations() {
        val runner = CFMLDapDebuggerRunner()
        val dummyProfile = object : com.intellij.execution.configurations.RunProfile {
            override fun getState(executor: com.intellij.execution.Executor, environment: com.intellij.execution.runners.ExecutionEnvironment) = null
            override fun getName(): String = "Dummy"
            override fun getIcon() = null
        }

        assertFalse(runner.canRun(DefaultDebugExecutor.EXECUTOR_ID, dummyProfile))
        assertFalse(runner.canRun(DefaultRunExecutor.EXECUTOR_ID, dummyProfile))
    }

    @Test
    fun testDoExecuteStartsDebugSessionAndCreatesDescriptor() {
        val serverSocket = ServerSocket(0)
        val port = serverSocket.localPort

        val executorService = Executors.newSingleThreadExecutor()
        executorService.submit {
            try {
                serverSocket.use { server ->
                    server.soTimeout = 5000
                    val client = server.accept()
                    client.soTimeout = 5000
                    // Read initialize request
                    val input = client.getInputStream()
                    val output = client.getOutputStream()
                    // Send initialize response
                    val initResp = """{"seq":1,"type":"response","request_seq":1,"command":"initialize","success":true,"body":{}}"""
                    output.write("Content-Length: ${initResp.length}\r\n\r\n$initResp".toByteArray(Charsets.UTF_8))
                    output.flush()

                    // Send attach response
                    val attachResp = """{"seq":2,"type":"response","request_seq":2,"command":"attach","success":true,"body":{}}"""
                    output.write("Content-Length: ${attachResp.length}\r\n\r\n$attachResp".toByteArray(Charsets.UTF_8))
                    output.flush()

                    // Send configurationDone response
                    val confResp = """{"seq":3,"type":"response","request_seq":3,"command":"configurationDone","success":true,"body":{}}"""
                    output.write("Content-Length: ${confResp.length}\r\n\r\n$confResp".toByteArray(Charsets.UTF_8))
                    output.flush()

                    Thread.sleep(500)
                }
            } catch (_: Exception) {
            }
        }

        val configType = CFMLDapRemoteConfigurationType()
        val factory = configType.configurationFactories[0]
        val configuration = CFMLDapRemoteConfiguration(project, factory, "Test DAP")
        configuration.host = "127.0.0.1"
        configuration.port = port
        configuration.dapSecret = "secret"

        val runner = CFMLDapDebuggerRunner()
        val debugExecutor = DefaultDebugExecutor.getDebugExecutorInstance()
        val environment = ExecutionEnvironmentBuilder.create(project, debugExecutor, configuration).build()

        val state = CFMLDapRemoteRunProfileState(configuration)
        val descriptor = runner.doExecute(state, environment)

        assertNotNull(descriptor)
        val sessions = XDebuggerManager.getInstance(project).debugSessions
        assertTrue(sessions.isNotEmpty())
        val session = sessions.firstOrNull { it.runContentDescriptor == descriptor } ?: sessions[0]
        assertNotNull(session)
        assertNotNull(session.debugProcess)

        // Stop session and cleanup
        session.stop()
        descriptor?.let { desc ->
            desc.executionConsole?.let { com.intellij.openapi.util.Disposer.dispose(it) }
            com.intellij.openapi.util.Disposer.dispose(desc)
        }
        com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        executorService.shutdown()
    }

    @Test
    fun testDebugProcessCreateConsole() {
        val serverSocket = ServerSocket(0)
        val port = serverSocket.localPort

        val executorService = Executors.newSingleThreadExecutor()
        executorService.submit {
            try {
                serverSocket.use { server ->
                    server.soTimeout = 5000
                    val client = server.accept()
                    client.soTimeout = 5000
                    val initResp = """{"seq":1,"type":"response","request_seq":1,"command":"initialize","success":true,"body":{}}"""
                    client.getOutputStream().write("Content-Length: ${initResp.length}\r\n\r\n$initResp".toByteArray(Charsets.UTF_8))
                    client.getOutputStream().flush()
                }
            } catch (_: Exception) {
            }
        }

        val configType = CFMLDapRemoteConfigurationType()
        val factory = configType.configurationFactories[0]
        val configuration = CFMLDapRemoteConfiguration(project, factory, "Test DAP")
        configuration.host = "127.0.0.1"
        configuration.port = port

        val runner = CFMLDapDebuggerRunner()
        val debugExecutor = DefaultDebugExecutor.getDebugExecutorInstance()
        val environment = ExecutionEnvironmentBuilder.create(project, debugExecutor, configuration).build()
        val state = CFMLDapRemoteRunProfileState(configuration)

        val descriptor = runner.doExecute(state, environment)
        val sessions = XDebuggerManager.getInstance(project).debugSessions
        assertTrue(sessions.isNotEmpty())
        val session = sessions.firstOrNull { it.runContentDescriptor == descriptor } ?: sessions[0]
        assertNotNull(session)

        val debugProcess = session.debugProcess as? CFMLDapDebugProcess
        assertNotNull(debugProcess)

        val console = debugProcess?.createConsole()
        assertNotNull(console)

        session.stop()
        console?.let { com.intellij.openapi.util.Disposer.dispose(it) }
        descriptor?.let { desc ->
            desc.executionConsole?.let { com.intellij.openapi.util.Disposer.dispose(it) }
            com.intellij.openapi.util.Disposer.dispose(desc)
        }
        com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        executorService.shutdown()
    }
}
