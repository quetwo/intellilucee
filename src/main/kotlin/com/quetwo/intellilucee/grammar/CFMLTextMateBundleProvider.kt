package com.quetwo.intellilucee.grammar

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider.PluginBundle

class CFMLTextMateBundleProvider: TextMateBundleProvider
{
    private val LOG: Logger = Logger.getInstance(CFMLTextMateBundleProvider::class.java)

    override fun getBundles(): List<PluginBundle>
    {
        val pluginPath = PluginManagerCore.getPlugin(PluginId.getId("com.quetwo.IntelliLucee"))?.pluginPath
            ?: error("Unable to resolve IntelliLucee plugin path")
        val bundlePath = pluginPath.resolve("grammar\\")
        val myCFMLBundle = PluginBundle("cfml", bundlePath)
        LOG.info("Installing Grammar for CFML -  ${myCFMLBundle.toString()}")
        return listOf(myCFMLBundle)
    }
}