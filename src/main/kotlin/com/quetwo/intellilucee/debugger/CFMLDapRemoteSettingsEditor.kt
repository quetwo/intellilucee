package com.quetwo.intellilucee.debugger

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.components.JBTextField
import java.text.DecimalFormat
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.text.NumberFormatter

class CFMLDapRemoteSettingsEditor : SettingsEditor<CFMLDapRemoteConfiguration>()
{
    private val hostField = JBTextField()
    private val portSpinner = JSpinner(SpinnerNumberModel(10000, 1, 65535, 1))
    private val dapSecretField = JBTextField()

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
    }

    override fun applyEditorTo(configuration: CFMLDapRemoteConfiguration)
    {
        configuration.host = hostField.text.trim()
        configuration.port = (portSpinner.value as Number).toInt()
        configuration.dapSecret = dapSecretField.text.trim()
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

        panel.add(JPanel(), GridBagConstraints(0, 3, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0))
        return panel
    }
}
