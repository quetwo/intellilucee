package com.quetwo.intellilucee.settings

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel

class CFMLFormatterApplicationConfigurable : BoundSearchableConfigurable("CFML Formatter", "settings.intellilucee.cfml.formatter.application")
{
    private val settings = CFMLFormatterApplicationSettings.getInstance()

    override fun createPanel(): DialogPanel
    {
        return panel {
            group("Defaults") {
                row {
                    checkBox("Formatter Enabled")
                        .bindSelected(settings.state::formatterEnabled)
                }
                row {
                    checkBox("Ignore White Space in Formatter")
                        .bindSelected(settings.state::ignoreWhiteSpaceInFormatter)
                }
                row {
                    checkBox("Format Within Query Tags")
                        .bindSelected(settings.state::formatWithinQueryTags)
                }
                row {
                    checkBox("Update CF tags to be lowercase")
                        .bindSelected(settings.state::updateCfTagsToLowercase)
                }
                row {
                    checkBox("Self Close HTML tags")
                        .bindSelected(settings.state::selfCloseHtmlTags)
                }
                row {
                    checkBox("Reject formatting results that change non-whitespace content")
                        .bindSelected(settings.state::rejectFormattingChangesToNonWhitespaceContent)
                }
                row {
                    checkBox("Update Attributes to be lowercase")
                        .bindSelected(settings.state::updateAttributesToLowercase)
                }
                row {
                    checkBox("Normalize attribute values to double quotes")
                        .bindSelected(settings.state::normalizeAttributeValuesToDoubleQuotes)
                }
                row {
                    checkBox("Normalize SQL keywords to upper-case")
                        .bindSelected(settings.state::normalizeSqlKeywordsToUppercase)
                }
                row("Normalize the CFML scope names") {
                    comboBox(listOf("upper", "lower", "leave"))
                        .bindItem(settings.state::normalizeCfmlScopeNames)
                }
                row("Comma placement in multi-line argument lists") {
                    comboBox(listOf("after", "before"))
                        .bindItem(settings.state::commaPlacementInMultilineArgumentLists)
                }
                row("Number of attributes per line") {
                    intTextField(1..1000)
                        .columns(8)
                        .bindIntText(settings.state::numberOfAttributesPerLine)
                }
                row("Line Width") {
                    intTextField(20..1000)
                        .columns(8)
                        .bindIntText(settings.state::lineWidth)
                }
            }
        }
    }
}
