package com.quetwo.intellilucee.grammar

import com.intellij.openapi.application.PluginPathManager
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider.PluginBundle

class CFMLTextMateBundleProvider: TextMateBundleProvider
{
    private val LOG: Logger = Logger.getInstance(CFMLTextMateBundleProvider::class.java)

    override fun getBundles(): List<PluginBundle>
    {
        val pluginPath = PluginPathManager.getPluginResource(CFMLTextMateBundleProvider::class.java, "grammar/")
        if (pluginPath == null)
        {
            LOG.error("CFML Grammar bundle was not found! Unable to start Textmate bundle.")
            return emptyList()
        }
        val myCFMLBundle = PluginBundle("cfml", pluginPath.toPath())
        LOG.info("Installing Grammar for CFML -  ${myCFMLBundle.toString()}")
        return listOf(myCFMLBundle)
    }
}