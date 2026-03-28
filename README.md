# sbt-autodoc

You would rather ship **ad-service** than spend your afternoon in **ad-service-documentation**. This plugin keeps you in the service repo: it pulls the shared JSON config from that docs repo (or a local clone), runs `git diff`, scopes changes to your sbt project, and writes Markdown so you touch the documentation repo as little as possible.

It:

1. Loads configuration from **ad-service-documentation** (or a local checkout you point at).
2. Runs `git diff --name-status` and, by default (**`autoDocPerSubproject := false`**), keeps paths under the **build root** `baseDirectory` (one repo, one `autodoc.md`). Set **`autoDocPerSubproject := true`** to scope each subproject separately and write **`target/autodoc/autodoc.md`** per module. By default it diffs **all commits on your current branch** since the merge-base with `origin/main` (`origin/main...HEAD`); you can switch to uncommitted-only or override the base ref (see settings below).
3. Resolves the **service** (e.g. `ad-service`) from `pathPrefixes` in the config, or from `autoDocServiceId`.
4. Renders a Markdown summary using a template from the documentation repo (or the bundled default).

The plugin is built for **Scala 2.12** (sbt’s own Scala version). It can be used in **Scala 2.12 and 2.13** application builds; those versions only affect your project code, not the plugin.

## Usage

### 1. Publish or depend on the plugin

```scala
// project/plugins.sbt
addSbtPlugin("autodoc" % "sbt-autodoc" % "0.3.0-SNAPSHOT")
```

### 2. Point at ad-service-documentation

Either clone the repo yourself and set `autoDocLocalDocumentationRoot`, or let the plugin shallow-clone:

```scala
// build.sbt (per project, or in ThisBuild / projectSettings)
autoDocDocumentationRepoUrl := Some("git@github.com:<owner>/ad-service-documentation.git"),
autoDocDocumentationRef := "main",
autoDocDocumentationConfigPath := "autodoc/config.json",
```

### 3. Run the task

```bash
sbt autoDoc
```

By default this runs on the **build root** only and writes **`target/autodoc/autodoc.md`** under that root (nested projects log a skip). For a **multi-folder service** in one git repo (e.g. `ad-service/`), set **`autoDocPerSubproject := true`** and run e.g. `sbt adService/autoDoc` so each module gets its own scoped diff and output.

### Useful settings

| Key | Purpose |
| --- | --- |
| `autoDocLocalDocumentationRoot` | Use a local path instead of cloning (overrides `autoDocDocumentationRepoUrl`). |
| `autoDocGitDiffScope` | **`branch`** (default): entire branch vs merge-base with `autoDocGitBranchBase` (`git diff base...HEAD`). **`uncommitted`**: only local changes vs `HEAD` (`git diff HEAD`). |
| `autoDocGitBranchBase` | Left side of the three-dot diff when scope is `branch` (default `origin/main`). Use `origin/master` if your default branch is `master`. |
| `autoDocGitDiffSpec` | If set, overrides scope and base; passed as the rev expression to `git diff --name-status` (e.g. `Some("origin/develop...HEAD")`). |
| `autoDocServiceId` | Force a service id from the JSON config when path-based inference is wrong. |
| `autoDocPerSubproject` | **`false`** (default): one output at **build root** `target/autodoc/`, git scope = whole repo; **`autoDoc` / `autoDocElaborate` no-op** on nested projects. **`true`**: legacy per-module outputs and scoped diffs. |
| `autoDocOutputFile` | Where to write the Markdown file (defaults under build root or subproject `target/` per `autoDocPerSubproject`). |
| `autoDocElaborationProvider` | `none` (default), `claude-code`, `cursor-cli`, etc. Requires plugin **≥ 0.3.0-SNAPSHOT** (or a build that includes elaboration). |
| `autoDocElaborationMode` | `handoff` (write prompt only) or `execute` (run provider CLI if configured). Task: `autoDocElaborate`. |
| `autoDocElaborationMermaidDiagrams` | If the documentation repo contains **`.mmd`** files: **`ask`** (default) uses sbt’s **InteractionService** (yes/no) to add diagram-update instructions; **`include`** always adds them; **`skip`** never does. The prompt tells the agent to **edit those `.mmd` paths in place**, not to paste Mermaid into the elaborated markdown. For CI / no TTY, prefer **`include`** or **`skip`** (batch mode may treat **ask** as “no”). |

