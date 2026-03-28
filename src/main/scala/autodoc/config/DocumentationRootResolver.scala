package autodoc.config

import sbt.Logger

import java.io.File

/** Resolves the documentation repository root (local path or cached clone), same logic as [[autodoc.task.AutoDocRunner]]. */
object DocumentationRootResolver {

  def resolve(
      log: Logger,
      documentationRepoUrl: Option[String],
      localDocumentationRoot: Option[File],
      documentationRef: String,
      documentationCacheDirectory: File,
  ): Either[String, File] =
    (localDocumentationRoot, documentationRepoUrl) match {
      case (Some(root), _) =>
        if (root.isDirectory) Right(root)
        else Left(s"sbt-autodoc: autoDocLocalDocumentationRoot is not a directory: ${root.getAbsolutePath}")
      case (None, Some(u)) =>
        DocumentationRepo.ensureCheckout(u, documentationRef, documentationCacheDirectory, log)
      case (None, None) =>
        Left(
          "sbt-autodoc: set autoDocDocumentationRepoUrl (git URL for ad-service-documentation) " +
            "or autoDocLocalDocumentationRoot (local checkout path)",
        )
    }
}
