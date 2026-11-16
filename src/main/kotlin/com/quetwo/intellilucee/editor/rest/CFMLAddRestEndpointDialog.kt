package com.quetwo.intellilucee.editor.rest

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.DefaultCellEditor
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class CFMLAddRestEndpointDialog(project: Project?) : DialogWrapper(project, true) {

    private val functionNameField = JBTextField()
    private val httpMethods = arrayOf("GET", "POST", "PUT", "UPDATE", "DELETE")
    private val httpMethodCombo = ComboBox(httpMethods)
    private val restPathField = JBTextField("/")
    private val producesField = JBTextField("application/json")
    private val consumesField = JBTextField("")

    private val argumentSources = arrayOf("Path", "Query", "Form", "Matrix", "Header", "Cookie")

    private val argumentTypeColumn = object : ColumnInfo<RestArgument, String>("Argument Type") {
        override fun valueOf(item: RestArgument): String = item.argumentType

        override fun isCellEditable(item: RestArgument): Boolean = true

        override fun setValue(item: RestArgument, value: String) {
            item.argumentType = value
        }

        override fun getEditor(item: RestArgument): TableCellEditor {
            return DefaultCellEditor(JBTextField())
        }
    }

    private val argumentNameColumn = object : ColumnInfo<RestArgument, String>("Argument Name") {
        override fun valueOf(item: RestArgument): String = item.argumentName

        override fun isCellEditable(item: RestArgument): Boolean = true

        override fun setValue(item: RestArgument, value: String) {
            item.argumentName = value
        }

        override fun getEditor(item: RestArgument): TableCellEditor {
            return DefaultCellEditor(JBTextField())
        }
    }

    private val argumentSourceColumn = object : ColumnInfo<RestArgument, String>("Argument Source") {
        override fun valueOf(item: RestArgument): String = item.argumentSource

        override fun isCellEditable(item: RestArgument): Boolean = true

        override fun setValue(item: RestArgument, value: String) {
            item.argumentSource = value
        }

        override fun getEditor(item: RestArgument): TableCellEditor {
            return DefaultCellEditor(JComboBox(argumentSources))
        }

        override fun getRenderer(item: RestArgument): TableCellRenderer {
            return DefaultTableCellRenderer()
        }
    }

    private val tableModel = ListTableModel<RestArgument>(argumentTypeColumn, argumentNameColumn, argumentSourceColumn)
    private val tableView = TableView(tableModel)

    init {
        title = "Add REST Endpoint"
        setOKButtonText("Create")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val insets = Insets(4, 4, 4, 4)

        panel.add(JLabel("Function Name:"), GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0))
        panel.add(functionNameField, GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0))

        panel.add(JLabel("HTTP Method:"), GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0))
        panel.add(httpMethodCombo, GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0))

        panel.add(JLabel("REST Path:"), GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0))
        panel.add(restPathField, GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0))

        panel.add(JLabel("Produces:"), GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0))
        panel.add(producesField, GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0))

        panel.add(JLabel("Consumes:"), GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0))
        panel.add(consumesField, GridBagConstraints(1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0))

        val argumentsPanel = JPanel(BorderLayout())
        argumentsPanel.border = IdeBorderFactory.createTitledBorder("Function Arguments", false)

        val decorator = ToolbarDecorator.createDecorator(tableView)
            .setAddAction {
                val newArg = RestArgument(argumentType = "string", argumentName = "", argumentSource = "Path")
                tableModel.addRow(newArg)
                val rowIndex = tableModel.rowCount - 1
                tableView.setRowSelectionInterval(rowIndex, rowIndex)
            }
            .setRemoveAction {
                val selectedRow = tableView.selectedRow
                if (selectedRow >= 0 && selectedRow < tableModel.rowCount) {
                    tableModel.removeRow(selectedRow)
                }
            }
            .disableUpDownActions()

        argumentsPanel.preferredSize = Dimension(480, 180)
        argumentsPanel.add(decorator.createPanel(), BorderLayout.CENTER)

        panel.add(argumentsPanel, GridBagConstraints(0, 5, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0))

        panel.preferredSize = Dimension(500, 380)
        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent = functionNameField

    public override fun doValidate(): ValidationInfo? {
        if (functionNameField.text.trim().isEmpty()) {
            return ValidationInfo("Function Name cannot be empty", functionNameField)
        }
        return super.doValidate()
    }

    override fun doOKAction() {
        if (tableView.isEditing) {
            tableView.cellEditor?.stopCellEditing()
        }
        super.doOKAction()
    }

    fun getEndpointDescriptor(): RestEndpointDescriptor {
        val selectedMethod = (httpMethodCombo.selectedItem as? String) ?: "GET"
        return RestEndpointDescriptor(
            functionName = functionNameField.text.trim(),
            httpMethod = selectedMethod,
            restPath = restPathField.text.trim(),
            produces = producesField.text.trim(),
            consumes = consumesField.text.trim(),
            arguments = tableModel.items.toList()
        )
    }

    // Helper methods for programmatic/test configuration
    fun setFunctionName(name: String) {
        functionNameField.text = name
    }

    fun setHttpMethod(method: String) {
        httpMethodCombo.selectedItem = method
    }

    fun setRestPath(path: String) {
        restPathField.text = path
    }

    fun setProduces(produces: String) {
        producesField.text = produces
    }

    fun setConsumes(consumes: String) {
        consumesField.text = consumes
    }

    fun addArgument(type: String, name: String, source: String) {
        tableModel.addRow(RestArgument(type, name, source))
    }
}
