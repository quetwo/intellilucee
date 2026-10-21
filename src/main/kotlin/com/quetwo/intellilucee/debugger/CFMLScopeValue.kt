package com.quetwo.intellilucee.debugger

import com.intellij.icons.AllIcons
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace

class CFMLScopeValue(
    private val dapClient: CFMLDapClient,
    val scope: DapScope
) : XNamedValue("[${scope.name}]") {

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(
            AllIcons.Nodes.Tag,
            "Scope",
            "${scope.name} scope",
            scope.variablesReference > 0
        )
    }

    override fun computeChildren(node: XCompositeNode) {
        if (scope.variablesReference <= 0) {
            node.addChildren(XValueChildrenList.EMPTY, true)
            return
        }

        dapClient.variables(scope.variablesReference).thenAccept { childVars ->
            val list = XValueChildrenList()
            for (childVar in childVars) {
                list.add(CFMLVariableValue(dapClient, childVar, scope.variablesReference))
            }
            node.addChildren(list, true)
        }.exceptionally { ex ->
            node.setErrorMessage(ex.localizedMessage ?: "Failed to retrieve scope variables")
            null
        }
    }
}
