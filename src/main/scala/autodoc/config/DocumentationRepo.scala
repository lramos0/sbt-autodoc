package autodoc.config

import autodoc.util.LogUtils
import sbt.Logger

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import scala.sys.process._

/**
  * Shallow-clone or update ad-service-documentation into a cache directory.
  *
  * Git operations are [[synchronized]] per canonical cache path so parallel sbt projects (e.g. `all autoDoc`)
  * do not corrupt the same clone (`shallow.lock`, "shallow file has changed since we read it").
  */
object DocumentationRepo {

  private val cacheLocks = new ConcurrentHashMap[String, AnyRef]()

  private def lockFor(cacheDir: File): AnyRef = {
    val key =
      try cacheDir.getCanonicalPath
      catch { case _: Exception => cacheDir.getAbsolutePath }
    cacheLocks.computeIfAbsent(key, _ => new AnyRef)
  }

  def ensureCheckout(
      repoUrl: String,
      ref: String,
      cacheDir: File,
      log: Logger,
  ): Either[String, File] =
    lockFor(cacheDir).synchronized {
      if (!cacheDir.exists()) cacheDir.mkdirs()
      val marker = new File(cacheDir, ".git")
      if (!marker.exists) cloneShallow(repoUrl, ref, cacheDir, log)
      else fetchAndCheckout(cacheDir, ref, log).map(_ => cacheDir)
    }

  private def cloneShallow(url: String, ref: String, dest: File, log: Logger): Either[String, File] = {
    LogUtils.info(log, s"sbt-autodoc: cloning documentation repo into ${dest.getAbsolutePath}")
    val withBranch = Process(Seq("git", "clone", "--depth", "1", "-b", ref, url, dest.getAbsolutePath))
    val code1 = withBranch.!(ProcessLogger(log.info(_), log.warn(_)))
    if (code1 == 0) Right(dest)
    else {
      LogUtils.warn(log, s"sbt-autodoc: shallow clone with -b $ref failed; retrying without branch then checking out")
      val code2 = Process(Seq("git", "clone", url, dest.getAbsolutePath)).!(ProcessLogger(log.info(_), log.warn(_)))
      if (code2 != 0) Left(s"git clone failed with exit code $code2")
      else checkout(dest, ref, log).map(_ => dest)
    }
  }

  private def fetchAndCheckout(dest: File, ref: String, log: Logger): Either[String, Unit] = {
    LogUtils.info(log, s"sbt-autodoc: updating documentation repo at ${dest.getAbsolutePath}")
    val fetch = Process(Seq("git", "-C", dest.getAbsolutePath, "fetch", "--depth", "1", "origin", ref))
    val _ = fetch.!(ProcessLogger(log.info(_), log.warn(_)))
    checkout(dest, ref, log)
  }

  private def checkout(dest: File, ref: String, log: Logger): Either[String, Unit] = {
    val co = Process(Seq("git", "-C", dest.getAbsolutePath, "checkout", ref))
    val code = co.!(ProcessLogger(log.info(_), log.warn(_)))
    if (code == 0) Right(())
    else Left(s"git checkout $ref failed with exit code $code")
  }
}
