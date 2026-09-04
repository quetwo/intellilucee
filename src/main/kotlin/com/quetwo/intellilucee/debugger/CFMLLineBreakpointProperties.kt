package com.quetwo.intellilucee.debugger

import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.xdebugger.breakpoints.XBreakpointProperties

class CFMLLineBreakpointProperties : XBreakpointProperties<CFMLLineBreakpointProperties>()
{
    override fun getState(): CFMLLineBreakpointProperties = this

    override fun loadState(state: CFMLLineBreakpointProperties)
    {
        XmlSerializerUtil.copyBean(state, this)
    }
}
