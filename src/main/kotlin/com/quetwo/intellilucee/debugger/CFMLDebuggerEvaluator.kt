package com.quetwo.intellilucee.debugger

import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator

class CFMLDebuggerEvaluator(
    private val dapClient: CFMLDapClient,
    private val frameId: Long?
) : XDebuggerEvaluator() {

    override fun evaluate(
        expression: String,
        callback: XEvaluationCallback,
        expressionPosition: XSourcePosition?
    ) {
        dapClient.evaluate(expression, frameId, "watch")
            .thenAccept { evalResult ->
                val variable = DapVariable(
                    name = expression,
                    value = evalResult.result,
                    type = evalResult.type,
                    variablesReference = evalResult.variablesReference,
                    namedVariables = evalResult.namedVariables,
                    indexedVariables = evalResult.indexedVariables
                )
                val value = CFMLVariableValue(dapClient, variable)
                callback.evaluated(value)
            }
            .exceptionally { ex ->
                callback.errorOccurred(ex.localizedMessage ?: "Evaluation failed")
                null
            }
    }

    override fun evaluate(
        expression: XExpression,
        callback: XEvaluationCallback,
        expressionPosition: XSourcePosition?
    ) {
        evaluate(expression.expression, callback, expressionPosition)
    }
}
