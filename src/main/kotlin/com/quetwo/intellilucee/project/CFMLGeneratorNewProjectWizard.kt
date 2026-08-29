package com.quetwo.intellilucee.project

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.GitNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.RootNewProjectWizardStep
import com.intellij.ide.wizard.newProjectWizardBaseStepWithoutGap
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.dsl.builder.Panel
import com.quetwo.intellilucee.CFMLIcon
import javax.swing.Icon

class CFMLGeneratorNewProjectWizard : GeneratorNewProjectWizard
{
    override val id: String = "cfml.application"
    override val name: String = "ColdFusion Application"
    override val icon: Icon = CFMLIcon.FILE

    override fun createStep(context: WizardContext): NewProjectWizardStep =
        RootNewProjectWizardStep(context)
            .nextStep(::newProjectWizardBaseStepWithoutGap)
            .nextStep(::GitNewProjectWizardStep)
            .nextStep(::CFMLStep)

    private class CFMLStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent)
    {
        private val generator = CFMLProjectGenerator()
        private val peer = generator.createPeer()

        override fun setupUI(builder: Panel)
        {
            val locationField = TextFieldWithBrowseButton()
            builder.row {
                cell(peer.getComponent(locationField) {})
            }
        }

        override fun setupProject(project: Project)
        {
            val projectDir = context.projectFileDirectory
            val baseDir = LocalFileSystem.getInstance()
                .refreshAndFindFileByPath(FileUtilRt.toSystemIndependentName(projectDir)) ?: return

            generator.generateProject(project, baseDir, peer.settings, null)
        }
    }
}
