<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# IntelliLucee Changelog

## [Unreleased]

### Changed
- The CFML language server is now clif (formerly cfmleditor-lsp), downloaded from `cfmleditor/clif`. The `clif` archive is tried first and the `cfmleditor-lsp` one after it, so a pinned release from before the rename still installs, and an executable already downloaded under either name is still used.

### Added
- Path mapping datagrid table in DAP server profile settings supporting multiple path mappings and local directory selection via folder chooser.
- Debug logging at DEBUG level to `idea.log` for all data sent to and received from the DAP server.
- Path mapping configuration (IDE prefix and server prefix) in DAP server settings, transmitted via `pathTransforms` in DAP `attach` requests.
- Remote DAP debugger maintains active socket connection and session lifecycle via process handler instead of immediately disconnecting.
- Remote DAP debugger configuration connects to the DAP server and sends DAP `attach` request with configured secret options.
- Line breakpoints now match default breakpoint variants, preventing inline breakpoint indicators from being displayed in the code editor and eliminating the source variant mismatch error.
- Line breakpoint indicators are now displayed only in the gutter rather than highlighting the line within the editor.
- Fixed support for line breakpoints via `setBreakpoints` DAP requests when connecting to the DAP server.
- Fixed support of comment tags (`<!--- ... --->`) in the commenter and editor. Added tests for commenter.
- Structure view support for .CFM and CFML files including HTML tags parsing and hierarchy
- Generated `.gitignore` for new projects using the project generator.  Won't commit secret or IDE properties.
- Generated `README.md` file when using the project generator with directory structure and setup todos
- Generated sample `index.cfm` entry point with server info output when using the project generator
- Generated `Application.cfc` template with application name, timeouts, and datasource configuration when using the project generator
- Generated database secrets file (`db/secrets.txt`) for Postgres and Microsoft SQL Server when using the project generator
- Generated default datasource configuration in `Application.cfc` for Postgres and MSSQL databases when using the project generator
