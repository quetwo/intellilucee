package com.quetwo.intellilucee.project;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator;
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.platform.ProjectGeneratorPeer;
import com.quetwo.intellilucee.CFMLIcon;
import com.quetwo.intellilucee.utils.QuickRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.IOException;

public final class CFMLProjectGenerator implements DirectoryProjectGenerator<CFMLProjectGenerator.Settings>, CustomStepProjectGenerator<CFMLProjectGenerator.Settings>
{
    @Override
    public @NotNull String getName()
    {
        return "ColdFusion application";
    }

    @Override
    public @Nullable Icon getLogo()
    {
        return CFMLIcon.FILE;
    }

    @Override
    public @NotNull ProjectGeneratorPeer<Settings> createPeer()
    {
        return new Peer();
    }

    @Override
    public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull Settings settings, @Nullable Module module)
    {
        WriteAction.run(() -> {
            try
            {
                VirtualFile cfml = VfsUtil.createDirectoryIfMissing(baseDir, "frontend");
                if (cfml == null)
                {
                    return;
                }

                VirtualFile webroot = VfsUtil.createDirectoryIfMissing(cfml, "webroot");
                if (webroot == null)
                {
                    return;
                }

                writeFile(webroot, "index.cfm", "");
                writeFile(webroot, "Application.cfc", "");

                if (settings.dockerEnabled)
                {
                    writeFile(baseDir, "docker-compose.yml", dockerCompose(settings));
                    writeFile(cfml, "Dockerfile", dockerfile(settings));
                    writeFile(cfml, "secrets.txt", cfSecretsFile(settings));

                    if (settings.includeDatabase)
                    {
                        VirtualFile db = VfsUtil.createDirectoryIfMissing(baseDir, "db");
                        VfsUtil.createDirectoryIfMissing(db, "sql");
                        if (db != null)
                        {
                            writeFile(db, "secrets.txt", dbSecretsFile(settings));
                        }
                    }

                    if (settings.includeReverseProxy)
                    {
                        VirtualFile proxy = VfsUtil.createDirectoryIfMissing(baseDir, "proxy");
                        if (proxy != null)
                        {
                            writeFile(proxy, "config.toml", "");
                        }
                    }
                }
            }
            catch (IOException ignored)
            {
            }
        });
    }

    @Override
    public @NotNull ValidationResult validate(@NotNull String baseDirPath)
    {
        if (baseDirPath.isBlank())
        {
            return new ValidationResult("Project location is required");
        }
        return ValidationResult.OK;
    }

    @Override
    public @NotNull AbstractActionWithPanel createStep(@NotNull DirectoryProjectGenerator<Settings> projectGenerator,
                                                       @NotNull AbstractNewProjectStep.AbstractCallback<Settings> callback)
    {
        return new ProjectSettingsStepBase<>(projectGenerator, callback);
    }

    private static void writeFile(@NotNull VirtualFile directory, @NotNull String name, @NotNull String content) throws IOException
    {
        VirtualFile file = directory.findChild(name);
        if (file == null)
        {
            file = directory.createChildData(CFMLProjectGenerator.class, name);
        }
        VfsUtil.saveText(file, content);
    }

    private static @NotNull String dockerfile(@NotNull Settings settings)
    {
        String luceeDockerVersionNumber = switch (settings.luceeVersion) {
            case "6.2.x" -> "6.2";
            case "7.1.x" -> "7.1";
            case "8.0.x" -> "8.0";
            default -> "LATEST";
        };

        return "FROM lucee/lucee:" + luceeDockerVersionNumber + "\n\n" +
                "RUN rm -R /var/www\n\n" +
                "WORKDIR /var/www\n" +
                "ENV LUCEE_LOGGING_FORCE_APPENDER=console\n" +
                "ENV LUCEE_LOGGING_FORCE_LEVEL=warning\n" +
                "COPY ./webroot/ /var/www\n" +
                "RUN /usr/local/tomcat/bin/prewarm.sh\n" +
                "EXPOSE 8888";
    }

    private static @NotNull String dockerCompose(@NotNull Settings settings)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("services:\n");
        builder.append("  frontend:\n");
        builder.append("    build:\n");
        builder.append("      context: frontend/\n");
        builder.append("      dockerfile: ./frontend/Dockerfile\n");
        builder.append("    container_name: ").append(settings.appName).append("\n");
        builder.append("    env_file:\n");
        builder.append("      - ./frontend/secrets.txt\n");
        if (settings.includeDatabase)
        {
            builder.append("      - ./db/secrets.txt\n");
        }
        if (!settings.includeReverseProxy)
        {
            builder.append("    ports:\n");
            builder.append("      - \"8888:8888\"\n");
        }

        if (settings.includeDatabase)
        {
            builder.append("\n  db:\n");
            builder.append("    image: ").append(databaseImage(settings.databaseType)).append("\n");
            builder.append("    env_file:\n");
            builder.append("        - ./db/secrets.txt\n");
            builder.append("    volumes:\n");
            builder.append("       - vol_db:/var/lib/mysql\n");
            builder.append("    ports:\n");
            builder.append("      - \"").append(databasePorts(settings.databaseType)).append("\"\n");
        }

        if (settings.includeReverseProxy)
        {
            builder.append("\n  proxy:\n");
            builder.append("    image: traefik:v3\n");
            builder.append("    ports:\n");
            builder.append("      - \"80:80\"\n");
            builder.append("      - \"443:443\"\n");
            builder.append("    volumes:\n");
            builder.append("       - vol_certs:/shared/certs/\n");
            builder.append("       - ./proxy/config.toml:/etc/traefik/traefik.toml\n");
            builder.append("       - /var/run/docker.sock:/var/run/docker.sock");
        }

        if (settings.includeDatabase ||  settings.includeReverseProxy)
        {
            builder.append("\n\nvolumes:\n");
            if (settings.includeDatabase)
            {
                builder.append("  vol_db:\n");
            }
            if (settings.includeReverseProxy)
            {
                builder.append("  vol_certs:\n");
            }
        }

        return builder.toString();
    }

    private static @NotNull String dbSecretsFile(@NotNull Settings settings)
    {
        StringBuilder builder = new StringBuilder();
        switch (settings.databaseType)
        {
            case "MySQL": case "MariaDB":
                builder.append("MYSQL_ROOT_PASSWORD=").append(QuickRandom.generateString(24));
                builder.append("\nMYSQL_DATABASE=db_").append(settings.appName);
                builder.append("\nMYSQL_USER=").append(settings.appName).append("_user");
                builder.append("\nMYSQL_PASSWORD=").append(QuickRandom.generateString(14));
                builder.append("\nDB_HOST=db");
                break;
            //TODO: ADD Secrets for POSTGRES and MSSQL
        }
        return  builder.toString();
    }

    private static @NotNull String cfSecretsFile(@NotNull Settings settings)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("LUCEE_ADMIN_ENABLES=true\n");
        builder.append("LUCEE_ADMIN_PASSWORD=").append(QuickRandom.generateString(8));
        return  builder.toString();
    }

    private static @NotNull String databaseImage(@NotNull String databaseType)
    {
        return switch (databaseType)
        {
            case "MySQL" -> "mysql:8.4";
            case "MariaDB" -> "mariadb:11";
            case "MSSQL" -> "mcr.microsoft.com/mssql/server:2022-latest";
            default -> "postgres:16";
        };
    }

    private static @NotNull String databasePorts(@Nullable String databaseType)
    {
        assert databaseType != null;
        return switch (databaseType)
        {
            case "Postgres" -> "5432:5432";
            case "MSSQL" -> "1433:1433";
            default -> "3306:3306";
        };
    }

    public static final class Settings
    {
        public final @NotNull String appName;
        public final @NotNull String luceeVersion;
        public final boolean dockerEnabled;
        public final boolean includeDatabase;
        public final @NotNull String databaseType;
        public final boolean includeReverseProxy;

        private Settings(@NotNull String appName, @NotNull String luceeVersion, boolean dockerEnabled, boolean includeDatabase, @NotNull String databaseType, boolean includeReverseProxy)
        {
            this.appName = appName;
            this.luceeVersion = luceeVersion;
            this.dockerEnabled = dockerEnabled;
            this.includeDatabase = includeDatabase;
            this.databaseType = databaseType;
            this.includeReverseProxy = includeReverseProxy;
        }
    }

    private static final class Peer implements ProjectGeneratorPeer<Settings>
    {
        private final JPanel panel = new JPanel(new GridBagLayout());
        private final JTextField appNameField = new JTextField("my-cfml-app", 25);
        private final JComboBox<String> luceeVersion = new JComboBox<>(new String[]{"6.2.x", "7.1.x", "8.0.x"});
        private final JCheckBox dockerEnabled = new JCheckBox("Docker-enabled project", true);
        private final JCheckBox includeDatabase = new JCheckBox("Include database server", false);
        private final JComboBox<String> databaseType = new JComboBox<>(new String[]{"MySQL", "MariaDB", "Postgres", "MSSQL"});
        private final JCheckBox includeReverseProxy = new JCheckBox("Include reverse proxy", false);

        private Peer()
        {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(4, 4, 4, 4);
            panel.add(new JLabel("Application name:"), c);

            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(appNameField, c);

            c.gridy++;
            c.gridx = 0;
            c.fill = GridBagConstraints.NONE;
            panel.add(new JLabel("Lucee version:"), c);

            c.gridx = 1;
            panel.add(luceeVersion, c);

            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 2;
            panel.add(dockerEnabled, c);

            c.gridy++;
            panel.add(includeDatabase, c);

            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 1;
            panel.add(new JLabel("Database type:"), c);

            c.gridx = 1;
            panel.add(databaseType, c);

            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 2;
            panel.add(includeReverseProxy, c);

            Runnable updateState = this::updateDynamicState;
            dockerEnabled.addActionListener(e -> updateState.run());
            includeDatabase.addActionListener(e -> updateState.run());
            updateDynamicState();
        }

        private void updateDynamicState()
        {
            boolean docker = dockerEnabled.isSelected();
            includeDatabase.setEnabled(docker);
            includeReverseProxy.setEnabled(docker);

            boolean db = docker && includeDatabase.isSelected();
            databaseType.setEnabled(db);
        }

        @Override
        public @NotNull JComponent getComponent(@NotNull TextFieldWithBrowseButton myLocationField, @NotNull Runnable checkValid)
        {
            appNameField.getDocument().addDocumentListener(new DocumentListener()
            {
                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    checkValid.run();
                }

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    checkValid.run();
                }

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    checkValid.run();
                }
            });
            return panel;
        }

        @Override
        public void buildUI(@NotNull SettingsStep settingsStep)
        {
        }

        @Override
        public @NotNull Settings getSettings()
        {
            return new Settings(
                    FileUtilRt.toSystemIndependentName(appNameField.getText().trim()).replace("/", "-").replace("\\", "-"),
                    String.valueOf(luceeVersion.getSelectedItem()),
                    dockerEnabled.isSelected(),
                    dockerEnabled.isSelected() && includeDatabase.isSelected(),
                    String.valueOf(databaseType.getSelectedItem()),
                    dockerEnabled.isSelected() && includeReverseProxy.isSelected()
            );
        }

        @Override
        public @Nullable ValidationInfo validate()
        {
            if (appNameField.getText().trim().isEmpty())
            {
                return new ValidationInfo("Application name is required", appNameField);
            }
            return null;
        }

        @Override
        public boolean isBackgroundJobRunning()
        {
            return false;
        }
    }
}
