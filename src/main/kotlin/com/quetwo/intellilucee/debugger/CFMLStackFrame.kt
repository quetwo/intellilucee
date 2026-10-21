package com.quetwo.intellilucee.debugger

import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValueChildrenList

class CFMLStackFrame(
    private val dapClient: CFMLDapClient,
    val frame: DapStackFrame,
    private val sourcePosition: XSourcePosition?
) : XStackFrame() {

    override fun getSourcePosition(): XSourcePosition? = sourcePosition

    override fun getEvaluator(): XDebuggerEvaluator = CFMLDebuggerEvaluator(dapClient, frame.id)

    override fun customizePresentation(component: ColoredTextContainer) {
        component.append(frame.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        val srcPath = frame.source?.path ?: frame.source?.name
        if (!srcPath.isNullOrBlank()) {
            val fileName = srcPath.substringAfterLast('/').substringAfterLast('\\')
            component.append(" ($fileName:${frame.line})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }

    override fun computeChildren(node: XCompositeNode) {
        dapClient.scopes(frame.id).thenAccept { scopes ->
            val list = XValueChildrenList()
            for (scope in scopes) {
                list.add(CFMLScopeValue(dapClient, scope))
            }

            val localScope = scopes.firstOrNull { it.name.equals("Local", ignoreCase = true) } ?: scopes.firstOrNull()
            if (localScope != null && localScope.variablesReference > 0) {
                dapClient.variables(localScope.variablesReference).thenAccept { localVars ->
                    for (localVar in localVars) {
                        list.add(CFMLVariableValue(dapClient, localVar, localScope.variablesReference))
                    }
                    node.addChildren(list, true)
                }.exceptionally {
                    node.addChildren(list, true)
                    null
                }
            } else {
                node.addChildren(list, true)
            }
        }.exceptionally { ex ->
            node.setErrorMessage(ex.localizedMessage ?: "Failed to retrieve stack frame scopes")
            null
        }
    }
}
