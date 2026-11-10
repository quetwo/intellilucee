package com.quetwo.intellilucee.docs

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.SwingUtilities
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.HyperlinkEvent

class CFMLDocumentationPanel(private val project: Project? = null) : JPanel(BorderLayout(8, 8)) {
    private val entries = CFMLDocumentationRepository.loadEntries()
    private var emptyMessage = "Select a function or tag to view its documentation."
    private var initialized = false
    private val searchField = JTextField()
    private val typeFilter = JComboBox(DocumentationTypeFilter.values())
    private val engineFilter = JComboBox(engineOptions.toTypedArray())
    private val versionField = JTextField()
    private val statusLabel = JLabel()
    internal val entryList = JList<CFMLDocumentationEntry>()
    internal val detailsPane = JEditorPane()
    private val documentationRenderer = object : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            if (value is CFMLDocumentationEntry) {
                val summary = value.description.replace(Regex("\\s+"), " ").trim().take(140)
                val foreground = label.foreground.toHtmlColor()
                label.text = "<html><span style='color:$foreground'><b>${value.name.htmlEscape()}</b> " +
                    "${value.type}<br>" +
                    "${summary.htmlEscape()}</span></html>"
                label.border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
            }
            return label
        }
    }

    init {
        border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        add(createControls(), BorderLayout.NORTH)

        entryList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        entryList.cellRenderer = documentationRenderer
        installResultContextMenu()
        entryList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) showSelectedEntry()
        }

        detailsPane.contentType = "text/html"
        detailsPane.isEditable = false
        detailsPane.addHyperlinkListener { event -> openLink(event) }
        refreshDetails()

        val splitPane = JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            JScrollPane(entryList),
            JScrollPane(detailsPane)
        ).apply {
            resizeWeight = 0.32
            dividerLocation = 250
            minimumSize = Dimension(0, 0)
        }
        add(splitPane, BorderLayout.CENTER)
        add(JPanel(BorderLayout(0, 4)).apply {
            add(statusLabel, BorderLayout.NORTH)
            add(JLabel("Powered by CFDocs.org"), BorderLayout.SOUTH)
        }, BorderLayout.SOUTH)

        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = applyFilters()
            override fun removeUpdate(event: DocumentEvent) = applyFilters()
            override fun changedUpdate(event: DocumentEvent) = applyFilters()
        })
        versionField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = applyFilters()
            override fun removeUpdate(event: DocumentEvent) = applyFilters()
            override fun changedUpdate(event: DocumentEvent) = applyFilters()
        })
        typeFilter.addActionListener { applyFilters() }
        engineFilter.addActionListener { applyFilters() }
        applyFilters()
        initialized = true
    }

    override fun updateUI() {
        super.updateUI()
        if (initialized) SwingUtilities.invokeLater { refreshDetails(preserveCaret = true) }
    }

    private fun createControls(): JPanel {
        val controls = JPanel(BorderLayout(0, 6))
        searchField.toolTipText = "Search function and tag names or descriptions"
        val search = JPanel(BorderLayout(6, 0))
        search.add(JLabel("Search"), BorderLayout.WEST)
        search.add(searchField, BorderLayout.CENTER)
        controls.add(search, BorderLayout.NORTH)

        val filters = JPanel(GridLayout(2, 3, 6, 2))
        typeFilter.toolTipText = "Filter by CFML documentation type"
        engineFilter.toolTipText = "Show records explicitly documented for this engine"
        versionField.toolTipText = "Optional engine version, for example 6 or 6.1.0"
        versionField.isEnabled = false
        filters.add(JLabel("Type"))
        filters.add(JLabel("Engine"))
        filters.add(JLabel("Version"))
        filters.add(typeFilter)
        filters.add(engineFilter)
        filters.add(versionField)
        controls.add(filters, BorderLayout.CENTER)
        return controls
    }

    private fun installResultContextMenu() {
        entryList.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) = showPopupForMouseEvent(event)
            override fun mouseReleased(event: MouseEvent) = showPopupForMouseEvent(event)
        })
        val showPopupAction = "showDocumentationResultPopup"
        entryList.inputMap.put(KeyStroke.getKeyStroke("shift F10"), showPopupAction)
        entryList.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), showPopupAction)
        entryList.actionMap.put(showPopupAction, object : javax.swing.AbstractAction() {
            override fun actionPerformed(event: java.awt.event.ActionEvent) {
                val index = entryList.selectedIndex
                val bounds = entryList.getCellBounds(index, index) ?: return
                showResultPopup(index, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2)
            }
        })
    }

    private fun showPopupForMouseEvent(event: MouseEvent) {
        if (!event.isPopupTrigger) return
        val index = entryList.locationToIndex(event.point)
        if (index < 0 || entryList.getCellBounds(index, index)?.contains(event.point) != true) return
        showResultPopup(index, event.x, event.y)
    }

    private fun showResultPopup(index: Int, x: Int, y: Int) {
        val popup = createResultPopup(index) ?: return
        popup.show(entryList, x, y)
    }

    internal fun createResultPopup(index: Int): JPopupMenu? {
        if (index !in 0 until entryList.model.size) return null
        entryList.selectedIndex = index
        val entry = entryList.model.getElementAt(index)
        val popup = JPopupMenu()
        popup.add(JMenuItem("Insert Function/Tag").apply {
            isEnabled = canInsertInSelectedEditor()
            addActionListener { insertEntry(entry) }
        })
        popup.add(JMenuItem("Copy Function/Tag Name").apply {
            addActionListener { copyText(CFMLDocumentationSnippetFormatter.name(entry)) }
        })
        popup.add(JMenuItem("Copy Function/Tag with Attributes").apply {
            addActionListener { copyText(CFMLDocumentationSnippetFormatter.withParameters(entry)) }
        })
        return popup
    }

    internal fun canInsertInSelectedEditor(): Boolean = selectedEditor()?.let { it.document.isWritable && !it.isViewer } == true

    private fun selectedEditor(): Editor? = project
        ?.takeUnless { it.isDisposed }
        ?.let { FileEditorManager.getInstance(it).selectedTextEditor }

    private fun insertEntry(entry: CFMLDocumentationEntry) {
        val editor = selectedEditor() ?: return
        insertSnippet(editor, CFMLDocumentationSnippetFormatter.withParameters(entry))
    }

    internal fun insertSnippet(editor: Editor, insertion: String): Boolean {
        val currentProject = project?.takeUnless { it.isDisposed } ?: return false
        if (!editor.document.isWritable || editor.isViewer) return false
        val caret = editor.caretModel.primaryCaret
        val start = if (caret.hasSelection()) caret.selectionStart else caret.offset
        val end = if (caret.hasSelection()) caret.selectionEnd else caret.offset
        WriteCommandAction.runWriteCommandAction(currentProject) {
            editor.document.replaceString(start, end, insertion)
            val newOffset = (start + insertion.length).coerceAtMost(editor.document.textLength)
            editor.caretModel.primaryCaret.removeSelection()
            editor.caretModel.primaryCaret.moveToOffset(newOffset)
        }
        return true
    }

    private fun copyText(text: String) {
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    private fun applyFilters() {
        val selectedEngine = engineFilter.selectedItem as EngineOption
        versionField.isEnabled = selectedEngine.id != null
        val version = if (selectedEngine.id == null) "" else versionField.text
        val versionError = CFMLDocumentationFilter.versionError(version)
        statusLabel.text = versionError ?: ""
        val result = if (versionError != null) {
            emptyList()
        } else {
            CFMLDocumentationFilter.filter(
                entries = entries,
                query = searchField.text,
                type = typeFilter.selectedItem as DocumentationTypeFilter,
                engineId = selectedEngine.id,
                version = version
            )
        }
        entryList.setListData(result.toTypedArray())
        if (result.isNotEmpty()) {
            entryList.selectedIndex = 0
            statusLabel.text = versionError ?: "${result.size} entries · Missing minimum versions are shown as unknown."
        } else {
            emptyMessage = "No matching CFML documentation was found."
            refreshDetails()
            if (versionError == null) statusLabel.text = "No matching entries."
        }
    }

    private fun showSelectedEntry() {
        emptyMessage = "Select a function or tag to view its documentation."
        refreshDetails()
    }

    private fun refreshDetails(preserveCaret: Boolean = false) {
        val caretPosition = detailsPane.caretPosition
        val entry = entryList.selectedValue
        detailsPane.text = entry?.let {
            CFMLDocumentationHtmlRenderer.render(it, detailsPane.foreground, detailsPane.background)
        } ?: CFMLDocumentationHtmlRenderer.renderMessage(
            emptyMessage,
            detailsPane.foreground,
            detailsPane.background
        )
        detailsPane.caretPosition = if (preserveCaret) caretPosition.coerceAtMost(detailsPane.document.length) else 0
    }

    private fun openLink(event: HyperlinkEvent) {
        if (event.eventType != HyperlinkEvent.EventType.ACTIVATED) return
        val uri = runCatching { URI(event.url.toString()) }.getOrNull() ?: return
        if (uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) {
            runCatching { BrowserUtil.browse(uri.toURL()) }
        }
    }

    private data class EngineOption(val id: String?, val label: String) {
        override fun toString(): String = label
    }

    companion object {
        private val engineOptions = listOf(
            EngineOption(null, "Any engine"),
            EngineOption("coldfusion", "Adobe ColdFusion"),
            EngineOption("lucee", "Lucee"),
            EngineOption("railo", "Railo"),
            EngineOption("rustcfml", "RustCFML"),
            EngineOption("boxlang", "BoxLang")
        )
    }
}

private fun String.htmlEscape(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

private fun Color.toHtmlColor(): String = "#%02x%02x%02x".format(red, green, blue)