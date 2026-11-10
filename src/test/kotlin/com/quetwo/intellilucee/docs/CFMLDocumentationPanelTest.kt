package com.quetwo.intellilucee.docs

import java.awt.Color
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JLabel
import javax.swing.SwingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CFMLDocumentationPanelTest {
    @Test
    fun themeUpdateRefreshesTheSelectedDocumentationAndKeepsListColors() {
        val panelReference = AtomicReference<CFMLDocumentationPanel>()
        SwingUtilities.invokeAndWait { panelReference.set(CFMLDocumentationPanel()) }
        val panel = panelReference.get()
        val selectedEntryReference = AtomicReference<CFMLDocumentationEntry>()
        val caretReference = AtomicReference<Int>()

        SwingUtilities.invokeAndWait {
            val list = panel.entryList
            val entry = list.model.getElementAt(0)
            selectedEntryReference.set(entry)
            val renderer = list.cellRenderer
            val unselectedLabel = renderer.getListCellRendererComponent(list, entry, 0, false, false) as JLabel
            val selectedLabel = renderer.getListCellRendererComponent(list, entry, 0, true, false) as JLabel

            assertEquals(list.foreground, unselectedLabel.foreground)
            assertEquals(list.selectionForeground, selectedLabel.foreground)
            assertTrue(unselectedLabel.text.contains("color:${unselectedLabel.foreground.toHtmlColor()}"))
            assertTrue(selectedLabel.text.contains("color:${selectedLabel.foreground.toHtmlColor()}"))
            assertFalse(unselectedLabel.text.contains("#777"))

            list.selectedIndex = 0
            panel.detailsPane.caretPosition = 40
            caretReference.set(panel.detailsPane.caretPosition)
            panel.detailsPane.foreground = Color(224, 224, 224)
            panel.detailsPane.background = Color(32, 32, 32)
            panel.updateUI()
        }

        SwingUtilities.invokeAndWait {
            assertSame(selectedEntryReference.get(), panel.entryList.selectedValue)
            assertTrue(panel.detailsPane.text.contains("#e0e0e0"))
            assertTrue(panel.detailsPane.text.contains("#202020"))
            assertEquals(caretReference.get(), panel.detailsPane.caretPosition)
            assertNotNull(panel.detailsPane.document)
        }
    }

    private fun Color.toHtmlColor(): String = "#%02x%02x%02x".format(red, green, blue)
}