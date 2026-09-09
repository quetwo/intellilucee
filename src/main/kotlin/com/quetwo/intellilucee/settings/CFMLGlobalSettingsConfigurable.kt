package com.quetwo.intellilucee.settings

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.quetwo.intellilucee.lsp.CFMLLspReleaseProvider

class CFMLGlobalSettingsConfigurable : BoundSearchableConfigurable("Lucee CFML", "settings.intellilucee.global")
{
    private val settings = CFMLGlobalSettings.getInstance()

    override fun createPanel(): DialogPanel
    {
        val versions = CFMLLspReleaseProvider.getAvailableVersions(settings.state.lspReleaseVersion)
        return panel {
            group("Language Server Protocol (LSP)") {
                row("LSP server release:") {
                    comboBox(versions)
                        .bindItem(
                            getter = { settings.state.lspReleaseVersion },
                            setter = { if (it != null) settings.state.lspReleaseVersion = it }
                        )
                        .comment("Select the release version of the CFML LSP server. Default is LATEST.")
                }
            }
        }
    }
}
