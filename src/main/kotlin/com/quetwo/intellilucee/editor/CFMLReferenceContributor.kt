package com.quetwo.intellilucee.editor

import com.intellij.openapi.util.TextRange
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
                    val calls = model.functionCalls
                    var callIdx = binarySearchFirstCallAfter(calls, elementRange.startOffset)
                    while (callIdx < calls.size)
                    {
                        val call = calls[callIdx]
                        if (call.range.startOffset > elementRange.endOffset) break
                        if (elementRange.contains(call.range))
                        {
                            val relativeRange = call.range.shiftRight(-elementRange.startOffset)
                            refs.add(CFMLPsiReference(element, relativeRange))
                        }
                        callIdx++
                    }

                    // Find variable usages within this element
                    val usages = model.variableUsages
                    var usageIdx = binarySearchFirstUsageAfter(usages, elementRange.startOffset)
                    while (usageIdx < usages.size)
                    {
                        val usage = usages[usageIdx]
                        if (usage.range.startOffset > elementRange.endOffset && usage.nameRange.startOffset > elementRange.endOffset) break
                        if (elementRange.contains(usage.nameRange))
                        {
                            val relativeRange = usage.nameRange.shiftRight(-elementRange.startOffset)
                            refs.add(CFMLPsiReference(element, relativeRange))
                        }
                        else if (elementRange.contains(usage.range))
                        {
                            val relativeRange = usage.range.shiftRight(-elementRange.startOffset)
                            refs.add(CFMLPsiReference(element, relativeRange))
                        }
                        else if (usage.range.contains(elementRange))
                        {
                            val relativeRange = TextRange(0, elementRange.length)
                            refs.add(CFMLPsiReference(element, relativeRange))
                        }
                        usageIdx++
                    }

                    return refs.toTypedArray()
                }
            }
        )
    }

    companion object
    {
        private fun binarySearchFirstCallAfter(calls: List<com.quetwo.intellilucee.model.CFMLFunctionCall>, startOffset: Int): Int
        {
            var low = 0
            var high = calls.size - 1
            var result = calls.size
            while (low <= high)
            {
                val mid = (low + high) ushr 1
                if (calls[mid].range.endOffset >= startOffset)
                {
                    result = mid
                    high = mid - 1
                }
                else
                {
                    low = mid + 1
                }
            }
            return result
        }

        private fun binarySearchFirstUsageAfter(usages: List<com.quetwo.intellilucee.model.CFMLVariableUsage>, startOffset: Int): Int
        {
            var low = 0
            var high = usages.size - 1
            var result = usages.size
            while (low <= high)
            {
                val mid = (low + high) ushr 1
                val u = usages[mid]
                val maxEnd = maxOf(u.range.endOffset, u.nameRange.endOffset)
                if (maxEnd >= startOffset)
                {
                    result = mid
                    high = mid - 1
                }
                else
                {
                    low = mid + 1
                }
            }
            return result
        }
    }
}
