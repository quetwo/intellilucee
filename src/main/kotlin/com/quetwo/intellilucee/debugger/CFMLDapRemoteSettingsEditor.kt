package com.quetwo.intellilucee.debugger

import com.intellij.execution.util.ListTableWithButtons
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.TableView
import com.intellij.util.ui.AbstractTableCellEditor
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.text.DecimalFormat
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JTable
import javax.swing.SpinnerNumberModel
import javax.swing.text.NumberFormatter

class CFMLDapRemoteSettingsEditor(private val project: Project? = null) : SettingsEditor<CFMLDapRemoteConfiguration>()
{
    private val hostField = JBTextField()
    private val portSpinner = JSpinner(SpinnerNumberModel(10000, 1, 65535, 1))
    private val dapSecretField = JBTextField()

    private class PathTransformWrapper(var idePrefix: String, var serverPrefix: String)

    private val idePrefixColumn = object : ColumnInfo<PathTransformWrapper, String>("IDE Path (Local)") {
        override fun valueOf(item: PathTransformWrapper): String = item.idePrefix

        override fun isCellEditable(item: PathTransformWrapper): Boolean = true

        override fun setValue(item: PathTransformWrapper, value: String) {
            item.idePrefix = value
        }

        override fun getEditor(item: PathTransformWrapper): javax.swing.table.TableCellEditor {
            return object : AbstractTableCellEditor() {
                private val browseField = TextFieldWithBrowseButton()

                init {
                    val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select IDE Local Directory")
                        .withDescription("Choose the project folder mapped to the server directory")
                    browseField.addBrowseFolderListener(
                        project,
                        descriptor,
                        TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
                    )
                }

                override fun getCellEditorValue(): Any = browseField.text

                override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
                    browseField.text = value?.toString() ?: ""
                    return browseField
                }
            }
        }
    }

    private val serverPrefixColumn = object : ColumnInfo<PathTransformWrapper, String>("Server Path (Remote)") {
        override fun valueOf(item: PathTransformWrapper): String = item.serverPrefix

        override fun isCellEditable(item: PathTransformWrapper): Boolean = true

        override fun setValue(item: PathTransformWrapper, value: String) {
            item.serverPrefix = value
        }
    }

    private val tableModel = ListTableModel<PathTransformWrapper>(idePrefixColumn, serverPrefixColumn)
    private val tableView = TableView(tableModel)

    init
    {
        val numberEditor = JSpinner.NumberEditor(portSpinner, "#")
        val formatter = numberEditor.textField.formatter as NumberFormatter
        (formatter.format as DecimalFormat).isGroupingUsed = false
        portSpinner.editor = numberEditor
    }

    override fun resetEditorFrom(configuration: CFMLDapRemoteConfiguration)
    {
        hostField.text = configuration.host
        portSpinner.value = configuration.port
        dapSecretField.text = configuration.dapSecret

        val transforms = configuration.getEffectivePathTransforms()
        val wrappers = transforms.map { PathTransformWrapper(it.idePrefix, it.serverPrefix) }.toMutableList()
        tableModel.items = wrappers
    }

    override fun applyEditorTo(configuration: CFMLDapRemoteConfiguration)
    {
        if (tableView.isEditing)
        {
            tableView.cellEditor?.stopCellEditing()
        }
        configuration.host = hostField.text.trim()
        configuration.port = (portSpinner.value as Number).toInt()
        configuration.dapSecret = dapSecretField.text.trim()

        val transforms = tableModel.items
            .map { PathTransform(it.idePrefix.trim(), it.serverPrefix.trim()) }
            .filter { it.idePrefix.isNotBlank() || it.serverPrefix.isNotBlank() }

        configuration.pathTransforms = transforms
        if (transforms.isNotEmpty())
        {
            configuration.idePrefix = transforms.first().idePrefix
            configuration.serverPrefix = transforms.first().serverPrefix
        }
        else
        {
            configuration.idePrefix = ""
            configuration.serverPrefix = ""
        }
    }

    override fun createEditor(): JComponent
    {
        val panel = JPanel(GridBagLayout())
        val insets = Insets(4, 4, 4, 4)

        panel.add(JLabel("Host:"), GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0))
        panel.add(hostField, GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0))

        panel.add(JLabel("Port:"), GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0))
        panel.add(portSpinner, GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0))

        panel.add(JLabel("DAP Secret:"), GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0))
        panel.add(dapSecretField, GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0))

        val pathMappingPanel = JPanel(BorderLayout())
        val decorator = ToolbarDecorator.createDecorator(tableView)
            .setAddAction {
                val newWrapper = PathTransformWrapper("", "")
                tableModel.addRow(newWrapper)
                val rowIndex = tableModel.rowCount - 1
                tableView.setRowSelectionInterval(rowIndex, rowIndex)
            }
            .setRemoveAction {
                val selectedRow = tableView.selectedRow
                if (selectedRow >= 0 && selectedRow < tableModel.rowCount)
                {
                    tableModel.removeRow(selectedRow)
                }
            }
            .disableUpDownActions()

        pathMappingPanel.preferredSize = Dimension(450, 150)
        pathMappingPanel.add(decorator.createPanel(), BorderLayout.CENTER)

        panel.add(JLabel("Path Mappings:"), GridBagConstraints(0, 3, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0))
        panel.add(pathMappingPanel, GridBagConstraints(0, 4, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0))

        return panel
    }
}
