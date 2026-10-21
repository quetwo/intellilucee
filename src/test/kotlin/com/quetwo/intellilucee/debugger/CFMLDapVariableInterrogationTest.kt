package com.quetwo.intellilucee.debugger

import com.intellij.lang.Language
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.frame.XValueModifier
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import com.quetwo.intellilucee.CFMLLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.Icon

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
@RunWith(JUnit4::class)
class CFMLDapVariableInterrogationTest {

    private fun sendDapFrame(output: PipedOutputStream, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val header = "Content-Length: ${bytes.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
        output.write(header)
        output.write(bytes)
        output.flush()
    }

    @Test
    fun testVariableValuePresentationAndExpansion() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        // 1. Primitive variable
        val primVar = DapVariable("age", "30", "numeric", 0L)
        val primValue = CFMLVariableValue(client, primVar)

        var presentedType: String? = null
        var presentedValue: String? = null
        var hasChildren: Boolean? = null

        val mockNode = object : XValueNode {
            override fun setPresentation(icon: Icon?, type: String?, value: String, hasChildrenFlag: Boolean) {
                presentedType = type
                presentedValue = value
                hasChildren = hasChildrenFlag
            }
            override fun setPresentation(icon: Icon?, presentation: com.intellij.xdebugger.frame.presentation.XValuePresentation, hasChildrenFlag: Boolean) {}
            override fun setFullValueEvaluator(fullValueEvaluator: com.intellij.xdebugger.frame.XFullValueEvaluator) {}
            override fun isObsolete(): Boolean = false
        }

        primValue.computePresentation(mockNode, XValuePlace.TREE)
        assertEquals("numeric", presentedType)
        assertEquals("30", presentedValue)
        assertEquals(false, hasChildren)

        // 2. Struct variable (expandable)
        val structVar = DapVariable("user", "Struct (2 keys)", "struct", 5001L)
        val structValue = CFMLVariableValue(client, structVar)
        structValue.computePresentation(mockNode, XValuePlace.TREE)
        assertEquals("struct", presentedType)
        assertEquals("Struct (2 keys)", presentedValue)
        assertEquals(true, hasChildren)

        // Test expanding struct children
        val childrenLatch = CountDownLatch(1)
        var addedChildrenCount = 0
        var firstChildName = ""
        val mockCompositeNode = object : XCompositeNode {
            override fun addChildren(children: XValueChildrenList, last: Boolean) {
                addedChildrenCount = children.size()
                if (children.size() > 0) {
                    firstChildName = children.getName(0)
                }
                childrenLatch.countDown()
            }
            override fun tooManyChildren(remaining: Int) {}
            override fun setAlreadySorted(alreadySorted: Boolean) {}
            override fun setErrorMessage(errorMessage: String) {}
            override fun setErrorMessage(errorMessage: String, link: com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink?) {}
            override fun setMessage(message: String, icon: Icon?, attributes: com.intellij.ui.SimpleTextAttributes, link: com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink?) {}
            override fun isObsolete(): Boolean = false
        }

        structValue.computeChildren(mockCompositeNode)

        sendDapFrame(serverOut, """{"seq":1,"type":"response","request_seq":1,"command":"variables","success":true,"body":{"variables":[{"name":"firstName","value":"John","type":"string","variablesReference":0},{"name":"lastName","value":"Doe","type":"string","variablesReference":0}]}}""")

        assertTrue(childrenLatch.await(5, TimeUnit.SECONDS))
        assertEquals(2, addedChildrenCount)
        assertEquals("firstName", firstChildName)

        client.close()
    }

    @Test
    fun testScopeValueExpansion() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        val scope = DapScope("Local", 1001L, false)
        val scopeValue = CFMLScopeValue(client, scope)

        val childrenLatch = CountDownLatch(1)
        var addedChildrenCount = 0
        val mockCompositeNode = object : XCompositeNode {
            override fun addChildren(children: XValueChildrenList, last: Boolean) {
                addedChildrenCount = children.size()
                childrenLatch.countDown()
            }
            override fun tooManyChildren(remaining: Int) {}
            override fun setAlreadySorted(alreadySorted: Boolean) {}
            override fun setErrorMessage(errorMessage: String) {}
            override fun setErrorMessage(errorMessage: String, link: com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink?) {}
            override fun setMessage(message: String, icon: Icon?, attributes: com.intellij.ui.SimpleTextAttributes, link: com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink?) {}
            override fun isObsolete(): Boolean = false
        }

        scopeValue.computeChildren(mockCompositeNode)
        sendDapFrame(serverOut, """{"seq":1,"type":"response","request_seq":1,"command":"variables","success":true,"body":{"variables":[{"name":"x","value":"1","type":"numeric","variablesReference":0}]}}""")

        assertTrue(childrenLatch.await(5, TimeUnit.SECONDS))
        assertEquals(1, addedChildrenCount)

        client.close()
    }

    @Test
    fun testVariableModifierSetValue() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        val variable = DapVariable("counter", "1", "numeric", 0L)
        val varValue = CFMLVariableValue(client, variable, parentVariablesReference = 1001L)

        val modifier = varValue.modifier
        assertNotNull(modifier)

        val modifiedLatch = CountDownLatch(1)
        val mockCallback = object : XValueModifier.XModificationCallback {
            override fun valueModified() {
                modifiedLatch.countDown()
            }
            override fun errorOccurred(errorMessage: String) {}
        }

        val expr = object : XExpression {
            override fun getExpression(): String = "100"
            override fun getLanguage(): Language = CFMLLanguage.INSTANCE
            override fun getCustomInfo(): String? = null
            override fun getMode(): EvaluationMode = EvaluationMode.EXPRESSION
        }
        modifier!!.setValue(expr, mockCallback)

        sendDapFrame(serverOut, """{"seq":1,"type":"response","request_seq":1,"command":"setVariable","success":true,"body":{"name":"counter","value":"100","type":"numeric"}}""")

        assertTrue(modifiedLatch.await(5, TimeUnit.SECONDS))

        client.close()
    }

    @Test
    fun testDebuggerEvaluator() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        val evaluator = CFMLDebuggerEvaluator(client, frameId = 100L)

        val evalLatch = CountDownLatch(1)
        var resultName = ""
        val mockCallback = object : XDebuggerEvaluator.XEvaluationCallback {
            override fun evaluated(result: com.intellij.xdebugger.frame.XValue) {
                if (result is CFMLVariableValue) {
                    resultName = result.variable.name
                }
                evalLatch.countDown()
            }
            override fun errorOccurred(errorMessage: String) {}
        }

        evaluator.evaluate("getSetting('app.name')", mockCallback, null)
        sendDapFrame(serverOut, """{"seq":1,"type":"response","request_seq":1,"command":"evaluate","success":true,"body":{"result":"IntelliLuceeApp","type":"string","variablesReference":0}}""")

        assertTrue(evalLatch.await(5, TimeUnit.SECONDS))
        assertEquals("getSetting('app.name')", resultName)

        client.close()
    }
}
