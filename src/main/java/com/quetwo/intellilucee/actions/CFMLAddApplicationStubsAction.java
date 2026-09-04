package com.quetwo.intellilucee.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CFMLAddApplicationStubsAction extends AnAction
{
    private static final String APPLICATION_CFC_FILE_NAME = "application.cfc";

    private static final List<FunctionStub> AVAILABLE_STUBS = List.of(
        new FunctionStub("onApplicationStart", "public boolean function onApplicationStart()\n{\n    return true;\n}"),
        new FunctionStub("onApplicationEnd", "public void function onApplicationEnd(struct applicationScope={})\n{\n    return;\n}"),
        new FunctionStub("onSessionStart", "public void function onSessionStart()\n{\n    return;\n}"),
        new FunctionStub("onSessionEnd", "public void function onSessionEnd(required struct sessionScope, struct applicationScope={})\n{\n    return;\n}"),
        new FunctionStub("onRequestStart", "public boolean function onRequestStart(required string targetPage)\n{\n    return true;\n}"),
        new FunctionStub("onRequest", "public void function onRequest(required string targetPage)\n{\ninclude arguments.targetPage;\n    return;\n}"),
        new FunctionStub("onRequestEnd","public void function onRequestEnd()\n{\n    return;\n}"),
        new FunctionStub("onCFCRequest","public void function onCFCRequest(string cfcName, string method, struct args)\n{\n    return;\n}"),
        new FunctionStub("onError", "public void function onError(required any exception, required string eventName)\n{\n    return;\n}"),
        new FunctionStub("onAbort","public void function onAbort(required string targetPage)\n{\n    return;\n}"),
        new FunctionStub("onMissingTemplate", "public boolean function onMissingTemplate(required string targetPage)\n{\n    return true;\n}")
    );

    @Override
    public void actionPerformed(@NotNull AnActionEvent event)
    {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || editor == null || file == null)
        {
            return;
        }

        Document document = editor.getDocument();
        List<FunctionStub> selectableStubs = getMissingStubs(document.getText());
        if (selectableStubs.isEmpty())
        {
            return;
        }

        StubSelectionDialog dialog = new StubSelectionDialog(selectableStubs);
        if (!dialog.showAndGet())
        {
            return;
        }

        List<FunctionStub> selectedStubs = dialog.getSelectedStubs();
        if (selectedStubs.isEmpty())
        {
            return;
        }

        String insertionText = buildInsertionText(selectedStubs);
        WriteCommandAction.runWriteCommandAction(project, () -> insertStubs(document, insertionText));
    }

    @Override
    public void update(@NotNull AnActionEvent event)
    {
        Project project = event.getProject();
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enabled = project != null && file != null && APPLICATION_CFC_FILE_NAME.equals(file.getName().toLowerCase(Locale.ROOT));
        event.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.BGT;
    }

    private static List<FunctionStub> getMissingStubs(String documentText)
    {
        List<FunctionStub> missing = new ArrayList<>();
        String lowerText = documentText.toLowerCase(Locale.ROOT);
        for (FunctionStub stub : AVAILABLE_STUBS)
        {
            String marker = "function " + stub.name.toLowerCase(Locale.ROOT) + "(";
            if (!lowerText.contains(marker))
            {
                missing.add(stub);
            }
        }
        return missing;
    }

    private static String buildInsertionText(List<FunctionStub> stubs)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < stubs.size(); i++)
        {
            if (i > 0)
            {
                builder.append("\n\n");
            }
            builder.append(stubs.get(i).body);
        }
        return builder.toString();
    }

    private static void insertStubs(Document document, String insertionText)
    {
        String text = document.getText();
        int insertOffset = findInsertOffset(text);
        String prefix = needsLeadingSpacing(text, insertOffset) ? "\n\n" : "";
        String suffix = needsTrailingSpacing(text, insertOffset) ? "\n\n" : "\n";
        document.insertString(insertOffset, prefix + insertionText + suffix);
    }

    private static int findInsertOffset(String text)
    {
        int cfcomponentClose = text.toLowerCase(Locale.ROOT).lastIndexOf("</cfcomponent>");
        if (cfcomponentClose >= 0)
        {
            return cfcomponentClose;
        }

        int scriptClose = text.lastIndexOf('}');
        if (scriptClose >= 0)
        {
            return scriptClose;
        }

        return text.length();
    }

    private static boolean needsLeadingSpacing(String text, int insertOffset)
    {
        if (insertOffset == 0)
        {
            return false;
        }
        return !text.substring(0, insertOffset).endsWith("\n\n");
    }

    private static boolean needsTrailingSpacing(String text, int insertOffset)
    {
        if (insertOffset >= text.length())
        {
            return false;
        }
        return !text.substring(insertOffset).startsWith("\n\n");
    }

    private static final class FunctionStub
    {
        private final String name;
        private final String body;

        private FunctionStub(String name, String body)
        {
            this.name = name;
            this.body = body;
        }
    }

    private static final class StubSelectionDialog extends DialogWrapper
    {
        private final List<FunctionStub> selectableStubs;
        private final List<JCheckBox> checkBoxes = new ArrayList<>();

        private StubSelectionDialog(List<FunctionStub> selectableStubs)
        {
            super(true);
            this.selectableStubs = selectableStubs;
            setTitle("Add Application.cfc Lifecycle Functions");
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel()
        {
            JPanel container = new JPanel(new BorderLayout());
            container.add(new JLabel("Select application lifecycle functions to add:"), BorderLayout.NORTH);

            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

            for (FunctionStub stub : selectableStubs)
            {
                JCheckBox checkBox = new JCheckBox(stub.name);
                checkBox.setSelected(true);
                checkBoxes.add(checkBox);
                listPanel.add(checkBox);
            }

            container.add(listPanel, BorderLayout.CENTER);
            return container;
        }

        private List<FunctionStub> getSelectedStubs()
        {
            List<FunctionStub> selected = new ArrayList<>();
            for (int i = 0; i < checkBoxes.size(); i++)
            {
                if (checkBoxes.get(i).isSelected())
                {
                    selected.add(selectableStubs.get(i));
                }
            }
            return selected;
        }
    }
}