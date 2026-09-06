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
        val pluginPath = PluginPathManager.getPluginHome("IntelliLucee").toPath()
        val bundlePath = pluginPath.resolve("grammar\\")
        val myCFMLBundle = PluginBundle("cfml", bundlePath)
        LOG.info("Installing Grammar for CFML -  ${myCFMLBundle.toString()}")
        return listOf(myCFMLBundle)
    }
}