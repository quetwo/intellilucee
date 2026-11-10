package com.quetwo.intellilucee.docs

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.datatransfer.DataFlavor
import javax.swing.JMenuItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class CFMLDocumentationPanelPlatformTest : BasePlatformTestCase() {
    fun testPopupSelectsTheRequestedEntryAndCopiesBothFormats() {
        val panel = CFMLDocumentationPanel(project)
        val index = (0 until panel.entryList.model.size)
            .map { panel.entryList.model.getElementAt(it) }
            .indexOfFirst { it.name == "arrayAppend" }
        assertTrue(index >= 0)

        val popup = panel.createResultPopup(index)
        assertNotNull(popup)
        assertEquals("arrayAppend", panel.entryList.selectedValue.name)
        assertFalse((popup!!.getComponent(0) as JMenuItem).isEnabled)
        assertNull(panel.createResultPopup(-1))
        assertEquals(
            listOf("Insert Function/Tag", "Copy Function/Tag Name", "Copy Function/Tag with Attributes"),
            (0 until popup.componentCount).map { (popup.getComponent(it) as JMenuItem).text }
        )

        (popup.getComponent(1) as JMenuItem).doClick()
        assertEquals("arrayAppend", clipboardText())
        (popup.getComponent(2) as JMenuItem).doClick()
        assertEquals("arrayAppend(array, value, merge)", clipboardText())
    }

    fun testInsertionReplacesSelectionAndCanBeUndone() {
        myFixture.configureByText("example.txt", "before<selection>old</selection>after")
        val panel = CFMLDocumentationPanel(project)
        val index = (0 until panel.entryList.model.size)
            .map { panel.entryList.model.getElementAt(it) }
            .indexOfFirst { it.name == "arrayAppend" }
        val popup = panel.createResultPopup(index) ?: error("Documentation context menu is missing")

        assertTrue((popup.getComponent(0) as JMenuItem).isEnabled)
        (popup.getComponent(0) as JMenuItem).doClick()
        assertEquals("beforearrayAppend(array, value, merge)after", myFixture.editor.document.text)
        myFixture.performEditorAction(IdeActions.ACTION_UNDO)
        assertEquals("beforeoldafter", myFixture.editor.document.text)
    }

    fun testInsertionAtCaretAndReadOnlyEditorAvailability() {
        myFixture.configureByText("example.txt", "before<caret>after")
        val panel = CFMLDocumentationPanel(project)
        val editor = myFixture.editor

        assertTrue(panel.canInsertInSelectedEditor())
        assertTrue(panel.insertSnippet(editor, "cfoutput"))
        assertEquals("beforecfoutputafter", editor.document.text)

        editor.document.setReadOnly(true)
        assertFalse(panel.canInsertInSelectedEditor())
        val index = (0 until panel.entryList.model.size)
            .map { panel.entryList.model.getElementAt(it) }
            .indexOfFirst { it.name == "arrayAppend" }
        val popup = panel.createResultPopup(index) ?: error("Documentation context menu is missing")
        assertFalse((popup.getComponent(0) as JMenuItem).isEnabled)
        assertFalse(panel.insertSnippet(editor, "ignored"))
        assertEquals("beforecfoutputafter", editor.document.text)
    }

    private fun clipboardText(): String {
        val contents = CopyPasteManager.getInstance().contents ?: error("Clipboard is empty")
        return contents.getTransferData(DataFlavor.stringFlavor) as String
    }
}