package com.quetwo.intellilucee.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.LspClient;
import com.intellij.platform.lsp.api.LspClientManager;
import com.quetwo.intellilucee.lsp.CFMLLspClientDescriptor;
import com.quetwo.intellilucee.lsp.CFMLLspIntegrationProvider;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class CFMLOpenActiveApplicationFileAction extends AnAction
{
    private static final String COMMAND = "cfmleditor.openActiveApplicationFile";

    @Override
    public void actionPerformed(@NotNull AnActionEvent event)
    {
        Project project = event.getProject();
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || file == null)
        {
            return;
        }

        LspClientManager manager = LspClientManager.getInstance(project);
        manager.startClientsIfNeeded(CFMLLspIntegrationProvider.class);
        Collection<LspClient> clients = manager.getClients(CFMLLspIntegrationProvider.class);

        for (LspClient client : clients)
        {
            if (!client.getDescriptor().isSupportedFile(file))
            {
                continue;
            }

            ApplicationManager.getApplication().executeOnPooledThread(() ->
                client.sendRequestSync(LspClient.DEFAULT_REQUEST_TIMEOUT_MS, (LanguageServer server) -> server.getWorkspaceService().executeCommand(new ExecuteCommandParams(COMMAND, List.of(file.getUrl()))))
            );
            return;
        }
    }

    @Override
    public void update(@NotNull AnActionEvent event)
    {
        Project project = event.getProject();
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enabled = project != null && file != null && CFMLLspClientDescriptor.Companion.isSupportedExtension(file.getExtension());
        event.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.BGT;
    }
}