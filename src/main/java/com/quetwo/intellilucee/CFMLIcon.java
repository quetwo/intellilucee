package com.quetwo.intellilucee;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

public final class CFMLIcon
{
    public static final Icon FILE = IconLoader.getIcon("/icons/cfType.svg", CFMLIcon.class);
    public static final Icon FILE_CFM = IconLoader.getIcon("/icons/CFMLFileIcon_CFM.svg", CFMLIcon.class);
    public static final Icon FILE_CFC = IconLoader.getIcon("/icons/CFMLFileIcon_CFC.svg", CFMLIcon.class);
    public static final Icon FILE_CFS = IconLoader.getIcon("/icons/CFMLFileIcon_CFS.svg", CFMLIcon.class);

    public static final Icon FUNCTION_PUBLIC = IconLoader.getIcon("/icons/function_public.svg", CFMLIcon.class);
    public static final Icon FUNCTION_PRIVATE = IconLoader.getIcon("/icons/function_private.svg", CFMLIcon.class);
    public static final Icon FUNCTION_REMOTE = IconLoader.getIcon("/icons/function_remote.svg", CFMLIcon.class);

    private  CFMLIcon()
    {

    }
}
