package com.quetwo.intellilucee.debugger

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.Messages
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.nio.file.Path
import java.text.DecimalFormat
import javax.swing.JComponent
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.text.NumberFormatter

open class RustCFMLSettingsEditor(
    private val project: Project,
    private val releaseDownloader: RustCFMLReleaseDownloader = RustCFMLReleaseDownloader()
) : SettingsEditor<RustCFMLRunConfiguration>()
{
    private val executableField = TextFieldWithBrowseButton()
    private val applicationDirectoryField = TextFieldWithBrowseButton()
    private val portSpinner = JSpinner(SpinnerNumberModel(RustCFMLRunConfiguration.DEFAULT_PORT, 1, 65535, 1))
    private val downloadLatestButton = JButton("Download latest")

    init
    {
        val executableDescriptor = FileChooserDescriptorFactory.singleFile()
            .withTitle("Select RUSTCFML executable")
            .withDescription("Select the local RUSTCFML executable")
        executableField.addBrowseFolderListener(project, executableDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT)
        downloadLatestButton.addActionListener { downloadLatestRelease() }

        val directoryDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Select application directory")
            .withDescription("Select the application root inside this project")
        applicationDirectoryField.addBrowseFolderListener(project, directoryDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT)

        val numberEditor = JSpinner.NumberEditor(portSpinner, "#")
        val formatter = numberEditor.textField.formatter as NumberFormatter
        (formatter.format as DecimalFormat).isGroupingUsed = false
        portSpinner.editor = numberEditor
    }

    override fun resetEditorFrom(configuration: RustCFMLRunConfiguration)
    {
        executableField.text = configuration.executablePath
        applicationDirectoryField.text = configuration.applicationDirectory
        portSpinner.value = configuration.port.coerceIn(1, 65535)
    }

    override fun applyEditorTo(configuration: RustCFMLRunConfiguration)
    {
        configuration.executablePath = executableField.text.trim()
        configuration.applicationDirectory = applicationDirectoryField.text.trim()
        configuration.port = (portSpinner.value as Number).toInt()
    }

    override fun createEditor(): JComponent
    {
        val panel = JPanel(GridBagLayout())
        val executablePanel = JPanel(BorderLayout(4, 0)).apply {
            add(executableField, BorderLayout.CENTER)
            add(downloadLatestButton, BorderLayout.EAST)
        }
        val insets = Insets(4, 4, 4, 4)
        val constraints = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.NONE
            this.insets = insets
        }

        panel.add(JLabel("RUSTCFML executable:"), constraints)
        constraints.gridx = 1
        constraints.weightx = 1.0
        constraints.fill = GridBagConstraints.HORIZONTAL
        panel.add(executablePanel, constraints)

        constraints.gridx = 0
        constraints.gridy++
        constraints.weightx = 0.0
        constraints.fill = GridBagConstraints.NONE
        panel.add(JLabel("Application directory:"), constraints)
        constraints.gridx = 1
        constraints.weightx = 1.0
        constraints.fill = GridBagConstraints.HORIZONTAL
        panel.add(applicationDirectoryField, constraints)

        constraints.gridx = 0
        constraints.gridy++
        constraints.weightx = 0.0
        constraints.fill = GridBagConstraints.NONE
        panel.add(JLabel("Port:"), constraints)
        constraints.gridx = 1
        panel.add(portSpinner, constraints)

        return panel
    }

    private fun downloadLatestRelease()
    {
        object : Task.Backgroundable(project, "Downloading RUSTCFML", true)
        {
            private var downloadedExecutable: Path? = null

            override fun run(indicator: ProgressIndicator)
            {
                downloadedExecutable = releaseDownloader.downloadLatest(indicator)
            }

            override fun onSuccess()
            {
                downloadedExecutable?.let { executableField.text = it.toString() }
            }

            override fun onThrowable(error: Throwable)
            {
                Messages.showErrorDialog(
                    project,
                    "Failed to download the latest RUSTCFML executable: ${error.message ?: "unknown error"}",
                    "RUSTCFML Download Failed"
                )
            }
        }.queue()
    }
}