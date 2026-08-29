package com.quetwo.intellilucee.settings

data class CFMLFormatterModuleSettingsState(
    var overrideApplicationSettings: Boolean = false,
    var formatterEnabled: Boolean = true,
    var ignoreWhiteSpaceInFormatter: Boolean = false,
    var formatWithinQueryTags: Boolean = true,
    var updateCfTagsToLowercase: Boolean = true,
    var selfCloseHtmlTags: Boolean = false,
    var rejectFormattingChangesToNonWhitespaceContent: Boolean = false,
    var updateAttributesToLowercase: Boolean = false,
    var normalizeAttributeValuesToDoubleQuotes: Boolean = false,
    var normalizeSqlKeywordsToUppercase: Boolean = false,
    var normalizeCfmlScopeNames: String? = "leave",
    var commaPlacementInMultilineArgumentLists: String? = "after",
    var numberOfAttributesPerLine: Int = 1,
    var lineWidth: Int = 120,
)
