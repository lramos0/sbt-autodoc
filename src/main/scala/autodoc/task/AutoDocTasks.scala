package autodoc.task

import autodoc.config.DocumentationRootResolver
import autodoc.keys.AutoDocKeys
import autodoc.render.MarkdownRenderer
import autodoc.util.FileUtils
import sbt.Keys._
import sbt._

import scala.sys.process._

object AutoDocTasks {
  import AutoDocKeys._

  def settings: Seq[Setting[_]] = Seq(
    autoDocDocumentationRepoUrl := None,
    autoDocLocalDocumentationRoot := None,
    autoDocDocumentationRef := "main",
    autoDocDocumentationConfigPath := "autodoc/config.json",
    // One clone per build (root project base, not each module's target/).
    autoDocDocumentationCacheDirectory :=
      (LocalRootProject / baseDirectory).value / "target" / "autodoc" / "documentation-repo",
    autoDocGitDiffScope := "branch",
    autoDocGitBranchBase := "origin/main",
    autoDocGitDiffSpec := None,
    autoDocServiceId := None,
    autoDocPerSubproject := false,
    autoDocOutputFile := {
      val t =
        if (autoDocPerSubproject.value) target.value
        else (LocalRootProject / target).value
      t / "autodoc" / "autodoc.md"
    },
    autoDocElaborationProvider := "none",
    autoDocElaborationMode := "handoff",
    autoDocElaborationPromptFile := {
      val t =
        if (autoDocPerSubproject.value) target.value
        else (LocalRootProject / target).value
      t / "autodoc" / "elaboration-prompt.md"
    },
    autoDocElaborationOutputFile := {
      val t =
        if (autoDocPerSubproject.value) target.value
        else (LocalRootProject / target).value
      t / "autodoc" / "autodoc-elaborated.md"
    },
    autoDocElaborationAudience := "engineering",
    autoDocElaborationTone := "concise",
    autoDocElaborationCustomPrompt := None,
    autoDocElaborationCommand := None,
    autoDocElaborationClaudeCodeExecutable := "claude",
    autoDocElaborationClaudeCodeArgs := Seq.empty,
    autoDocElaborationCursorCliExecutable := "agent",
    autoDocElaborationCursorCliArgs := Seq.empty,
    autoDocElaborationMermaidDiagrams := "ask",
    autoDoc := {
      val log = streams.value.log
      val out = autoDocOutputFile.value
      val per = autoDocPerSubproject.value
      val rootBase = (LocalRootProject / baseDirectory).value.getCanonicalFile
      val isRoot = baseDirectory.value.getCanonicalFile == rootBase
      if (!per && !isRoot) {
        log.info(
          "sbt-autodoc: skipping autoDoc on nested project (single-repo mode). " +
            "Run `autoDoc` on the root project, or set autoDocPerSubproject := true for per-module outputs.",
        )
        out
      } else {
        val scopeBase =
          if (per) baseDirectory.value
          else (LocalRootProject / baseDirectory).value
        val gitDiff =
          autoDocGitDiffSpec.value.orElse {
            if (autoDocGitDiffScope.value.equalsIgnoreCase("branch"))
              Some(s"${autoDocGitBranchBase.value}...HEAD")
            else None
          }
        AutoDocRunner
          .run(
            log = log,
            baseDirectory = scopeBase,
            documentationRepoUrl = autoDocDocumentationRepoUrl.value,
            localDocumentationRoot = autoDocLocalDocumentationRoot.value,
            documentationRef = autoDocDocumentationRef.value,
            documentationConfigRelativePath = autoDocDocumentationConfigPath.value,
            documentationCacheDirectory = autoDocDocumentationCacheDirectory.value,
            gitDiffSpec = gitDiff,
            serviceIdOverride = autoDocServiceId.value,
            outputFile = out,
            loader = classOf[MarkdownRenderer].getClassLoader,
          )
          .fold(
            msg => sys.error(msg),
            file => {
              log.success(s"sbt-autodoc: wrote ${file.getAbsolutePath}")
              file
            },
          )
      }
    },
    autoDocElaborate := {
      val log = streams.value.log
      val interaction = interactionService.value
      val per = autoDocPerSubproject.value
      val rootBase = (LocalRootProject / baseDirectory).value.getCanonicalFile
      val isRoot = baseDirectory.value.getCanonicalFile == rootBase
      val _ = autoDoc.value
      if (!per && !isRoot) {
        log.info(
          "sbt-autodoc: skipping autoDocElaborate on nested project (single-repo mode). " +
            "Run on the root project, or set autoDocPerSubproject := true.",
        )
        Seq.empty
      } else {
        val elaborationWorkDir =
          if (per) baseDirectory.value
          else (LocalRootProject / baseDirectory).value
        val provider = autoDocElaborationProvider.value.trim
        if (provider.equalsIgnoreCase("none")) {
          log.info("sbt-autodoc: autoDocElaborationProvider is none; skipping autoDocElaborate")
          Seq.empty
        } else {
        val mode = autoDocElaborationMode.value.trim.toLowerCase
        val input = autoDocOutputFile.value
        val promptFile = autoDocElaborationPromptFile.value
        val outputFile = autoDocElaborationOutputFile.value
        val audience = autoDocElaborationAudience.value
        val tone = autoDocElaborationTone.value
        val custom = autoDocElaborationCustomPrompt.value
        val customBlock =
          custom.map(c => s"\n## Additional instructions\n$c\n").getOrElse("")
        val diagramBlock = {
          val docRoot = DocumentationRootResolver.resolve(
            log,
            autoDocDocumentationRepoUrl.value,
            autoDocLocalDocumentationRoot.value,
            autoDocDocumentationRef.value,
            autoDocDocumentationCacheDirectory.value,
          )
          docRoot match {
            case Left(msg) =>
              log.debug(s"sbt-autodoc: documentation repo not resolved for .mmd scan: $msg")
              ""
            case Right(root) =>
              val mmds = MermaidDiagramIndex.findMmdFiles(root)
              if (mmds.isEmpty) ""
              else if (
                ElaborationDiagramPolicy.shouldInclude(
                  autoDocElaborationMermaidDiagrams.value,
                  mmds.size,
                  log,
                  interaction,
                )
              ) {
                log.info(
                  s"sbt-autodoc: adding Mermaid diagram hints (${mmds.size} .mmd file(s)) to elaboration prompt",
                )
                MermaidDiagramIndex.formatDiagramSection(mmds, root)
              }
              else ""
          }
        }
        val promptBody =
          s"""# Autodoc AI elaboration ($provider)
             |
             |Read the generated autodoc markdown at:
             |`${input.getAbsolutePath}`
             |
             |Write elaborated documentation to:
             |`${outputFile.getAbsolutePath}`
             |
             |- **Audience**: $audience
             |- **Tone**: $tone
             |$customBlock$diagramBlock
             |""".stripMargin
        val promptForDisk =
          if (provider.equalsIgnoreCase("claude-code"))
            ClaudeCodeElaboration.buildClaudePrompt(promptBody)
          else if (provider.equalsIgnoreCase("cursor-cli"))
            CursorCliElaboration.buildCursorPrompt(promptBody)
          else promptBody
        FileUtils.writeUtf8(promptFile, promptForDisk)
        log.info(s"sbt-autodoc: wrote elaboration prompt ${promptFile.getAbsolutePath}")
        mode match {
          case "execute" =>
            autoDocElaborationCommand.value match {
              case Some(template) =>
                val expanded = template
                  .replace("{input}", input.getAbsolutePath)
                  .replace("{output}", outputFile.getAbsolutePath)
                  .replace("{prompt}", promptFile.getAbsolutePath)
                val exit = expanded ! ProcessLogger(log.info(_), log.warn(_))
                if (exit != 0)
                  sys.error(s"autoDocElaborationCommand exited with $exit")
                log.success(s"sbt-autodoc: elaboration command finished; see ${outputFile.getAbsolutePath}")
                Seq(outputFile)
              case None =>
                if (provider.equalsIgnoreCase("claude-code")) {
                  FileUtils.ensureParentDir(outputFile)
                  val exit = ClaudeCodeElaboration.run(
                    log = log,
                    workDir = elaborationWorkDir,
                    executable = autoDocElaborationClaudeCodeExecutable.value,
                    promptText = promptForDisk,
                    extraArgs = autoDocElaborationClaudeCodeArgs.value,
                  )
                  if (exit != 0)
                    sys.error(s"Claude Code CLI exited with code $exit")
                  log.success(s"sbt-autodoc: Claude Code elaboration finished; see ${outputFile.getAbsolutePath}")
                  Seq(outputFile)
                } else if (provider.equalsIgnoreCase("cursor-cli")) {
                  FileUtils.ensureParentDir(outputFile)
                  val exit = CursorCliElaboration.run(
                    log = log,
                    workDir = elaborationWorkDir,
                    executable = autoDocElaborationCursorCliExecutable.value,
                    promptText = promptForDisk,
                    extraArgs = autoDocElaborationCursorCliArgs.value,
                  )
                  if (exit != 0)
                    sys.error(s"Cursor CLI (agent) exited with code $exit")
                  log.success(s"sbt-autodoc: Cursor CLI elaboration finished; see ${outputFile.getAbsolutePath}")
                  Seq(outputFile)
                } else
                  sys.error(
                    "autoDocElaborationMode is execute but autoDocElaborationCommand is not set " +
                      "(set a custom command, or use autoDocElaborationProvider := \"claude-code\" or \"cursor-cli\" for built-in CLIs)",
                  )
            }
          case "handoff" =>
            Seq(promptFile)
          case other =>
            sys.error(s"autoDocElaborationMode must be handoff or execute, got: $other")
        }
        }
      }
    },
  )
}
