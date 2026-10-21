package com.quetwo.intellilucee.debugger

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager

@Suppress("DEPRECATION")
class CFMLDapDebuggerRunner : GenericProgramRunner<RunnerSettings>() {

    override fun getRunnerId(): String = "CFMLDapDebuggerRunner"

    override fun canRun(executorId: String, profile: RunProfile): Boolean =
        (DefaultDebugExecutor.EXECUTOR_ID == executorId || DefaultRunExecutor.EXECUTOR_ID == executorId) &&
            profile is CFMLDapRemoteConfiguration

    public override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        val configuration = environment.runProfile as? CFMLDapRemoteConfiguration ?: return null
        val session = XDebuggerManager.getInstance(environment.project).startSessionAndShowTab(
            environment.runProfile.name,
            environment.runProfile.icon,
            environment.contentToReuse,
            false,
            object : XDebugProcessStarter() {
                override fun start(session: XDebugSession): XDebugProcess =
                    CFMLDapDebugProcess.create(session, configuration)
            }
        )
        return session.runContentDescriptor
    }
}
