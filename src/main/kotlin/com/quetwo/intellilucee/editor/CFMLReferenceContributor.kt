package com.quetwo.intellilucee.editor

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.quetwo.intellilucee.psi.CFMLPsiReference
import com.quetwo.intellilucee.psi.CFMLPsiUtil

class CFMLReferenceContributor : PsiReferenceContributor()
{

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar)
    {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiElement::class.java),
            object : PsiReferenceProvider()
            {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference>
                {
                    if (element is PsiFile) return PsiReference.EMPTY_ARRAY
                    val file = element.containingFile ?: return PsiReference.EMPTY_ARRAY
                    if (!CFMLPsiUtil.isCFMLFile(file)) return PsiReference.EMPTY_ARRAY

                    val elementRange = element.textRange
                    val model = CFMLPsiUtil.getModel(file)
                    val refs = mutableListOf<PsiReference>()

                    // Find function calls within this element
                    for (call in model.functionCalls)
                    {
                        if (elementRange.contains(call.range))
                        {
                            val relativeRange = call.range.shiftRight(-elementRange.startOffset)
                            refs.add(CFMLPsiReference(element, relativeRange))
                        }
                    }

                    // Find variable usages within this element
                    for (usage in model.variableUsages)
                    {
                        if (elementRange.contains(usage.range))
                        {
                            val relativeRange = usage.range.shiftRight(-elementRange.startOffset)
                            refs.add(CFMLPsiReference(element, relativeRange))
                        }
                    }

                    return refs.toTypedArray()
                }
            }
        )
    }
}
