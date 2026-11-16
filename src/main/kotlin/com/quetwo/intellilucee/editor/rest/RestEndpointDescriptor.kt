package com.quetwo.intellilucee.editor.rest

data class RestArgument(
    var argumentType: String = "string",
    var argumentName: String = "",
    var argumentSource: String = "Path"
)

data class RestEndpointDescriptor(
    val functionName: String,
    val httpMethod: String,
    val restPath: String = "",
    val produces: String = "application/json",
    val consumes: String = "",
    val arguments: List<RestArgument> = emptyList()
)
