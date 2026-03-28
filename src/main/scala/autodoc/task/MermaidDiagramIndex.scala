package autodoc.task

import autodoc.util.FileUtils

import java.io.File

/** Finds `.mmd` (Mermaid source) files under the documentation repository checkout. */
object MermaidDiagramIndex {

  /** Recursive listing; skips `.git` and `target` directories. */
  def findMmdFiles(docRepoRoot: File): Seq[File] = {
    if (!docRepoRoot.isDirectory) return Seq.empty
    def walk(dir: File): List[File] = {
      val kids = Option(dir.listFiles()).getOrElse(Array.empty[File])
      kids.toList.flatMap { f =>
        if (f.isDirectory) {
          val n = f.getName
          if (n == ".git" || n == "target") Nil
          else walk(f)
        } else if (f.isFile && f.getName.endsWith(".mmd")) List(f)
        else Nil
      }
    }
    walk(docRepoRoot).sortBy(_.getAbsolutePath)
  }

  def formatDiagramSection(files: Seq[File], docRoot: File): String = {
    val rootPath = docRoot.getCanonicalFile.toPath
    val lines = files.map { f =>
      val canonical = f.getCanonicalFile
      val rel = FileUtils.posixPathString(rootPath.relativize(canonical.toPath))
      s"- `${canonical.getAbsolutePath}` _(repo-relative: `$rel`)_"
    }.mkString("\n")
    s"""
       |
       |## Mermaid diagrams (.mmd)
       |These files are the **source of truth** for diagrams in the documentation repository. If the autodoc changes imply diagram updates, **edit these paths on disk**—do not relocate diagram text into the elaborated markdown.
       |
       |$lines
       |
       |**Rules**
       |- Do **not** paste Mermaid syntax, fenced code blocks, or full `.mmd` file bodies into the elaborated markdown output file. That output should remain normal prose/summary; diagrams stay in `.mmd`.
       |- Update diagrams by **editing the listed `.mmd` files in place**: keep the same diagram kind (flowchart, sequenceDiagram, etc.) and overall shape when possible; change only labels, nodes, edges, or structure needed to match the code changes.
       |- Prefer a **minimal diff** to each `.mmd`—revise the existing diagram rather than replacing it with an unrelated chart or duplicating it inside markdown.
       |""".stripMargin
  }
}
