package com.quetwo.intellilucee.settings

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import java.awt.GridLayout
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class CFMLFormatterProjectOverridesConfigurable(private val project: Project) : SearchableConfigurable
{
    private lateinit var panel: JPanel
    private lateinit var moduleCombo: JComboBox<Module>
    private lateinit var overrideApplicationSettingsCheckBox: JCheckBox
    private lateinit var formatterEnabledCheckBox: JCheckBox
    private lateinit var ignoreWhiteSpaceCheckBox: JCheckBox
    private lateinit var formatWithinQueryTagsCheckBox: JCheckBox
    private lateinit var updateCfTagsToLowercaseCheckBox: JCheckBox
    private lateinit var selfCloseHtmlTagsCheckBox: JCheckBox
    private lateinit var rejectNonWhitespaceChangesCheckBox: JCheckBox
    private lateinit var updateAttributesToLowercaseCheckBox: JCheckBox
    private lateinit var normalizeAttributeValuesToDoubleQuotesCheckBox: JCheckBox
    private lateinit var normalizeSqlKeywordsToUppercaseCheckBox: JCheckBox
    private lateinit var normalizeCfmlScopeNamesComboBox: JComboBox<String>
    private lateinit var commaPlacementComboBox: JComboBox<String>
    private lateinit var numberOfAttributesPerLineSpinner: JSpinner
    private lateinit var lineWidthSpinner: JSpinner

    override fun getId(): String = "settings.intellilucee.cfml.formatter.project.overrides"

    override fun getDisplayName(): String = "CFML Formatter Module Settings"

    override fun createComponent(): JComponent
    {
        moduleCombo = JComboBox(ModuleManager.getInstance(project).modules)
        overrideApplicationSettingsCheckBox = JCheckBox("Override application settings")
        formatterEnabledCheckBox = JCheckBox("Formatter Enabled")
        ignoreWhiteSpaceCheckBox = JCheckBox("Ignore White Space in Formatter")
        formatWithinQueryTagsCheckBox = JCheckBox("Format Within Query Tags")
        updateCfTagsToLowercaseCheckBox = JCheckBox("Update CF tags to be lowercase")
        selfCloseHtmlTagsCheckBox = JCheckBox("Self Close HTML tags")
        rejectNonWhitespaceChangesCheckBox = JCheckBox("Reject formatting results that change non-whitespace content")
        updateAttributesToLowercaseCheckBox = JCheckBox("Update Attributes to be lowercase")
        normalizeAttributeValuesToDoubleQuotesCheckBox = JCheckBox("Normalize attribute values to double quotes")
        normalizeSqlKeywordsToUppercaseCheckBox = JCheckBox("Normalize SQL keywords to upper-case")
        normalizeCfmlScopeNamesComboBox = JComboBox(arrayOf("upper", "lower", "leave"))
        commaPlacementComboBox = JComboBox(arrayOf("after", "before"))
        numberOfAttributesPerLineSpinner = JSpinner(SpinnerNumberModel(1, 1, 1000, 1))
        lineWidthSpinner = JSpinner(SpinnerNumberModel(120, 20, 1000, 1))

        panel = JPanel(GridLayout(0, 1, 0, 4)).apply {
            add(JLabel("Module"))
            add(moduleCombo)
            add(overrideApplicationSettingsCheckBox)
            add(formatterEnabledCheckBox)
            add(ignoreWhiteSpaceCheckBox)
            add(formatWithinQueryTagsCheckBox)
            add(updateCfTagsToLowercaseCheckBox)
            add(selfCloseHtmlTagsCheckBox)
            add(rejectNonWhitespaceChangesCheckBox)
            add(updateAttributesToLowercaseCheckBox)
            add(normalizeAttributeValuesToDoubleQuotesCheckBox)
            add(normalizeSqlKeywordsToUppercaseCheckBox)
            add(JLabel("Normalize the CFML scope names"))
            add(normalizeCfmlScopeNamesComboBox)
            add(JLabel("Comma placement in multi-line argument lists"))
            add(commaPlacementComboBox)
            add(JLabel("Number of attributes per line"))
            add(numberOfAttributesPerLineSpinner)
            add(JLabel("Line Width"))
            add(lineWidthSpinner)
        }

        moduleCombo.addActionListener { reset() }
        reset()
        return panel
    }

    override fun isModified(): Boolean
    {
        val module = moduleCombo.selectedItem as? Module ?: return false
        val state = CFMLFormatterModuleSettings.getInstance(module).state
        return state.overrideApplicationSettings != overrideApplicationSettingsCheckBox.isSelected ||
            state.formatterEnabled != formatterEnabledCheckBox.isSelected ||
            state.ignoreWhiteSpaceInFormatter != ignoreWhiteSpaceCheckBox.isSelected ||
            state.formatWithinQueryTags != formatWithinQueryTagsCheckBox.isSelected ||
            state.updateCfTagsToLowercase != updateCfTagsToLowercaseCheckBox.isSelected ||
            state.selfCloseHtmlTags != selfCloseHtmlTagsCheckBox.isSelected ||
            state.rejectFormattingChangesToNonWhitespaceContent != rejectNonWhitespaceChangesCheckBox.isSelected ||
            state.updateAttributesToLowercase != updateAttributesToLowercaseCheckBox.isSelected ||
            state.normalizeAttributeValuesToDoubleQuotes != normalizeAttributeValuesToDoubleQuotesCheckBox.isSelected ||
            state.normalizeSqlKeywordsToUppercase != normalizeSqlKeywordsToUppercaseCheckBox.isSelected ||
            state.normalizeCfmlScopeNames != normalizeCfmlScopeNamesComboBox.selectedItem as String ||
            state.commaPlacementInMultilineArgumentLists != commaPlacementComboBox.selectedItem as String ||
            state.numberOfAttributesPerLine != (numberOfAttributesPerLineSpinner.value as Int) ||
            state.lineWidth != (lineWidthSpinner.value as Int)
    }

    override fun apply()
    {
        val module = moduleCombo.selectedItem as? Module ?: return
        val state = CFMLFormatterModuleSettings.getInstance(module).state
        state.overrideApplicationSettings = overrideApplicationSettingsCheckBox.isSelected
        state.formatterEnabled = formatterEnabledCheckBox.isSelected
        state.ignoreWhiteSpaceInFormatter = ignoreWhiteSpaceCheckBox.isSelected
        state.formatWithinQueryTags = formatWithinQueryTagsCheckBox.isSelected
        state.updateCfTagsToLowercase = updateCfTagsToLowercaseCheckBox.isSelected
        state.selfCloseHtmlTags = selfCloseHtmlTagsCheckBox.isSelected
        state.rejectFormattingChangesToNonWhitespaceContent = rejectNonWhitespaceChangesCheckBox.isSelected
        state.updateAttributesToLowercase = updateAttributesToLowercaseCheckBox.isSelected
        state.normalizeAttributeValuesToDoubleQuotes = normalizeAttributeValuesToDoubleQuotesCheckBox.isSelected
        state.normalizeSqlKeywordsToUppercase = normalizeSqlKeywordsToUppercaseCheckBox.isSelected
        state.normalizeCfmlScopeNames = normalizeCfmlScopeNamesComboBox.selectedItem as String
        state.commaPlacementInMultilineArgumentLists = commaPlacementComboBox.selectedItem as String
        state.numberOfAttributesPerLine = numberOfAttributesPerLineSpinner.value as Int
        state.lineWidth = lineWidthSpinner.value as Int
    }

    override fun reset()
    {
        val module = if (::moduleCombo.isInitialized) moduleCombo.selectedItem as? Module else null
        if (module == null)
        {
            return
        }

        val state = CFMLFormatterModuleSettings.getInstance(module).state
        overrideApplicationSettingsCheckBox.isSelected = state.overrideApplicationSettings
        formatterEnabledCheckBox.isSelected = state.formatterEnabled
        ignoreWhiteSpaceCheckBox.isSelected = state.ignoreWhiteSpaceInFormatter
        formatWithinQueryTagsCheckBox.isSelected = state.formatWithinQueryTags
        updateCfTagsToLowercaseCheckBox.isSelected = state.updateCfTagsToLowercase
        selfCloseHtmlTagsCheckBox.isSelected = state.selfCloseHtmlTags
        rejectNonWhitespaceChangesCheckBox.isSelected = state.rejectFormattingChangesToNonWhitespaceContent
        updateAttributesToLowercaseCheckBox.isSelected = state.updateAttributesToLowercase
        normalizeAttributeValuesToDoubleQuotesCheckBox.isSelected = state.normalizeAttributeValuesToDoubleQuotes
        normalizeSqlKeywordsToUppercaseCheckBox.isSelected = state.normalizeSqlKeywordsToUppercase
        normalizeCfmlScopeNamesComboBox.selectedItem = state.normalizeCfmlScopeNames ?: "leave"
        commaPlacementComboBox.selectedItem = state.commaPlacementInMultilineArgumentLists ?: "after"
        numberOfAttributesPerLineSpinner.value = state.numberOfAttributesPerLine
        lineWidthSpinner.value = state.lineWidth
    }

    override fun disposeUIResources()
    {
    }
}
