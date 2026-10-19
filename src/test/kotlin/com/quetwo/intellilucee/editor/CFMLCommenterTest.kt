package com.quetwo.intellilucee.editor

import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.quetwo.intellilucee.CFMLLanguage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLCommenterTest : BasePlatformTestCase() {

    @Test
    fun testCommenterRegisteredForLanguage() {
        val commenter = LanguageCommenters.INSTANCE.forLanguage(CFMLLanguage.INSTANCE)
        assertNotNull(commenter)
        assertTrue(commenter is CFMLCommenter)
    }

    @Test
    fun testLineCommentOutsideCfscriptInCfm() {
        myFixture.configureByText("test.cfm", "<caret><cfset x = 1>")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("<!--- <cfset x = 1> --->", myFixture.editor.document.text)

        // Toggle / uncomment line comment
        myFixture.editor.caretModel.moveToOffset(0)
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("<cfset x = 1>", myFixture.editor.document.text)
    }

    @Test
    fun testLineCommentOutsideCfscriptIndented() {
        myFixture.configureByText("test.cfm", "    <caret><cfset x = 1>")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("    <!--- <cfset x = 1> --->", myFixture.editor.document.text)

        // Toggle / uncomment line comment
        myFixture.editor.caretModel.moveToOffset(0)
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("    <cfset x = 1>", myFixture.editor.document.text)
    }

    @Test
    fun testBlockCommentOutsideCfscriptInCfm() {
        myFixture.configureByText("test.cfm", "<selection><cfset x = 1></selection>")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_BLOCK)
        assertEquals("<!--- <cfset x = 1> --->", myFixture.editor.document.text)

        // Toggle / uncomment block comment
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_BLOCK)
        assertEquals("<cfset x = 1>", myFixture.editor.document.text)
    }

    @Test
    fun testLineCommentInsideCfscriptInCfm() {
        val code = """
            <cfoutput>#name#</cfoutput>
            <cfscript>
                <caret>var x = 1;
                var y = 2;
            </cfscript>
        """.trimIndent()
        myFixture.configureByText("test.cfm", code)
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)

        val expected = """
            <cfoutput>#name#</cfoutput>
            <cfscript>
                //var x = 1;
                var y = 2;
            </cfscript>
        """.trimIndent()
        assertEquals(expected, myFixture.editor.document.text)

        // Move caret back to the commented line and toggle / uncomment
        myFixture.editor.caretModel.moveToOffset(myFixture.editor.document.text.indexOf("//var x = 1;"))
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(code.replace("<caret>", ""), myFixture.editor.document.text)
    }

    @Test
    fun testBlockCommentInsideCfscriptInCfm() {
        val code = """
            <cfoutput>#name#</cfoutput>
            <cfscript>
                <selection>var x = 1;</selection>
                var y = 2;
            </cfscript>
        """.trimIndent()
        myFixture.configureByText("test.cfm", code)
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_BLOCK)

        val expected = """
            <cfoutput>#name#</cfoutput>
            <cfscript>
                /* var x = 1; */
                var y = 2;
            </cfscript>
        """.trimIndent()
        assertEquals(expected, myFixture.editor.document.text)

        // Toggle / uncomment
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_BLOCK)
        assertEquals(code.replace("<selection>", "").replace("</selection>", ""), myFixture.editor.document.text)
    }

    @Test
    fun testMixedCommentingInCfmFile() {
        val code = """
            <cfset a = 1>
            <cfscript>
                var b = 2;
            </cfscript>
            <cfset c = 3>
        """.trimIndent()
        myFixture.configureByText("test.cfm", code)

        // Comment outside cfscript (line 1)
        myFixture.editor.caretModel.moveToOffset(code.indexOf("<cfset a = 1>"))
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)

        // Comment inside cfscript (line 3)
        myFixture.editor.caretModel.moveToOffset(myFixture.editor.document.text.indexOf("var b = 2;"))
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)

        // Comment outside cfscript (line 5)
        myFixture.editor.caretModel.moveToOffset(myFixture.editor.document.text.indexOf("<cfset c = 3>"))
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)

        val expected = """
            <!--- <cfset a = 1> --->
            <cfscript>
                //var b = 2;
            </cfscript>
            <!--- <cfset c = 3> --->
        """.trimIndent()
        assertEquals(expected, myFixture.editor.document.text)
    }

    @Test
    fun testScriptCfcUsesSlashSlashAndSlashStar() {
        val code = """
            component {
                <caret>function doWork() {
                }
            }
        """.trimIndent()
        myFixture.configureByText("MyService.cfc", code)

        // Line comment
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        val expected = """
            component {
                //function doWork() {
                }
            }
        """.trimIndent()
        assertEquals(expected, myFixture.editor.document.text)

        // Move caret back to the commented line and uncomment
        myFixture.editor.caretModel.moveToOffset(myFixture.editor.document.text.indexOf("//function doWork() {"))
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals(code.replace("<caret>", ""), myFixture.editor.document.text)
    }

    @Test
    fun testScriptFileCfsUsesSlashSlashAndSlashStar() {
        val code = "<caret>var x = 1;"
        myFixture.configureByText("test.cfs", code)

        // Line comment
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("//var x = 1;", myFixture.editor.document.text)

        // Move caret back to commented line and uncomment
        myFixture.editor.caretModel.moveToOffset(0)
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("var x = 1;", myFixture.editor.document.text)

        // Block comment
        myFixture.configureByText("test.cfs", "<selection>var x = 1;</selection>")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_BLOCK)
        assertEquals("/* var x = 1; */", myFixture.editor.document.text)

        // Uncomment
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_BLOCK)
        assertEquals("var x = 1;", myFixture.editor.document.text)
    }

    @Test
    fun testTagCfcUsesTagCommentOutsideCfscript() {
        val code = """
            <cfcomponent>
                <caret><cffunction name="doWork">
                </cffunction>
            </cfcomponent>
        """.trimIndent()
        myFixture.configureByText("MyService.cfc", code)

        // Line comment outside cfscript
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        val expected = """
            <cfcomponent>
                <!--- <cffunction name="doWork"> --->
                </cffunction>
            </cfcomponent>
        """.trimIndent()
        assertEquals(expected, myFixture.editor.document.text)
    }
}
