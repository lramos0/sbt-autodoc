package autodoc.task

import autodoc.config.{ConfigLoader, DocumentationRootResolver}
import autodoc.detect.{ProjectContextDetector, ProjectScope}
import autodoc.git.GitCliClient
import autodoc.model.DocContext
import autodoc.render.MarkdownRenderer
import autodoc.util.FileUtils

import java.io.File

import sbt.Logger

object AutoDocRunner {
  def run(
      log: Logger,
      baseDirectory: File,
      documentationRepoUrl: Option[String],
      localDocumentationRoot: Option[File],
      documentationRef: String,
      documentationConfigRelativePath: String,
      documentationCacheDirectory: File,
      gitDiffSpec: Option[String],
      serviceIdOverride: Option[String],
      outputFile: File,
      loader: ClassLoader,
  ): Either[String, File] = {
    val git = new GitCliClient()
    for {
      docRoot <- DocumentationRootResolver.resolve(
        log,
        documentationRepoUrl,
        localDocumentationRoot,
        documentationRef,
        documentationCacheDirectory,
      )
      config <- ConfigLoader.load(new File(docRoot, documentationConfigRelativePath))
      _ <- Either.cond(config.version == 1, (), s"Unsupported autodoc config version: ${config.version}")
      gitRoot <- git.repoRoot(baseDirectory)
      entries <- git.diffNameStatus(baseDirectory, gitDiffSpec)
      projectPath = relativePathFromRepoRoot(gitRoot, baseDirectory)
      scoped <- ProjectScope.filterToProject(gitRoot, baseDirectory, entries)
      service <- ProjectContextDetector.resolveService(config, projectPath, serviceIdOverride)
      ctx = DocContext(
        serviceId = service.id,
        serviceTitle = service.title.getOrElse(service.id),
        projectBase = baseDirectory,
        gitRoot = gitRoot,
        projectPathFromRepoRoot = projectPath,
        scopedChanges = scoped,
      )
      renderer = new MarkdownRenderer(loader)
      md <- renderer.renderMarkdown(docRoot, config.defaults, service, ctx)
      _ = FileUtils.writeUtf8(outputFile, md)
    } yield outputFile
  }

  private def relativePathFromRepoRoot(gitRoot: File, projectBase: File): String = {
    val root = gitRoot.getCanonicalFile.toPath
    val base = projectBase.getCanonicalFile.toPath
    FileUtils.posixPathString(root.relativize(base))
  }
}
