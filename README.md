# IntelliLucee

IntelliLucee is a plugin for IntelliJ Platform based IDEs (IntelliJ IDEA, WebStorm, PHPStorm, etc.)  It provides
the IDE with the ability to process ColdFusion Markup Language based files used by Lucee, Adobe ColdFusion and 
BoxLang. It provides modern IDE functionality using a combination of built-in code and functionality sourced from
the [CFMLEditor-LSP project](https://github.com/cfmleditor/cfmleditor-lsp).

This plugin targets the "Community" or free version of IntelliJ and should have no dependencies on licensed features.
Licensed features of the IDE may add additional tooling for the end user, such as AI support that may be useful.

The project is written in a combination of Kotlin and Java.  It requires Gradle and the IntelliJ SDK to compile. 
Downstream projects, like the CFMLEditor-LSP and TextMate grammar processor utilize other languages such as Go.

## Overview

This repository implements the IntelliLucee IntelliJ Platform plugin that provides support for ColdFusion Markup
support, including .CFML, .CFM, .CFC and .CFS files.  Lucee engine is supported first, with support also provided
for Adobe ColdFusion and BoxLang runtimes.

This plugin is written in Kotlin and Java and will require the IntelliJ IDE to compile, along with Gradle. 


## Build script

The [build.gradle.kts][file:build.gradle.kts] is the core of the project definition. It applies three Gradle plugins:

| Plugin                            | Description                                                                      |
|-----------------------------------|----------------------------------------------------------------------------------|
| `org.jetbrains.kotlin.jvm`        | Adds Kotlin support                                                              |
| `org.jetbrains.changelog`         | Simplifies patching the [CHANGELOG.md][file:CHANGELOG.md] file                   |
| `org.jetbrains.intellij.platform` | The [IntelliJ Platform Gradle Plugin][docs:intellij-platform-gradle-plugin-docs] |

The `intellijPlatform` dependencies block selects the IDE to compile against:

```kotlin
intellijIdea("2026.2.1")
```

See [Target Versions][docs:target-version] for more information.

The `intellijPlatform` dependencies block also contains a dependency on the platform testing framework:

```kotlin
testFramework(TestFrameworkType.Platform)
```
See [Testing][docs:testing] for more information


## Predefined Run/Debug configurations

Within the default project structure, there is a `.run` directory provided containing predefined *Run/Debug
configurations* that expose corresponding Gradle tasks:

| Configuration name  | Description                                                                                                                                                                           |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Run IDE with Plugin | Runs [`:runIde`][docs:intellij-platform-gradle-plugin-runIde] IntelliJ Platform Gradle Plugin task. Use the *Debug* icon for plugin debugging.                                        |
| Run Tests           | Runs [`:check`][gradle:lifecycle-tasks] Gradle task.                                                                                                                                  |
| Run Verifications   | Runs [`:verifyPlugin`][docs:intellij-platform-gradle-plugin-verifyPlugin] IntelliJ Platform Gradle Plugin task to check the plugin compatibility against the specified IntelliJ IDEs. |

> [!NOTE]
> You can find the logs from the running task in the `idea.log` tab.
 |

### GitHub issue templates

The project includes GitHub issue templates:

- [Bug Report](.github/ISSUE_TEMPLATE/bug-report.yml)
- [Feature Request](.github/ISSUE_TEMPLATE/feature-request.yml)

### Dependabot

[Dependabot configuration](.github/dependabot.yml) file enables tracking outdated or vulnerable dependencies.

## Useful links

- [CFMLEditor-LSP Project](https://github.com/cfmleditor/cfmleditor-lsp)
- [CFML-TreeSitter Project](https://github.com/cfmleditor/tree-sitter-cfml)
- [Lucee Runtime Engine](https://lucee.org/)
- [IntelliJ Platform SDK Plugin SDK][docs]
- [IntelliJ Platform Explorer][jb:ipe]
- [IntelliJ SDK Code Samples][gh:code-samples]

[docs]: https://plugins.jetbrains.com/docs/intellij
[docs:plugin.xml]: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html?from=IJPluginReadmeFile
[docs:publishing]: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginReadmeFile
[docs:intellij-platform-gradle-plugin-docs]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html?from=IJPluginReadmeFile
[docs:intellij-platform-gradle-plugin-runIde]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html?from=IJPluginReadmeFile#runIde
[docs:intellij-platform-gradle-plugin-verifyPlugin]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html?from=IJPluginReadmeFile#verifyPlugin
[docs:logo]: https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html?from=IJPluginReadmeFile
[docs:target-version]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html?from=IJPluginReadmeFile#target-versions
[docs:testing]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html?from=IJPluginReadmeFile#testing

[file:build.gradle.kts]: ./build.gradle.kts
[file:CHANGELOG.md]: ./CHANGELOG.md
[file:gradle.properties]: ./gradle.properties
[file:plugin.xml]: ./src/main/resources/META-INF/plugin.xml

[gh:code-samples]: https://github.com/JetBrains/intellij-sdk-code-samples

[gradle:lifecycle-tasks]: https://docs.gradle.org/current/userguide/java_plugin.html#lifecycle_tasks

[jb:github]: https://github.com/JetBrains/.github/blob/main/profile/README.md
[jb:forum]: https://platform.jetbrains.com/
[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html
[jb:paid-plugins]: https://plugins.jetbrains.com/docs/marketplace/paid-plugins-marketplace.html
[jb:ipe]: https://jb.gg/ipe
[jb:ui-guidelines]: https://jetbrains.github.io/ui
