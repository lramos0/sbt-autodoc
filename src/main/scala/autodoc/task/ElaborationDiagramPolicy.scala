package autodoc.task

import sbt.{InteractionService, Logger}

/**
  * Whether to add Mermaid / `.mmd` instructions to the elaboration prompt.
  * - `include` — always add when `.mmd` files exist
  * - `skip` — never add
  * - `ask` — uses sbt [[InteractionService]] (not `System.console`, which blocks under sbt)
  */
object ElaborationDiagramPolicy {

  def shouldInclude(
      policy: String,
      mmdCount: Int,
      log: Logger,
      interaction: InteractionService,
  ): Boolean = {
    if (mmdCount <= 0) return false
    policy.trim.toLowerCase match {
      case "skip" | "no" | "false" => false
      case "include" | "yes" | "true" => true
      case "ask" =>
        val msg =
          s"sbt-autodoc: Found $mmdCount .mmd file(s) in the documentation repo. " +
            "Include diagram-update instructions in the elaboration prompt?"
        // Supershell / JLine: avoid deadlocks and hidden prompts (see sbt#5122)
        System.out.synchronized {
          interaction.confirm(msg)
        }
      case other =>
        log.warn(s"sbt-autodoc: unknown autoDocElaborationMermaidDiagrams value '$other', treating as skip")
        false
    }
  }
}
