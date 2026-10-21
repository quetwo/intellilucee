package com.quetwo.intellilucee.debugger

import com.intellij.icons.AllIcons
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.frame.XValueModifier
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import javax.swing.Icon

class CFMLVariableValue(
    private val dapClient: CFMLDapClient,
    val variable: DapVariable,
    private val parentVariablesReference: Long = 0L
) : XNamedValue(variable.name) {

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        val hasChildren = variable.variablesReference > 0
        val type = variable.type
        val value = variable.value
        val icon: Icon = when (type?.lowercase()) {
            "struct", "object", "component" -> AllIcons.Nodes.Class
            "array", "list" -> AllIcons.Debugger.Db_array
            "query" -> AllIcons.Nodes.DataTables
            "function", "closure" -> AllIcons.Nodes.Function
            else -> AllIcons.Nodes.Variable
        }
        node.setPresentation(icon, type, value, hasChildren)
    }

    override fun computeChildren(node: XCompositeNode) {
        if (variable.variablesReference <= 0) {
            node.addChildren(XValueChildrenList.EMPTY, true)
            return
        }

        dapClient.variables(variable.variablesReference).thenAccept { childVars ->
            val list = XValueChildrenList()
            for (childVar in childVars) {
                list.add(CFMLVariableValue(dapClient, childVar, variable.variablesReference))
            }
            node.addChildren(list, true)
        }.exceptionally { ex ->
            node.setErrorMessage(ex.localizedMessage ?: "Failed to retrieve variables")
            null
        }
    }

    override fun getModifier(): XValueModifier? {
        if (parentVariablesReference <= 0) return null
        return object : XValueModifier() {
            override fun setValue(expression: XExpression, callback: XModificationCallback) {
                dapClient.setVariable(parentVariablesReference, variable.name, expression.expression)
                    .thenAccept {
                        callback.valueModified()
                    }
                    .exceptionally { ex ->
                        callback.errorOccurred(ex.localizedMessage ?: "Failed to set variable value")
                        null
                    }
            }
        }
    }
}
