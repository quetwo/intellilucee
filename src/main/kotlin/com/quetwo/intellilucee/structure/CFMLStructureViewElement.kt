package com.quetwo.intellilucee.structure

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.FakePsiElement
import com.quetwo.intellilucee.CFMLIcon
import com.quetwo.intellilucee.model.CFMLAccessType
import com.quetwo.intellilucee.model.CFMLDocumentModel
import com.quetwo.intellilucee.model.CFMLFunctionDeclaration
import com.quetwo.intellilucee.model.CFMLModelParser
import com.quetwo.intellilucee.psi.CFMLPsiFile
import java.util.*
import java.util.regex.Pattern
import javax.swing.Icon

/**
 * Tag info parsed from document text.
 */
data class ParsedTagNode(
    val name: String,
    val rawName: String,
    val isClosing: Boolean,
    val isSelfClosing: Boolean,
    val isCfTag: Boolean,
    val attributes: Map<String, String>,
    val range: TextRange,
    val tagHeaderRange: TextRange,
    val children: MutableList<ParsedTagNode> = mutableListOf(),
    val functions: MutableList<CFMLFunctionDeclaration> = mutableListOf()
)

/**
 * A synthetic PsiElement that represents a node in the structure tree (Tag, Function, Script Block, etc.)
 */
class CFMLStructurePsiElement(
    private val containingPsiFile: PsiFile,
    private val myPresentableText: String,
    val locationText: String?,
    val icon: Icon?,
    private val textRange: TextRange,
    private val nameOffset: Int,
    val isLeaf: Boolean,
    val childrenSupplier: () -> List<CFMLStructurePsiElement> = { emptyList() }
) : FakePsiElement() {

    override fun getParent(): PsiElement = containingPsiFile

    override fun getContainingFile(): PsiFile = containingPsiFile

    override fun isValid(): Boolean = containingPsiFile.isValid

    override fun getTextRange(): TextRange = textRange

    override fun getTextOffset(): Int = nameOffset

    override fun getName(): String = myPresentableText

    override fun getPresentableText(): String = myPresentableText

    override fun canNavigate(): Boolean = true

    override fun canNavigateToSource(): Boolean = true

    override fun navigate(requestFocus: Boolean) {
        val file = containingPsiFile.virtualFile ?: return
        com.intellij.openapi.fileEditor.OpenFileDescriptor(
            project,
            file,
            nameOffset
        ).navigate(requestFocus)
    }
}

/**
 * TreeElement for the Structure View.
 */
