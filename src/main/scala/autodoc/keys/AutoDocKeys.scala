package autodoc.keys

import sbt._

trait AutoDocKeys {
  val autoDoc = taskKey[File](
    "Generate markdown from git diff using shared config from ad-service-documentation (or a local clone)",
  )

  /** Git clone URL (e.g. git@github.com:org/ad-service-documentation.git). */
  val autoDocDocumentationRepoUrl =
    settingKey[Option[String]]("Git clone URL for the ad-service-documentation repository")

  /** Use a local checkout instead of cloning (overrides autoDocDocumentationRepoUrl). */
  val autoDocLocalDocumentationRoot =
    settingKey[Option[File]]("Local checkout of ad-service-documentation (skips clone)")

  /** Branch or tag in the documentation repository. */
  val autoDocDocumentationRef = settingKey[String]("Branch or tag to fetch in the documentation repository")

  /** Path to the JSON config file inside the documentation repository. */
  val autoDocDocumentationConfigPath =
    settingKey[String]("Path to autodoc JSON config inside the documentation repo checkout")

  /** Where to clone or update the documentation repository (under target/ by default). */
  val autoDocDocumentationCacheDirectory =
    settingKey[File]("Cache directory for the cloned documentation repository")

  /**
    * `branch` — all commits on the current branch vs merge-base with [[autoDocGitBranchBase]] (`git diff base...HEAD`).
    * `uncommitted` — working tree + index vs `HEAD` only (`git diff HEAD`).
    */
  val autoDocGitDiffScope = settingKey[String]("branch | uncommitted")

  /** Left side of `git diff --name-status <this>...HEAD` when scope is `branch` (three-dot / merge-base). */
  val autoDocGitBranchBase = settingKey[String]("e.g. origin/main")

  /**
    * If set, overrides [[autoDocGitDiffScope]] / [[autoDocGitBranchBase]] and is passed as the sole rev argument
    * to `git diff --name-status` (e.g. `origin/main...HEAD`, `main...HEAD`).
    */
  val autoDocGitDiffSpec =
    settingKey[Option[String]]("Optional full git diff rev expression; overrides scope when set")

  /** Pin service id from config instead of inferring from project path. */
  val autoDocServiceId = settingKey[Option[String]]("Optional service id override (must exist in config)")

  /**
    * If false (default), treat the git checkout as a single logical project: one `autodoc.md` under the build root
    * `target/`, git diff scoped to [[LocalRootProject]] `baseDirectory`, and `autoDoc` / `autoDocElaborate` no-op on
    * nested sbt projects. If true, each subproject gets its own output and scoped diff (legacy behavior).
    */
  val autoDocPerSubproject = settingKey[Boolean]("Per-subproject autodoc output and git scope (default: false)")

  /** Output markdown file (defaults under build root or subproject `target/` depending on [[autoDocPerSubproject]]). */
  val autoDocOutputFile = settingKey[File]("Generated markdown output path")

  /** Elaborate generated docs with an AI provider (runs after [[autoDoc]]). */
  val autoDocElaborate = taskKey[Seq[File]]("Elaborate generated docs with an AI provider")

  /** `none` | `claude-api` | `claude-code` | `cursor-cli` */
  val autoDocElaborationProvider = settingKey[String]("none | claude-api | claude-code | cursor-cli")

  /**
    * `handoff` writes a prompt file only; `execute` runs a provider command. Built-in CLIs when
    * [[autoDocElaborationCommand]] is unset: `claude-code` → `claude --bare -p ...`, `cursor-cli` → `agent -p --force ...`.
    */
  val autoDocElaborationMode = settingKey[String]("handoff | execute")

  /** Path for the generated elaboration prompt (handoff and execute). */
  val autoDocElaborationPromptFile = settingKey[File]("Generated prompt file path")

  /** Path for AI-elaborated markdown when the provider writes to disk. */
  val autoDocElaborationOutputFile = settingKey[File]("AI-elaborated markdown output path")

  val autoDocElaborationAudience = settingKey[String]("Target audience for elaboration")

  val autoDocElaborationTone = settingKey[String]("Writing tone for elaboration")

  val autoDocElaborationCustomPrompt = settingKey[Option[String]]("Extra prompt text")

  /**
    * Optional shell command for CLI providers in `execute` mode.
    * Placeholders: `{input}` (autodoc markdown), `{output}` (elaborated markdown), `{prompt}` (prompt file).
    */
  val autoDocElaborationCommand = settingKey[Option[String]]("Custom command template for CLI providers")

  /** Executable or path for Claude Code when [[autoDocElaborationProvider]] is `claude-code` and command is default. */
  val autoDocElaborationClaudeCodeExecutable = settingKey[String]("Claude Code CLI executable (default: claude)")

  /**
    * Extra arguments inserted after `-p <prompt>` for the default `claude-code` invocation, e.g. `Seq("--model", "sonnet")`.
    */
  val autoDocElaborationClaudeCodeArgs = settingKey[Seq[String]]("Extra Claude Code CLI args for default claude-code execute")

  /** Cursor Agent CLI executable when [[autoDocElaborationProvider]] is `cursor-cli` and command is default (usually `agent`). */
  val autoDocElaborationCursorCliExecutable = settingKey[String]("Cursor CLI agent executable (default: agent)")

  /** Extra args after the prompt for default `cursor-cli` invocation (e.g. pass model via Seq("--model", "gpt-5.2")). */
  val autoDocElaborationCursorCliArgs = settingKey[Seq[String]]("Extra Cursor CLI args for default cursor-cli execute")

  /**
    * When the documentation repo contains `.mmd` files: `ask` uses sbt InteractionService (yes/no), `include` always
    * adds diagram-update instructions to the elaboration prompt, `skip` never does.
    */
  val autoDocElaborationMermaidDiagrams = settingKey[String]("ask | include | skip — elaboration prompt and .mmd files")
}

object AutoDocKeys extends AutoDocKeys
