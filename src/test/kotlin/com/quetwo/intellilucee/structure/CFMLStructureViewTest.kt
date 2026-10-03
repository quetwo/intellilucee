package com.quetwo.intellilucee.structure

import com.intellij.lang.LanguageStructureViewBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.quetwo.intellilucee.psi.CFMLPsiFile
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLStructureViewTest : BasePlatformTestCase() {

    @Test
    fun testStructureViewRegistration() {
        val file = myFixture.configureByText("test.cfm", "<cfset x = 1>") as CFMLPsiFile
        val builder = LanguageStructureViewBuilder.getInstance().getStructureViewBuilder(file)
        assertNotNull("StructureViewBuilder should be registered for CFML", builder)
    }

    @Test
    fun testHtmlAndCFMLTagsHierarchy() {
        val code = """
            <!DOCTYPE html>
            <html>
                <head>
                    <title>My Page</title>
                </head>
                <body class="main-body">
                    <div id="content" class="container fluid">
                        <h1>Header</h1>
                        <cfquery name="getUsers" datasource="dsn">
                            SELECT * FROM users
                        </cfquery>
                        <cfloop query="getUsers">
                            <div class="user-row">
                                <cfoutput>#getUsers.name#</cfoutput>
                            </div>
                        </cfloop>
                        <input type="text" name="search" />
                    </div>
                </body>
            </html>
        """.trimIndent()

        val file = myFixture.configureByText("page.cfm", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        // Top level should have html
        assertTrue("Expected top-level HTML tags", children.isNotEmpty())
        val htmlElement = children.find { it.presentation.presentableText?.startsWith("html") == true }
        assertNotNull("html tag should be in root children", htmlElement)

        val htmlChildren = htmlElement!!.children
        val headElement = htmlChildren.find { it.presentation.presentableText?.startsWith("head") == true }
        val bodyElement = htmlChildren.find { it.presentation.presentableText?.startsWith("body") == true }
        assertNotNull("head tag should be under html", headElement)
        assertNotNull("body tag should be under html", bodyElement)
        assertEquals("body.main-body", bodyElement!!.presentation.presentableText)

        val bodyChildren = bodyElement.children
        val divContent = bodyChildren.find { it.presentation.presentableText?.startsWith("div#content") == true }
        assertNotNull("div#content should be under body", divContent)
        assertEquals("div#content.container", divContent!!.presentation.presentableText)

        val divChildren = divContent.children
        val h1Element = divChildren.find { it.presentation.presentableText == "h1" }
        val queryElement = divChildren.find { it.presentation.presentableText == "cfquery" }
        val loopElement = divChildren.find { it.presentation.presentableText == "cfloop" }
        val inputElement = divChildren.find { it.presentation.presentableText?.startsWith("input") == true }

        assertNotNull("h1 should be under div#content", h1Element)
        assertNotNull("cfquery should be under div#content", queryElement)
        assertEquals("getUsers", queryElement!!.presentation.locationString)

        assertNotNull("cfloop should be under div#content", loopElement)
        val loopChildren = loopElement!!.children
        val userRowDiv = loopChildren.find { it.presentation.presentableText?.startsWith("div.user-row") == true }
        assertNotNull("div.user-row should be under cfloop", userRowDiv)

        val userRowChildren = userRowDiv!!.children
        val outputElement = userRowChildren.find { it.presentation.presentableText == "cfoutput" }
        assertNotNull("cfoutput should be under div.user-row", outputElement)

        assertNotNull("input element should be under div#content", inputElement)
        assertEquals("name=\"search\"", inputElement!!.presentation.locationString)
    }

    @Test
    fun testCffunctionStructure() {
        val code = """
            <cfcomponent>
                <cffunction name="getUserById" access="public" returntype="struct">
                    <cfargument name="id" type="numeric" required="true">
                    <cfreturn { id: arguments.id, name: "Test" }>
                </cffunction>
                <cffunction name="privateHelper" access="private">
                    <cfset var x = 1>
                </cffunction>
            </cfcomponent>
        """.trimIndent()

        val file = myFixture.configureByText("User.cfc", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        val compElement = children.find { it.presentation.presentableText == "cfcomponent" }
        assertNotNull("cfcomponent should be present", compElement)

        val compChildren = compElement!!.children
        val getFunc = compChildren.find { it.presentation.presentableText?.contains("getUserById") == true }
        val privateFunc = compChildren.find { it.presentation.presentableText?.contains("privateHelper") == true }

        assertNotNull("getUserById should be under cfcomponent", getFunc)
        assertNotNull("privateHelper should be under cfcomponent", privateFunc)
        assertEquals(": struct", getFunc!!.presentation.locationString)
    }

    @Test
    fun testScriptFunctionsInStructureView() {
        val code = """
            <cfscript>
                function calculateTotal(numeric price, numeric tax) {
                    return price + tax;
                }
            </cfscript>
        """.trimIndent()

        val file = myFixture.configureByText("calculator.cfm", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        val cfscriptElement = children.find { it.presentation.presentableText == "cfscript" }
        assertNotNull("cfscript tag should be found", cfscriptElement)

        val scriptChildren = cfscriptElement!!.children
        val calcFunc = scriptChildren.find { it.presentation.presentableText?.startsWith("calculateTotal") == true }
        assertNotNull("calculateTotal function should be found under cfscript", calcFunc)
    }

    @Test
    fun testSelfClosingAndVoidTags() {
        val code = """
            <div>
                <img src="test.png" id="avatar" class="rounded" />
                <br>
                <hr>
                <p>Paragraph text</p>
            </div>
        """.trimIndent()

        val file = myFixture.configureByText("void.cfm", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        assertEquals(1, children.size)
        val div = children[0]
        assertEquals("div", div.presentation.presentableText)

        val divChildren = div.children
        val names = divChildren.map { it.presentation.presentableText }
        assertEquals("Expected [img#avatar.rounded, br, hr, p] but got $names", 4, divChildren.size)
        assertEquals("img#avatar.rounded", divChildren[0].presentation.presentableText)
        assertEquals("br", divChildren[1].presentation.presentableText)
        assertEquals("hr", divChildren[2].presentation.presentableText)
        assertEquals("p", divChildren[3].presentation.presentableText)
    }

    @Test
    fun testCustomTagsAndBooleanAttributes() {
        val code = """
            <form id="loginForm" method="post" novalidate>
                <input type="text" name="username" required disabled />
                <cf_customheader title="Login" />
                <button type="submit" class="btn btn-primary">Submit</button>
            </form>
        """.trimIndent()

        val file = myFixture.configureByText("form.cfm", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        assertEquals(1, children.size)
        val form = children[0]
        assertEquals("form#loginForm", form.presentation.presentableText)

        val formChildren = form.children
        assertEquals(3, formChildren.size)
        assertEquals("input", formChildren[0].presentation.presentableText)
        assertEquals("name=\"username\"", formChildren[0].presentation.locationString)
        assertEquals("cf_customheader", formChildren[1].presentation.presentableText)
        assertEquals("button.btn", formChildren[2].presentation.presentableText)
    }

    @Test
    fun testNavigation() {
        val code = """
            <html>
                <body id="main">
                    <cffunction name="testFn">
                    </cffunction>
                </body>
            </html>
        """.trimIndent()

        val file = myFixture.configureByText("nav.cfm", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val htmlTree = rootElement.children[0] as CFMLStructureViewElement
        val bodyTree = htmlTree.children[0] as CFMLStructureViewElement
        val fnTree = bodyTree.children[0] as CFMLStructureViewElement

        assertTrue(htmlTree.canNavigate())
        assertTrue(bodyTree.canNavigate())
        assertTrue(fnTree.canNavigate())
        assertTrue(htmlTree.canNavigateToSource())
    }
}
