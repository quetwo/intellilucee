package com.quetwo.intellilucee.docs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;

public class CFMLDocumentationToolWindowFactory implements ToolWindowFactory {
    public static final String TOOL_WINDOW_ID = "CFML Documentation";

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        var content = ContentFactory.getInstance().createContent(new CFMLDocumentationPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}