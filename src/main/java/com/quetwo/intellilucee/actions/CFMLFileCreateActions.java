package com.quetwo.intellilucee.actions;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiDirectory;
import com.quetwo.intellilucee.CFMLIcon;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public final class CFMLFileCreateActions extends CreateFileFromTemplateAction implements DumbAware
{

    private static final @NonNls
    @NotNull String CLASS_TEMPLATE_TAG_NAME = "CFML Class (tag)";
    private static final @NonNls
    @NotNull String CLASS_TEMPLATE_SCRIPT_NAME = "CFML Class (script)";
    private static final @NonNls
    @NotNull String PAGE_TEMPLATE_TAG_NAME = "CFML Page (tag)";
    private static final @NonNls
    @NotNull String PAGE_TEMPLATE_SCRIPT_NAME = "CFML Page (script)";

    public CFMLFileCreateActions()
    {
        super("CFML File","Create CFML File", CFMLIcon.FILE);
    }

    @Override
    protected void buildDialog(@NotNull Project project, @NotNull PsiDirectory psiDirectory, CreateFileFromTemplateDialog.@NotNull Builder builder)
    {
        builder.setTitle( "New CFML File" )
                .addKind( "CFML Class (tag)", CFMLIcon.FILE, CLASS_TEMPLATE_TAG_NAME )
                .addKind( "CFML Class (script)", CFMLIcon.FILE, CLASS_TEMPLATE_SCRIPT_NAME )
                .addKind( "CFML Page (tag)", CFMLIcon.FILE, PAGE_TEMPLATE_TAG_NAME )
                .addKind( "CFML Page (script)", CFMLIcon.FILE, PAGE_TEMPLATE_SCRIPT_NAME );
    }

    @Override
    protected @NlsContexts.Command String getActionName(PsiDirectory psiDirectory, @NonNls @NotNull String s, @NonNls String s1)
    {
        return "Create CFML File";
    }
}
