package com.quetwo.intellilucee.debugger

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.quetwo.intellilucee.lsp.CFMLLspClientDescriptor

class CFMLLineBreakpointType : XLineBreakpointType<CFMLLineBreakpointProperties>(
    "cfml.line.breakpoint",
    "CFML Line Breakpoints"
)
{
    override fun createBreakpointProperties(file: VirtualFile, line: Int): CFMLLineBreakpointProperties =
        CFMLLineBreakpointProperties()

    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean =
        CFMLLspClientDescriptor.isSupportedExtension(file.extension)
}
