package autodoc.config

import autodoc.model.{AutoDocConfig, ServiceConfig}

object ConfigResolver {
  /**
    * Pick the service whose path prefix best matches the project path (longest prefix wins).
    * A prefix of `.` or `*` (or empty) means "this service owns the whole repository" — used when the git
    * root is one service (e.g. ad-service with sbt modules `core`, `service`, …).
    */
  def resolveService(config: AutoDocConfig, projectPathFromRepoRoot: String): Option[ServiceConfig] = {
    val dir = normalizeDir(projectPathFromRepoRoot)
    val catchAllServices = config.services.filter(_.pathPrefixes.exists(isCatchAllPrefix))
    val scored = config.services.flatMap { svc =>
      svc.pathPrefixes.filterNot(isCatchAllPrefix).flatMap { p =>
        val prefix = normalizePrefix(p)
        if (dir == prefix.dropRight(1) || dir.startsWith(prefix)) Some((prefix.length, svc))
        else None
      }
    }
    scored.sortBy(-_._1).headOption.map(_._2).orElse(catchAllServices.headOption)
  }

  private def isCatchAllPrefix(p: String): Boolean = {
    val t = p.replace('\\', '/').stripPrefix("/").stripSuffix("/").trim
    t.isEmpty || t == "." || t == "*"
  }

  private def normalizeDir(s: String): String = {
    val t = s.replace('\\', '/').stripPrefix("/").stripSuffix("/")
    if (t.isEmpty) "." else t
  }

  private def normalizePrefix(p: String): String = {
    val t = p.replace('\\', '/').stripPrefix("/")
    if (t.endsWith("/")) t else t + "/"
  }
}