class CFMLStructureViewElement(
    private val element: PsiElement
) : StructureViewTreeElement {

    override fun getValue(): Any = element

    override fun navigate(requestFocus: Boolean) {
        if (element is com.intellij.pom.Navigatable && element.canNavigate()) {
            element.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean {
        return element is com.intellij.pom.Navigatable && element.canNavigate()
    }

    override fun canNavigateToSource(): Boolean {
        return element is com.intellij.pom.Navigatable && element.canNavigateToSource()
    }

    override fun getPresentation(): ItemPresentation {
        if (element is CFMLStructurePsiElement) {
            return object : ItemPresentation {
                override fun getPresentableText(): String = element.presentableText
                override fun getLocationString(): String? = element.locationText
                override fun getIcon(unused: Boolean): Icon? = element.icon
            }
        }
        if (element is PsiFile) {
            return object : ItemPresentation {
                override fun getPresentableText(): String = element.name
                override fun getLocationString(): String? = null
                override fun getIcon(unused: Boolean): Icon = element.getIcon(0) ?: CFMLIcon.FILE_CFM
            }
        }
        return object : ItemPresentation {
            override fun getPresentableText(): String = element.text
            override fun getLocationString(): String? = null
            override fun getIcon(unused: Boolean): Icon? = null
        }
    }

    override fun getChildren(): Array<TreeElement> {
        if (element is PsiFile) {
            val rootNodes = CFMLStructureTreeBuilder.buildTree(element)
            return rootNodes.map { CFMLStructureViewElement(it) }.toTypedArray()
        }
        if (element is CFMLStructurePsiElement) {
            val children = element.childrenSupplier()
            return children.map { CFMLStructureViewElement(it) }.toTypedArray()
        }
        return emptyArray()
    }
}

object CFMLStructureTreeBuilder {

    private val HTML_VOID_TAGS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr"
    )

    private val CFML_VOID_TAGS = setOf(
        "cfset", "cfparam", "cfinclude", "cfabort", "cfreturn", "cfbreak", "cfcontinue",
        "cfargument", "cfproperty", "cfdump", "cfheader", "cflocation", "cfthrow", "cfrethrow",
        "cfdirectory", "cffile", "cfmailparam", "cfhttpparam", "cfprocparam", "cfprocresult", "cfqueryparam",
        "cfpop", "cffeed", "cfftp", "cfimage", "cfcontent", "cfcookie", "cfsetting",
        "cfassociate", "cfregistry", "cfschedule", "cfcollection", "cfindex", "cfsearch",
        "cfchartdata", "cfapplication"
    )

    fun buildTree(file: PsiFile): List<CFMLStructurePsiElement> {
        val text = file.text
        if (text.isEmpty()) return emptyList()

        val model = CFMLModelParser.parse(text)
        val tagForest = parseTagForest(text, model)

        return tagForest.map { createPsiElementForTag(file, it) }
    }

    private fun parseTagForest(text: String, model: CFMLDocumentModel): List<ParsedTagNode> {
        val tags = scanTagsWithAttributes(text)
        val roots = mutableListOf<ParsedTagNode>()
        val stack = ArrayDeque<ParsedTagNode>()

        for (tag in tags) {
            if (tag.isClosing) {
                // Pop stack until matching tag found
                var found = false
                val tempStack = ArrayDeque<ParsedTagNode>()
                while (stack.isNotEmpty()) {
                    val top = stack.pop()
                    if (top.name.equals(tag.name, ignoreCase = true)) {
                        found = true
                        val fullRange = TextRange.create(top.range.startOffset, tag.range.endOffset)
                        val closedTag = top.copy(range = fullRange)
                        if (stack.isNotEmpty()) {
                            stack.peek().children.add(closedTag)
                        } else {
                            roots.add(closedTag)
                        }
                        break
                    } else {
                        tempStack.push(top)
                    }
                }
                if (!found) {
                    // Tag had no matching open, restore stack and treat temp popped as unclosed in their parents
                    while (tempStack.isNotEmpty()) {
                        val unclosed = tempStack.pop()
                        if (stack.isNotEmpty()) {
                            stack.peek().children.add(unclosed)
                        } else {
                            roots.add(unclosed)
                        }
                    }
                }
            } else if (tag.isSelfClosing || isVoidTag(tag.name, tag.isCfTag)) {
                if (stack.isNotEmpty()) {
                    stack.peek().children.add(tag)
                } else {
                    roots.add(tag)
                }
            } else {
                stack.push(tag)
            }
        }

        // Remaining unclosed tags on stack
        val unclosedList = mutableListOf<ParsedTagNode>()
        while (stack.isNotEmpty()) {
            unclosedList.add(0, stack.pop())
        }
        for (unclosed in unclosedList) {
            roots.add(unclosed)
        }

        // Now attach functions from the model into the appropriate tags or root
        if (model.functions.isNotEmpty()) {
            distributeFunctions(roots, model.functions)
        }

        return roots
    }

    private fun distributeFunctions(roots: MutableList<ParsedTagNode>, functions: List<CFMLFunctionDeclaration>) {
        for (func in functions) {
            val assigned = assignFunctionToTag(roots, func)
            if (!assigned) {
                // If not enclosed in any tag, create a synthetic container tag or attach
                val funcNode = ParsedTagNode(
                    name = "function",
                    rawName = func.name,
                    isClosing = false,
                    isSelfClosing = true,
                    isCfTag = false,
                    attributes = emptyMap(),
                    range = func.range,
                    tagHeaderRange = func.nameRange
                )
                funcNode.functions.add(func)
                roots.add(funcNode)
            }
        }
    }

    private fun assignFunctionToTag(nodes: List<ParsedTagNode>, func: CFMLFunctionDeclaration): Boolean {
        for (node in nodes) {
            if (node.range.contains(func.range)) {
                // Try children first
                val inChild = assignFunctionToTag(node.children, func)
                if (!inChild) {
                    node.functions.add(func)
                }
                return true
            }
        }
        return false
    }

    private fun isVoidTag(name: String, isCfTag: Boolean): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return if (isCfTag) {
            CFML_VOID_TAGS.contains(lower)
        } else {
            HTML_VOID_TAGS.contains(lower)
        }
    }

    private fun scanTagsWithAttributes(text: String): List<ParsedTagNode> {
        val list = mutableListOf<ParsedTagNode>()
        val len = text.length
        var i = 0

        while (i < len) {
            // Comments
            if (i + 4 < len && text.startsWith("<!---", i)) {
                var depth = 1
                i += 5
                while (i < len && depth > 0) {
                    if (i + 4 < len && text.startsWith("<!---", i)) {
                        depth++
                        i += 5
                    } else if (i + 3 < len && text.startsWith("--->", i)) {
                        depth--
                        i += 4
                    } else {
                        i++
                    }
                }
                continue
            }
            if (i + 3 < len && text.startsWith("<!--", i)) {
                val end = text.indexOf("-->", i + 4)
                i = if (end >= 0) end + 3 else len
                continue
            }
            if (i + 1 < len && text.charAt(i) == '/' && text.charAt(i + 1) == '*') {
                val end = text.indexOf("*/", i + 2)
                i = if (end >= 0) end + 2 else len
                continue
            }
            if (i + 1 < len && text.charAt(i) == '/' && text.charAt(i + 1) == '/') {
                val end = text.indexOf('\n', i + 2)
                i = if (end >= 0) end + 1 else len
                continue
            }

            if (text.charAt(i) == '<') {
                val tagStart = i
                var cursor = i + 1
                var isClosing = false
                if (cursor < len && text.charAt(cursor) == '/') {
                    isClosing = true
                    cursor++
                }

                while (cursor < len && Character.isWhitespace(text.charAt(cursor))) {
                    cursor++
                }

                val nameStart = cursor
                while (cursor < len && isTagIdentifierChar(text.charAt(cursor))) {
                    cursor++
                }

                if (cursor > nameStart) {
                    val rawName = text.substring(nameStart, cursor)
                    val isCfTag = rawName.regionMatches(0, "cf", 0, 2, ignoreCase = true)
                    var isSelfClosing = false
                    val attributes = mutableMapOf<String, String>()

                    // Parse attributes until '>'
                    val tagHeaderStart = tagStart
                    while (cursor < len) {
                        while (cursor < len && Character.isWhitespace(text.charAt(cursor))) {
                            cursor++
                        }
                        if (cursor >= len) break
                        val c = text.charAt(cursor)
                        if (c == '>') {
                            if (cursor > tagStart && text.charAt(cursor - 1) == '/') {
                                isSelfClosing = true
                            }
                            cursor++
                            break
                        }
                        if (c == '/' && cursor + 1 < len && text.charAt(cursor + 1) == '>') {
                            isSelfClosing = true
                            cursor += 2
                            break
                        }

                        // Attribute name
                        val attrNameStart = cursor
                        while (cursor < len && isAttributeNameChar(text.charAt(cursor))) {
                            cursor++
                        }
                        if (cursor > attrNameStart) {
                            val attrName = text.substring(attrNameStart, cursor)
                            while (cursor < len && Character.isWhitespace(text.charAt(cursor))) {
                                cursor++
                            }
                            var attrValue = ""
                            if (cursor < len && text.charAt(cursor) == '=') {
                                cursor++
                                while (cursor < len && Character.isWhitespace(text.charAt(cursor))) {
                                    cursor++
                                }
                                if (cursor < len) {
                                    val qc = text.charAt(cursor)
                                    if (qc == '"' || qc == '\'') {
                                        cursor++
                                        val valStart = cursor
                                        while (cursor < len) {
                                            if (text.charAt(cursor) == qc) {
                                                if (cursor + 1 < len && text.charAt(cursor + 1) == qc) {
                                                    cursor += 2
                                                } else {
                                                    attrValue = text.substring(valStart, cursor)
                                                    cursor++
                                                    break
                                                }
                                            } else {
                                                cursor++
                                            }
                                        }
                                    } else {
                                        val valStart = cursor
                                        while (cursor < len && !Character.isWhitespace(text.charAt(cursor)) && text.charAt(cursor) != '>') {
                                            cursor++
                                        }
                                        attrValue = text.substring(valStart, cursor)
                                    }
                                }
                            }
                            attributes[attrName.lowercase(Locale.ROOT)] = attrValue
                        } else {
                            cursor++
                        }
                    }

                    val tagHeaderRange = TextRange.create(tagHeaderStart, cursor)
                    val tagRange = TextRange.create(tagStart, cursor)

                    list.add(
                        ParsedTagNode(
                            name = rawName.lowercase(Locale.ROOT),
                            rawName = rawName,
                            isClosing = isClosing,
                            isSelfClosing = isSelfClosing,
                            isCfTag = isCfTag,
                            attributes = attributes,
                            range = tagRange,
                            tagHeaderRange = tagHeaderRange
                        )
                    )
                    i = cursor
                    continue
                }
            }
            i++
        }
        return list
    }

    private fun isTagIdentifierChar(c: Char): Boolean =
        Character.isLetterOrDigit(c) || c == '_' || c == ':' || c == '-'

    private fun isAttributeNameChar(c: Char): Boolean =
        Character.isLetterOrDigit(c) || c == '_' || c == ':' || c == '-' || c == '.'

    private fun createPsiElementForTag(file: PsiFile, node: ParsedTagNode): CFMLStructurePsiElement {
        val (text, loc, icon) = computeTagPresentation(node)
        val nameOffset = node.tagHeaderRange.startOffset
        val isLeaf = node.children.isEmpty() && node.functions.isEmpty()

        return CFMLStructurePsiElement(
            containingPsiFile = file,
            myPresentableText = text,
            locationText = loc,
            icon = icon,
            textRange = node.range,
            nameOffset = nameOffset,
            isLeaf = isLeaf,
            childrenSupplier = {
                val result = mutableListOf<CFMLStructurePsiElement>()
                // Functions inside this tag (e.g. inside <cfcomponent> or <cfscript>)
                for (func in node.functions) {
                    result.add(createPsiElementForFunction(file, func))
                }
                for (child in node.children) {
                    result.add(createPsiElementForTag(file, child))
                }
                result
            }
        )
    }

    private fun createPsiElementForFunction(file: PsiFile, func: CFMLFunctionDeclaration): CFMLStructurePsiElement {
        val icon = when (func.access) {
            CFMLAccessType.PUBLIC -> CFMLIcon.FUNCTION_PUBLIC
            CFMLAccessType.PRIVATE -> CFMLIcon.FUNCTION_PRIVATE
            CFMLAccessType.REMOTE -> CFMLIcon.FUNCTION_REMOTE
            CFMLAccessType.PACKAGE -> CFMLIcon.FUNCTION_PUBLIC
        }
        val params = func.parameters.joinToString(", ") { it.name }
        val presentable = "${func.name}($params)"
        return CFMLStructurePsiElement(
            containingPsiFile = file,
            myPresentableText = presentable,
            locationText = func.access.name.lowercase(Locale.ROOT),
            icon = icon,
            textRange = func.range,
            nameOffset = func.nameRange.startOffset,
            isLeaf = true,
            childrenSupplier = { emptyList() }
        )
    }

    private fun computeTagPresentation(node: ParsedTagNode): Triple<String, String?, Icon?> {
        val rawName = node.rawName
        val lower = node.name

        if (node.isCfTag) {
            return when (lower) {
                "cffunction" -> {
                    val fnName = node.attributes["name"] ?: "anonymous"
                    val access = node.attributes["access"] ?: "public"
                    val returnType = node.attributes["returntype"] ?: ""
                    val icon = when (access.lowercase(Locale.ROOT)) {
                        "private" -> CFMLIcon.FUNCTION_PRIVATE
                        "remote" -> CFMLIcon.FUNCTION_REMOTE
                        else -> CFMLIcon.FUNCTION_PUBLIC
                    }
                    val loc = if (returnType.isNotEmpty()) ": $returnType" else null
                    Triple("$rawName $fnName()", loc, icon)
                }
                "cfquery" -> {
                    val qName = node.attributes["name"] ?: ""
                    val loc = if (qName.isNotEmpty()) qName else null
                    Triple(rawName, loc, CFMLIcon.FILE_CFM)
                }
                "cfset" -> {
                    val label = if (node.attributes.isNotEmpty()) {
                        val firstAttr = node.attributes.keys.first()
                        val v = node.attributes[firstAttr]
                        if (v.isNullOrEmpty()) firstAttr else "$firstAttr = $v"
                    } else null
                    Triple(rawName, label, CFMLIcon.FILE_CFM)
                }
                "cfparam" -> {
                    val name = node.attributes["name"] ?: ""
                    val defaultVal = node.attributes["default"]
                    val loc = if (defaultVal != null) "$name = $defaultVal" else name
                    Triple(rawName, loc.ifEmpty { null }, CFMLIcon.FILE_CFM)
                }
                "cfinclude" -> {
                    val template = node.attributes["template"] ?: ""
                    Triple(rawName, template.ifEmpty { null }, CFMLIcon.FILE_CFM)
                }
                "cfcomponent" -> {
                    val ext = node.attributes["extends"]
                    val loc = if (ext != null) "extends $ext" else null
                    Triple(rawName, loc, CFMLIcon.FILE_CFC)
                }
                else -> {
                    val nameAttr = node.attributes["name"] ?: node.attributes["id"]
                    Triple(rawName, nameAttr, CFMLIcon.FILE_CFM)
                }
            }
        }

        // HTML tag presentation
        val id = node.attributes["id"]
        val cls = node.attributes["class"]
        val type = node.attributes["type"]
        val name = node.attributes["name"]

        val label = buildString {
            append(rawName)
            if (!id.isNullOrEmpty()) {
                append("#$id")
            }
            if (!cls.isNullOrEmpty()) {
                val firstClass = cls.trim().split(Regex("\\s+")).firstOrNull()
                if (!firstClass.isNullOrEmpty()) {
                    append(".$firstClass")
                }
            }
        }

        val extraLoc = when {
            !name.isNullOrEmpty() -> "name=\"$name\""
            !type.isNullOrEmpty() -> "type=\"$type\""
            else -> null
        }

        return Triple(label, extraLoc, CFMLIcon.FILE_CFM)
    }
}

private fun String.charAt(index: Int): Char = this[index]
