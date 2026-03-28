package autodoc

import autodoc.keys.AutoDocKeys

/**
  * Use in consumer `build.sbt` when `AutoPlugin.autoImport` does not resolve keys (e.g. old cached artifact):
  * {{{
  * import autodoc.SbtKeys._
  * }}}
  * Same `SettingKey` instances as `AutoDocPlugin` / `AutoDocKeys`.
  */
object SbtKeys {
  val autoDoc = AutoDocKeys.autoDoc
  val autoDocDocumentationRepoUrl = AutoDocKeys.autoDocDocumentationRepoUrl
  val autoDocLocalDocumentationRoot = AutoDocKeys.autoDocLocalDocumentationRoot
  val autoDocDocumentationRef = AutoDocKeys.autoDocDocumentationRef
  val autoDocDocumentationConfigPath = AutoDocKeys.autoDocDocumentationConfigPath
  val autoDocDocumentationCacheDirectory = AutoDocKeys.autoDocDocumentationCacheDirectory
  val autoDocGitDiffScope = AutoDocKeys.autoDocGitDiffScope
  val autoDocGitBranchBase = AutoDocKeys.autoDocGitBranchBase
  val autoDocGitDiffSpec = AutoDocKeys.autoDocGitDiffSpec
  val autoDocServiceId = AutoDocKeys.autoDocServiceId
  val autoDocPerSubproject = AutoDocKeys.autoDocPerSubproject
  val autoDocOutputFile = AutoDocKeys.autoDocOutputFile
  val autoDocElaborate = AutoDocKeys.autoDocElaborate
  val autoDocElaborationProvider = AutoDocKeys.autoDocElaborationProvider
  val autoDocElaborationMode = AutoDocKeys.autoDocElaborationMode
  val autoDocElaborationPromptFile = AutoDocKeys.autoDocElaborationPromptFile
  val autoDocElaborationOutputFile = AutoDocKeys.autoDocElaborationOutputFile
  val autoDocElaborationAudience = AutoDocKeys.autoDocElaborationAudience
  val autoDocElaborationTone = AutoDocKeys.autoDocElaborationTone
  val autoDocElaborationCustomPrompt = AutoDocKeys.autoDocElaborationCustomPrompt
  val autoDocElaborationCommand = AutoDocKeys.autoDocElaborationCommand
  val autoDocElaborationClaudeCodeExecutable = AutoDocKeys.autoDocElaborationClaudeCodeExecutable
  val autoDocElaborationClaudeCodeArgs = AutoDocKeys.autoDocElaborationClaudeCodeArgs
  val autoDocElaborationCursorCliExecutable = AutoDocKeys.autoDocElaborationCursorCliExecutable
  val autoDocElaborationCursorCliArgs = AutoDocKeys.autoDocElaborationCursorCliArgs
  val autoDocElaborationMermaidDiagrams = AutoDocKeys.autoDocElaborationMermaidDiagrams
}
