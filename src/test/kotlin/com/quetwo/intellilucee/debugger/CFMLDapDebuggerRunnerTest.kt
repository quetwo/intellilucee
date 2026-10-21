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
}
