package com.quetwo.intellilucee.settings

import com.intellij.openapi.module.Module

data class CFMLFormatterEffectiveSettings(
    val formatterEnabled: Boolean,
    val ignoreWhiteSpaceInFormatter: Boolean,
    val formatWithinQueryTags: Boolean,
    val updateCfTagsToLowercase: Boolean,
    val selfCloseHtmlTags: Boolean,
    val rejectFormattingChangesToNonWhitespaceContent: Boolean,
    val updateAttributesToLowercase: Boolean,
    val normalizeAttributeValuesToDoubleQuotes: Boolean,
    val normalizeSqlKeywordsToUppercase: Boolean,
    val normalizeCfmlScopeNames: String,
    val commaPlacementInMultilineArgumentLists: String,
    val numberOfAttributesPerLine: Int,
    val lineWidth: Int,
)

object CFMLFormatterSettingsResolver
{
    fun resolve(module: Module?): CFMLFormatterEffectiveSettings
    {
        val applicationState = CFMLFormatterApplicationSettings.getInstance().state

        if (module == null)
        {
            return applicationState.toEffectiveSettings()
        }

        val moduleState = CFMLFormatterModuleSettings.getInstance(module).state
        if (!moduleState.overrideApplicationSettings)
        {
            return applicationState.toEffectiveSettings()
        }

        return CFMLFormatterEffectiveSettings(
            formatterEnabled = moduleState.formatterEnabled,
            ignoreWhiteSpaceInFormatter = moduleState.ignoreWhiteSpaceInFormatter,
            formatWithinQueryTags = moduleState.formatWithinQueryTags,
            updateCfTagsToLowercase = moduleState.updateCfTagsToLowercase,
            selfCloseHtmlTags = moduleState.selfCloseHtmlTags,
            rejectFormattingChangesToNonWhitespaceContent = moduleState.rejectFormattingChangesToNonWhitespaceContent,
            updateAttributesToLowercase = moduleState.updateAttributesToLowercase,
            normalizeAttributeValuesToDoubleQuotes = moduleState.normalizeAttributeValuesToDoubleQuotes,
            normalizeSqlKeywordsToUppercase = moduleState.normalizeSqlKeywordsToUppercase,
            normalizeCfmlScopeNames = moduleState.normalizeCfmlScopeNames ?: "leave",
            commaPlacementInMultilineArgumentLists = moduleState.commaPlacementInMultilineArgumentLists ?: "after",
            numberOfAttributesPerLine = moduleState.numberOfAttributesPerLine,
            lineWidth = moduleState.lineWidth,
        )
    }

    private fun CFMLFormatterSettingsState.toEffectiveSettings(): CFMLFormatterEffectiveSettings
    {
        return CFMLFormatterEffectiveSettings(
            formatterEnabled = formatterEnabled,
            ignoreWhiteSpaceInFormatter = ignoreWhiteSpaceInFormatter,
            formatWithinQueryTags = formatWithinQueryTags,
            updateCfTagsToLowercase = updateCfTagsToLowercase,
            selfCloseHtmlTags = selfCloseHtmlTags,
            rejectFormattingChangesToNonWhitespaceContent = rejectFormattingChangesToNonWhitespaceContent,
            updateAttributesToLowercase = updateAttributesToLowercase,
            normalizeAttributeValuesToDoubleQuotes = normalizeAttributeValuesToDoubleQuotes,
            normalizeSqlKeywordsToUppercase = normalizeSqlKeywordsToUppercase,
            normalizeCfmlScopeNames = normalizeCfmlScopeNames ?: "leave",
            commaPlacementInMultilineArgumentLists = commaPlacementInMultilineArgumentLists ?: "after",
            numberOfAttributesPerLine = numberOfAttributesPerLine,
            lineWidth = lineWidth,
        )
    }
}
