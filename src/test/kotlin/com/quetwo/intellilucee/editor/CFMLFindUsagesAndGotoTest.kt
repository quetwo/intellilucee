package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import com.quetwo.intellilucee.psi.CFMLFunctionElement
import com.quetwo.intellilucee.psi.CFMLPsiUtil
import com.quetwo.intellilucee.psi.CFMLVariableElement
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLFindUsagesAndGotoTest : BasePlatformTestCase() {

    @Test
    fun testGotoFunctionDeclarationScript() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                public void function calculateTotal(numeric a, numeric b) {
                    return a + b;
                }
                
                public void function main() {
                    calculate<caret>Total(1, 2);
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull("Targets should not be null", targets)
        assertEquals(1, targets!!.size)
        assertTrue("Target should be CFMLFunctionElement", targets[0] is CFMLFunctionElement)
        val func = targets[0] as CFMLFunctionElement
        assertEquals("calculateTotal", func.name)
    }

    @Test
    fun testGotoFunctionDeclarationTag() {
        val file = myFixture.configureByText(
            "test.cfm",
            """
            <cffunction name="getUserName" access="public" returntype="string">
                <cfargument name="userId" type="numeric">
                <cfreturn "User" & arguments.userId>
            </cffunction>
            
            <cfset name = get<caret>UserName(123)>
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        assertTrue(targets[0] is CFMLFunctionElement)
        assertEquals("getUserName", (targets[0] as CFMLFunctionElement).name)
    }

    @Test
    fun testGotoFunctionDeclarationCaseInsensitive() {
        val file = myFixture.configureByText(
            "test.cfs",
            """
            function doWork() {
                return true;
            }
            
            DOW<caret>ORK();
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        assertEquals("doWork", (targets[0] as CFMLFunctionElement).name)
    }

    @Test
    fun testGotoVariableDeclarationScriptVar() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function run() {
                    var message = "hello";
                    writeOutput(mess<caret>age);
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull("Targets should not be null", targets)
        assertEquals(1, targets!!.size)
        assertTrue("Target should be CFMLVariableElement", targets[0] is CFMLVariableElement)
        val v = targets[0] as CFMLVariableElement
        assertEquals("message", v.name)
    }

    @Test
    fun testGotoVariableDeclarationFunctionArgument() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function process(required string userCode, numeric count) {
                    writeOutput(user<caret>Code);
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        assertEquals("userCode", (targets[0] as CFMLVariableElement).name)
    }

    @Test
    fun testGotoVariableDeclarationTagCfset() {
        val file = myFixture.configureByText(
            "test.cfm",
            """
            <cfset greeting = "Welcome!">
            <cfoutput>#gree<caret>ting#</cfoutput>
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        assertEquals("greeting", (targets[0] as CFMLVariableElement).name)
    }

    @Test
    fun testGotoVariableDeclarationTagCfparam() {
        val file = myFixture.configureByText(
            "test.cfm",
            """
            <cfparam name="pageTitle" default="Home">
            <h1>#page<caret>Title#</h1>
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        assertEquals("pageTitle", (targets[0] as CFMLVariableElement).name)
    }

    @Test
    fun testGotoVariableDeclarationCaseInsensitive() {
        val file = myFixture.configureByText(
            "test.cfm",
            """
            <cfset myNumber = 42>
            <cfset result = MYNUM<caret>BER + 1>
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        assertEquals("myNumber", (targets[0] as CFMLVariableElement).name)
    }

    @Test
    fun testFindUsagesFunction() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                public void function foo() {
                }
                
                function bar() {
                    foo();
                    foo();
                }
            }
            """.trimIndent()
        )

        val model = CFMLPsiUtil.getModel(file)
        val func = model.findFunctionDeclaration("foo")
        assertNotNull(func)

        val funcElement = CFMLPsiUtil.getFunctionElement(file, func!!)
        val handler = CFMLFindUsagesHandlerFactory().createFindUsagesHandler(funcElement, false)
        assertNotNull(handler)

        val usages = mutableListOf<UsageInfo>()
        handler!!.processElementUsages(
            funcElement,
            Processor { usages.add(it); true },
            FindUsagesOptions(project)
        )

        assertEquals("Should find 2 usages of foo", 2, usages.size)
    }

    @Test
    fun testFindUsagesVariable() {
        val file = myFixture.configureByText(
            "test.cfm",
            """
            <cfset count = 0>
            <cfset count = count + 1>
            <cfoutput>#count#</cfoutput>
            """.trimIndent()
        )

        val model = CFMLPsiUtil.getModel(file)
        val varDecl = model.findVariableDeclaration("count", 0)
        assertNotNull(varDecl)

        val varElement = CFMLPsiUtil.getVariableElement(file, varDecl!!)
        val handler = CFMLFindUsagesHandlerFactory().createFindUsagesHandler(varElement, false)
        assertNotNull(handler)

        val usages = mutableListOf<UsageInfo>()
        handler!!.processElementUsages(
            varElement,
            Processor { usages.add(it); true },
            FindUsagesOptions(project)
        )

        // Usages: count + 1, #count#
        assertTrue("Should find at least 2 usages of count", usages.size >= 2)
    }

    @Test
    fun testFunctionUsageCountLineMarker() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function helper() {
                    return 1;
                }
                
                function run() {
                    helper();
                    helper();
                    helper();
                }
            }
            """.trimIndent()
        )

        val model = CFMLPsiUtil.getModel(file)
        val helperFunc = model.findFunctionDeclaration("helper")
        assertNotNull(helperFunc)

        val count = model.getFunctionUsageCount(helperFunc!!)
        assertEquals(3, count)

        val lineMarkerProvider = CFMLFunctionUsageLineMarkerProvider()
        val markers = mutableListOf<com.intellij.codeInsight.daemon.LineMarkerInfo<*>>()
        lineMarkerProvider.collectSlowLineMarkers(listOf(file), markers)

        val helperMarker = markers.firstOrNull { it.lineMarkerTooltip?.contains("3 uses") == true }
        assertNotNull("Should find line marker with 3 uses tooltip", helperMarker)
    }

    @Test
    fun testFunctionUsageCountSingleUse() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function doOnce() {
                }
                
                function run() {
                    doOnce();
                }
            }
            """.trimIndent()
        )

        val model = CFMLPsiUtil.getModel(file)
        val func = model.findFunctionDeclaration("doOnce")
        assertNotNull(func)
        assertEquals(1, model.getFunctionUsageCount(func!!))

        val lineMarkerProvider = CFMLFunctionUsageLineMarkerProvider()
        val markers = mutableListOf<com.intellij.codeInsight.daemon.LineMarkerInfo<*>>()
        lineMarkerProvider.collectSlowLineMarkers(listOf(file), markers)

        val marker = markers.firstOrNull { it.lineMarkerTooltip?.contains("1 use") == true }
        assertNotNull("Should find line marker with 1 use tooltip", marker)
    }

    @Test
    fun testNoDuplicateLineMarkersForElementsChunks() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function foo() {
                    return 1;
                }
                
                function bar() {
                    return foo();
                }
            }
            """.trimIndent()
        )

        val lineMarkerProvider = CFMLFunctionUsageLineMarkerProvider()
        val allMarkers = mutableListOf<com.intellij.codeInsight.daemon.LineMarkerInfo<*>>()

        // Simulate daemon passing elements one by one or in batches
        val text = file.text
        for (i in 0 until text.length) {
            val element = file.findElementAt(i) ?: continue
            val chunkMarkers = mutableListOf<com.intellij.codeInsight.daemon.LineMarkerInfo<*>>()
            lineMarkerProvider.collectSlowLineMarkers(listOf(element), chunkMarkers)
            for (m in chunkMarkers) {
                // Ensure no duplicate marker is added for the same element
                if (!allMarkers.any { it.element == m.element }) {
                    allMarkers.add(m)
                }
            }
        }

        // There should be exactly 2 markers: one for foo, one for bar
        val model = CFMLPsiUtil.getModel(file)
        assertEquals(2, model.functions.size)
        assertEquals(2, allMarkers.size)
    }

    @Test
    fun testNoLineMarkerForNonFunctionElement() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                var x = 10;
                function foo() {
                    return x;
                }
            }
            """.trimIndent()
        )

        val lineMarkerProvider = CFMLFunctionUsageLineMarkerProvider()
        val varElement = file.findElementAt(file.text.indexOf("x = 10"))!!
        val markers = mutableListOf<com.intellij.codeInsight.daemon.LineMarkerInfo<*>>()
        lineMarkerProvider.collectSlowLineMarkers(listOf(varElement), markers)

        // No line markers should be added for a variable element
        assertTrue("No line markers should be added for non-function element", markers.isEmpty())
    }

    @Test
    fun testNoLineMarkerForAnonymousFunction() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function foo() {
                    var cb = function() { return 42; };
                    return cb();
                }
            }
            """.trimIndent()
        )

        val lineMarkerProvider = CFMLFunctionUsageLineMarkerProvider()
        val markers = mutableListOf<com.intellij.codeInsight.daemon.LineMarkerInfo<*>>()
        lineMarkerProvider.collectSlowLineMarkers(listOf(file), markers)

        // Only foo should have a marker, anonymous function shouldn't
        assertEquals(1, markers.size)
        assertEquals("foo", (CFMLPsiUtil.resolveSymbolAt(file, markers[0].element!!.textRange.startOffset) as? CFMLFunctionElement)?.name)
    }

    @Test
    fun testGotoFunctionFromCfinvoke() {
        val file = myFixture.configureByText(
            "test.cfm",
            """
            <cffunction name="executeProcess" access="public">
            </cffunction>
            
            <cfinvoke method="execute<caret>Process">
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        assertEquals("executeProcess", (targets[0] as CFMLFunctionElement).name)
    }

    @Test
    fun testGotoVariableFromLoop() {
        val file = myFixture.configureByText(
            "test.cfm",
            """
            <cfloop index="itemIdx" from="1" to="10">
                <cfoutput>#item<caret>Idx#</cfoutput>
            </cfloop>
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        assertEquals("itemIdx", (targets[0] as CFMLVariableElement).name)
    }

    @Test
    fun testGotoVariableTypedDeclaration() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function test() {
                    numeric totalAmount = 100;
                    return total<caret>Amount + 10;
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        assertEquals("totalAmount", (targets[0] as CFMLVariableElement).name)
    }

    @Test
    fun testInlayHintsProviderCollector() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function myHandler() {
                }
                
                function run() {
                    myHandler();
                    myHandler();
                }
            }
            """.trimIndent()
        )

        val provider = CFMLFunctionUsageInlayHintsProvider()
        val collector = provider.createCollector(file, myFixture.editor)
        assertNotNull("Collector should not be null", collector)
        assertTrue(collector is com.intellij.codeInsight.hints.declarative.SharedBypassCollector)

        var hintCount = 0
        val mockSink = object : com.intellij.codeInsight.hints.declarative.InlayTreeSink {
            override fun addPresentation(
                position: com.intellij.codeInsight.hints.declarative.InlayPosition,
                payloads: List<com.intellij.codeInsight.hints.declarative.InlayPayload>?,
                tooltip: String?,
                hintFormat: com.intellij.codeInsight.hints.declarative.HintFormat,
                builder: com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder.() -> Unit
            ) {
                hintCount++
            }
            override fun whenOptionEnabled(optionId: String, block: () -> Unit) {
                block()
            }
        }

        val bypassCollector = collector as com.intellij.codeInsight.hints.declarative.SharedBypassCollector
        // Calling for file
        bypassCollector.collectFromElement(file, mockSink)
        assertEquals("Should emit exactly 2 hints (one per function) when collecting for file", 2, hintCount)

        // Calling for a child element should not emit additional hints
        val child = file.firstChild ?: file
        bypassCollector.collectFromElement(child, mockSink)
        assertEquals("Should NOT emit hints when called on child element", 2, hintCount)
    }

    @Test
    fun testGotoTargetUnderlinesOnlyIdentifierNotEntireFile() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function runTest() {
                    var myCount = 10;
                    writeOutput(my<caret>Count);
                }
            }
            """.trimIndent()
        )

        val element = file.findElementAt(myFixture.caretOffset)
        assertNotNull("Leaf element at caret should not be null", element)
        assertEquals("Leaf element text should be only the variable name", "myCount", element!!.text)
        assertTrue("Leaf element should NOT cover entire file", file.textLength != element.textRange.length)

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(element, myFixture.caretOffset, myFixture.editor)
        assertNotNull("Should resolve variable declaration", targets)
        assertEquals(1, targets!!.size)
        assertEquals("myCount", (targets[0] as CFMLVariableElement).name)
    }

    @Test
    fun testGotoDeclarationReturnsNullOnKeywordOrWhitespace() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            func<caret>tion testFunc() {
                var x = 1;
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val element = file.findElementAt(myFixture.caretOffset)
        assertNotNull(element)
        val targets = gotoHandler.getGotoDeclarationTargets(element, myFixture.caretOffset, myFixture.editor)
        assertNull("Goto targets should be null on keyword 'function'", targets)
    }

    @Test
    fun testCFMLLexerTokenization() {
        val lexer = com.quetwo.intellilucee.parser.CFMLLexer()
        val code = "var total = calc(10, \"str\"); // comment"
        lexer.start(code, 0, code.length, 0)

        val tokens = mutableListOf<Pair<String, com.intellij.psi.tree.IElementType?>>()
        while (lexer.tokenType != null) {
            val tokenText = code.substring(lexer.tokenStart, lexer.tokenEnd)
            tokens.add(Pair(tokenText, lexer.tokenType))
            lexer.advance()
        }

        assertEquals("var", tokens[0].first)
        assertEquals(com.quetwo.intellilucee.parser.CFMLTokenTypes.IDENTIFIER, tokens[0].second)

        assertEquals(" ", tokens[1].first)
        assertEquals(com.intellij.psi.TokenType.WHITE_SPACE, tokens[1].second)

        assertEquals("total", tokens[2].first)
        assertEquals(com.quetwo.intellilucee.parser.CFMLTokenTypes.IDENTIFIER, tokens[2].second)

        assertEquals(" ", tokens[3].first)
        assertEquals("=", tokens[4].first)
        assertEquals(" ", tokens[5].first)

        assertEquals("calc", tokens[6].first)
        assertEquals(com.quetwo.intellilucee.parser.CFMLTokenTypes.IDENTIFIER, tokens[6].second)

        val stringToken = tokens.firstOrNull { it.first == "\"str\"" }
        assertNotNull("Should have string token", stringToken)
        assertEquals(com.quetwo.intellilucee.parser.CFMLTokenTypes.STRING, stringToken!!.second)

        val commentToken = tokens.firstOrNull { it.first == "// comment" }
        assertNotNull("Should have comment token", commentToken)
        assertEquals(com.quetwo.intellilucee.parser.CFMLTokenTypes.COMMENT, commentToken!!.second)
    }

    @Test
    fun testGotoLocalVariableWithinSameFunctionScript() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function compute() {
                    var discount = 0.15;
                    var total = 100 * (1 - disc<caret>ount);
                    return total;
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull("Targets should not be null", targets)
        assertEquals(1, targets!!.size)
        assertTrue("Target should be CFMLVariableElement", targets[0] is CFMLVariableElement)
        val varElem = targets[0] as CFMLVariableElement
        assertEquals("discount", varElem.name)
        assertTrue("Should be local variable", varElem.variableDecl.isLocal)
        assertEquals("compute", varElem.variableDecl.enclosingFunction?.name)
    }

    @Test
    fun testGotoLocalVariableMultiVarScript() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function calculate() {
                    var a = "first, item", b = 20, c;
                    return <caret>b + 5;
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        val varElem = targets[0] as CFMLVariableElement
        assertEquals("b", varElem.name)
        assertTrue(varElem.variableDecl.isLocal)
        assertEquals("calculate", varElem.variableDecl.enclosingFunction?.name)
    }

    @Test
    fun testGotoLocalVariableScopedLocal() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function run() {
                    local.status = "ACTIVE";
                    writeOutput(loc<caret>al.status);
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        val varElem = targets[0] as CFMLVariableElement
        assertEquals("status", varElem.name)
        assertTrue(varElem.variableDecl.isLocal)
        assertEquals("run", varElem.variableDecl.enclosingFunction?.name)
    }

    @Test
    fun testGotoFunctionArgumentFromArgumentsScope() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function greet(required string name, numeric age = 30) {
                    writeOutput("Hello " & arguments.na<caret>me);
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        val varElem = targets[0] as CFMLVariableElement
        assertEquals("name", varElem.name)
        assertTrue(varElem.variableDecl.isLocal)
        assertEquals("greet", varElem.variableDecl.enclosingFunction?.name)
    }

    @Test
    fun testGotoLocalVariableSeparateFunctionsSameName() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function funcA() {
                    var value = 100;
                    return value;
                }
                
                function funcB() {
                    var value = 200;
                    return val<caret>ue;
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        val varElem = targets[0] as CFMLVariableElement
        assertEquals("value", varElem.name)
        assertTrue(varElem.variableDecl.isLocal)
        assertEquals("funcB", varElem.variableDecl.enclosingFunction?.name)
    }

    @Test
    fun testGotoLocalVariableShadowsFileVariable() {
        val file = myFixture.configureByText(
            "test.cfm",
            """
            <cfset count = 999>
            
            <cffunction name="doCount">
                <cfset var count = 10>
                <cfreturn cou<caret>nt + 1>
            </cffunction>
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        val varElem = targets[0] as CFMLVariableElement
        assertEquals("count", varElem.name)
        assertTrue("Should resolve to local count in doCount function", varElem.variableDecl.isLocal)
        assertEquals("doCount", varElem.variableDecl.enclosingFunction?.name)
    }

    @Test
    fun testGotoLocalVariablePlainAssignmentInFunction() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function process() {
                    itemsProcessed = 0;
                    itemsProcessed = itemsProcessed + 1;
                    return itemsProce<caret>ssed;
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        val varElem = targets[0] as CFMLVariableElement
        assertEquals("itemsProcessed", varElem.name)
        assertTrue(varElem.variableDecl.isLocal)
        assertEquals("process", varElem.variableDecl.enclosingFunction?.name)
    }

    @Test
    fun testGotoLocalVariableForLoopInFunction() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function iterate(items) {
                    for (item in items) {
                        writeOutput(it<caret>em);
                    }
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        val varElem = targets[0] as CFMLVariableElement
        assertEquals("item", varElem.name)
        assertTrue(varElem.variableDecl.isLocal)
        assertEquals("iterate", varElem.variableDecl.enclosingFunction?.name)
    }

    @Test
    fun testGotoLocalVariableCatchBlockInFunction() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function safeRun() {
                    try {
                        doSomething();
                    } catch (any err) {
                        writeLog(e<caret>rr.message);
                    }
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        val varElem = targets[0] as CFMLVariableElement
        assertEquals("err", varElem.name)
        assertTrue(varElem.variableDecl.isLocal)
        assertEquals("safeRun", varElem.variableDecl.enclosingFunction?.name)
    }

    @Test
    fun testGotoLocalVariableTagCffunction() {
        val file = myFixture.configureByText(
            "test.cfm",
            """
            <cffunction name="generateOutput">
                <cfargument name="prefix" type="string">
                <cfset var result = prefix & "_output">
                <cfreturn resu<caret>lt>
            </cffunction>
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        val varElem = targets[0] as CFMLVariableElement
        assertEquals("result", varElem.name)
        assertTrue(varElem.variableDecl.isLocal)
        assertEquals("generateOutput", varElem.variableDecl.enclosingFunction?.name)
    }

    @Test
    fun testGotoLocalVariableClosure() {
        val file = myFixture.configureByText(
            "test.cfc",
            """
            component {
                function runClosure(list) {
                    var factor = 2;
                    return list.map(function(num) {
                        var doubled = num * factor;
                        return doub<caret>led;
                    });
                }
            }
            """.trimIndent()
        )

        val gotoHandler = CFMLGotoDeclarationHandler()
        val targets = gotoHandler.getGotoDeclarationTargets(
            file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        val varElem = targets[0] as CFMLVariableElement
        assertEquals("doubled", varElem.name)
        assertTrue(varElem.variableDecl.isLocal)
    }
}
