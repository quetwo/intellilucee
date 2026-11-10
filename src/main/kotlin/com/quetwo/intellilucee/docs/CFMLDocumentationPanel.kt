package com.quetwo.intellilucee.docs

import com.intellij.ide.BrowserUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import java.net.URI
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.HyperlinkEvent

class CFMLDocumentationPanel : JPanel(BorderLayout(8, 8)) {
    private val entries = CFMLDocumentationRepository.loadEntries()
    private val searchField = JTextField()
    private val typeFilter = JComboBox(DocumentationTypeFilter.values())
    private val engineFilter = JComboBox(engineOptions.toTypedArray())
    private val versionField = JTextField()
    private val statusLabel = JLabel()
    private val entryList = JList<CFMLDocumentationEntry>()
    private val detailsPane = JEditorPane()
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
                label.text = "<html><b>${value.name.htmlEscape()}</b> " +
                    "<span style='color:#777'>${value.type}</span><br>" +
                    "<span>${summary.htmlEscape()}</span></html>"
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
        entryList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) showSelectedEntry()
        }

        detailsPane.contentType = "text/html"
        detailsPane.isEditable = false
        detailsPane.addHyperlinkListener { event -> openLink(event) }
        detailsPane.text = "<html><body>Select a function or tag to view its documentation.</body></html>"

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
        add(statusLabel, BorderLayout.SOUTH)

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
            detailsPane.text = "<html><body>No matching CFML documentation was found.</body></html>"
            if (versionError == null) statusLabel.text = "No matching entries."
        }
    }

    private fun showSelectedEntry() {
        val entry = entryList.selectedValue
        detailsPane.text = entry?.let(CFMLDocumentationHtmlRenderer::render)
            ?: "<html><body>Select a function or tag to view its documentation.</body></html>"
        detailsPane.caretPosition = 0
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