## ad-service-documentation JSON (`version`: 1)

Place a file such as `autodoc/config.json` in that repository:

```json
{
  "version": 1,
  "services": [
    {
      "id": "ad-service",
      "title": "Ad Service",
      "pathPrefixes": ["ad-service/", "services/ad-service/"]
    }
  ],
  "defaults": {
    "markdownTemplate": "default.md.tpl",
    "templateRoot": "templates",
    "outputFileName": "autodoc.md"
  }
}
```

Single git repo for one service with several sbt modules (`core/`, `service/`, …):

```json
{
  "version": 1,
  "services": [
    {
      "id": "ad-service",
      "title": "Ad Service",
      "pathPrefixes": ["."]
    }
  ]
}
```

- **pathPrefixes**: Used to infer which service an sbt project belongs to (longest matching prefix wins). Paths are POSIX-style, relative to the git repository root. Use **`"."`** (or `"*"`) when the git repo is a single service with multiple sbt modules (e.g. ad-service with `core/`, `service/`, …) so every module maps to the same service.
- **templates**: Resolved under the documentation repo root, e.g. `templates/default.md.tpl`. Placeholders: `{{serviceId}}`, `{{serviceTitle}}`, `{{projectPath}}`, `{{generatedAt}}`, `{{changeList}}`.

### Troubleshooting

**`RuntimeException` showing only a path to `.../autodoc/config.json`**

That almost always means the file **does not exist** at `autoDocDocumentationConfigPath` inside the documentation repo (default `autodoc/config.json`). Add it there, or point `autoDocDocumentationConfigPath` at whatever path your team actually uses (for example `config/autodoc.json`).

**Many parallel `git fetch` lines when running `autoDoc` on all projects**

The documentation repo is cached under **`(LocalRootProject base)/target/autodoc/documentation-repo`**. Git access is serialized per cache dir. With the default **`autoDocPerSubproject := false`**, only the root project writes **`autodoc.md`**; `all autoDoc` mostly skips nested projects. Use **`autoDocPerSubproject := true`** only when you need one markdown per sbt module.

**`no service mapping for project path 'core'`**

Your git root is probably the service repo (paths like `core`, `service`). Either add **`pathPrefixes: ["."]`** on the ad-service service in the documentation config, list every module (`core/`, `service/`, …), or set **`autoDocServiceId`** in sbt.

**`not found: value autoDocGitDiffScope` (or other keys, e.g. `autoDocElaborationProvider`)**

1. **Use a JAR that defines those keys** — elaboration settings exist from **`0.3.0-SNAPSHOT`**. Set `addSbtPlugin("autodoc" % "sbt-autodoc" % "0.3.0-SNAPSHOT")`, run **`sbt publishLocal`** in this repo, then **`reload`** in the consumer. Remote **`0.2.0-SNAPSHOT`** artifacts may predate elaboration; Coursier can keep serving an old snapshot unless you bump the declared version or clear caches.
2. **Import keys explicitly** (first lines of `build.sbt`, before any `lazy val`):

```scala
import autodoc.SbtKeys._
```

or:

```scala
import autodoc.keys.AutoDocKeys
// then AutoDocKeys.autoDocGitDiffScope := ...
```

3. Optional: `import autodoc.AutoDocPlugin.autoImport._` — same keys as `SbtKeys` when the plugin JAR on the classpath is current.

**`Cannot run program "agent"` / `No such file or directory` (cursor-cli)**

The Cursor CLI binary is not on the **`PATH` seen by the JVM** (common when sbt is started from an IDE). Install [Cursor CLI](https://cursor.com/docs/cli/installation) and add **`~/.local/bin`** to PATH in the environment that launches sbt, or set an absolute path, e.g. `autoDocElaborationCursorCliExecutable := sys.props("user.home") + "/.local/bin/agent"`. The plugin also tries **`~/.local/bin/agent`** automatically when the setting is the default name `agent` but `agent` is missing from PATH.

## Development

```bash
sbt compile
```

## License

See [LICENSE](LICENSE).
