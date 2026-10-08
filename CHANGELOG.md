<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# IntelliLucee Changelog

## [Unreleased]

### Changed
- The CFML language server is now clif (formerly cfmleditor-lsp), downloaded from `cfmleditor/clif`. The `clif` archive is tried first and the `cfmleditor-lsp` one after it, so a pinned release from before the rename still installs, and an executable already downloaded under either name is still used.

### Added
- Structure view support for .CFM and CFML files including HTML tags parsing and hierarchy
- Generated `.gitignore` for new projects using the project generator.  Won't commit secret or IDE properties.
- Generated `README.md` file when using the project generator with directory structure and setup todos
