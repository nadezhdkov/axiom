# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

This is the **Axiom repository**: a Java 21, multi-module Gradle library (`io.axiom`), plus the planning/audit documents that drove its design. All six roadmap etapas (`docs/architecture.md §14`) are implemented — 10 modules, each with `module-info.java`, a real test suite, a compiled `examples/` sourceSet, and a `README.md`. There is no remaining "not yet built" module; `axiom-experimental`/`axiom-bench` are the only ones still unclaimed, and only because nothing has needed them yet.

The repository contains:

1. **`auditoria.md`** — a detailed, file-and-class-cited technical audit of two prior Java libraries, JToolBox and Obsidian, that Axiom's design was derived from. Their source trees are **no longer in this repo** — porting is complete and they were removed once nothing further needed to be extracted from them (their GitHub remotes still exist if the original source is ever needed again). `auditoria.md` is now a frozen historical record: don't add new file/class citations to it (there's no source left to verify them against), only correct a factual error in what's already written.
2. **`docs/architecture.md`** — the architecture proposal for Axiom, derived from `auditoria.md`. This is the normative source for module boundaries, the dependency DAG, and design principles. It predates implementation and is *not* a status tracker — for current implementation status, read `README.md` and `CHANGELOG.md` instead.
3. **`docs/CONVENTIONS.md`** — the PR checklist (naming, dependencies, tests, docs) applied to every module, plus the per-module README template; includes a recorded open tension (exception hierarchy — see below).
4. **The `axiom-*/` modules themselves** — `axiom-core`, `axiom-collections`, `axiom-concurrent`, `axiom-reflect`, `axiom-placeholder`, `axiom-dotenv`, `axiom-json`, `axiom-yaml`, `axiom-io`, `axiom-console`. Root `build.gradle.kts`/`settings.gradle.kts`/`gradle/libs.versions.toml`/`Makefile` wire them together.

(`axiom.md` and `docs/AXIOM-PROPOSTAS-E-PADRAO-DOCS.md` existed earlier in the project's history — a root-level copy of the architecture doc, and a module-proposal log — and were removed once `docs/architecture.md`/`docs/CONVENTIONS.md`/`README.md`/`CHANGELOG.md` made them fully redundant. If you see either name mentioned in old commit messages or module READMEs, they mean `docs/architecture.md`.)

Any question about "does X exist in JToolBox/Obsidian" or "why does Axiom do Y instead of what those libraries did" should be answered from `auditoria.md` and `docs/architecture.md` — both cite exact file paths and classes from a point-in-time reading of the source, but that source is no longer available locally to re-verify against. Treat citations there as historical fact reported at the time of the audit, not something re-checkable in this repo.

## Working in this repository

- **This is a real, buildable codebase now** — treat requests as normal software engineering work (bug fixes, new features, refactors) unless the user is explicitly asking about the planning documents themselves.
- **`docs/architecture.md` is still normative for new work.** Any new module idea, dependency, or package name must be checked against it (and against `docs/CONVENTIONS.md` for documentation format) before being added. If a change contradicts an existing decision in `docs/architecture.md` (e.g., re-adding a control-flow DSL module, which was deliberately rejected), surface that conflict instead of silently reconciling it.
- If updating `auditoria.md` or `docs/architecture.md`, preserve the existing convention: technical claims about JToolBox/Obsidian cite a concrete file path and class/interface name rather than a general impression — even though that source is no longer in this repo to check against, the citation format is what makes the claim verifiable in principle (via the projects' GitHub remotes) rather than folklore.
- Distinguish, in both documents, between what was **observed** in the audited libraries (fact) and what is **recommended** for Axiom (decision) — this distinction is already threaded through both files and must be preserved in edits.
- **Known open tension, not yet resolved**: most module root exceptions (`JsonException`, `YamlException`, `FileOperationException`, `ReflectException`, `PromiseException`, `ParseFailureException`/`ValidationException`) extend `RuntimeException` directly instead of `axiom-core`'s `AxiomException`, because those modules deliberately don't depend on `axiom-core`. See `docs/CONVENTIONS.md` ("Hierarquia de exceções") before "fixing" this unilaterally — it needs a maintainer decision, not a mechanical patch.
- **Etapa 1's `axiom-collections` and Etapa 3's `axiom-dotenv` were originally shipped with deferred pieces** (`PVector`/`PQueue`/`PStack`/`PSortedMap`/`PSortedSet`; `@Profile`/`@Reloadable`) that have since been completed — `CHANGELOG.md` has the full history if you need to understand why a design decision was made in two steps.

## Commands

- `./gradlew build` / `make build` — compiles and tests every module, including compiling each `examples/` sourceSet
- `./gradlew :axiom-core:test` — test a single module
- `make local` — `publishToMavenLocal`, useful for verifying a consumer project against local changes
- `bash scripts/audit-deps.sh` / `make audit-deps` — fails if any `implementation(libs.x)` dependency has no real `import` backing it in `src/main`
- No known failing/red state — `./gradlew build` and `make local` are green as of the last verification in `CHANGELOG.md`

## Architecture (from `docs/architecture.md`)

Axiom is a multi-module Gradle project, group `io.axiom`, Java 21 minimum, JPMS (`module-info.java`) per module. Strict dependency DAG rooted at `axiom-core`; no capability module depends on another capability module except where explicitly declared. Actual (not just planned) dependency edges today:

```
axiom-core            (Try, Result, Maybe, Failable*, base exceptions, HumanDuration, TypeReference)
   ↑
   ├── axiom-collections     (no dependency — HAMT map/set, PVector/PStack/PQueue, PSortedMap/PSortedSet)
   ├── axiom-reflect          (no dependency — fluent reflection, lookup cache)
   ├── axiom-concurrent       (no dependency — Promise, Box/AtomicBox, Tasks)
   ├── axiom-io               (no dependency — files, hashing, compression, text search)
   ├── axiom-console          (→ axiom-core, for Result in InputHandler#tryRead)
   ├── axiom-placeholder      (→ axiom-core)
   ├── axiom-dotenv           (→ axiom-core, axiom-reflect, axiom-placeholder)
   ├── axiom-json             (→ axiom-core, axiom-reflect; Gson hidden behind internal.gson.*)
   └── axiom-yaml             (→ axiom-core, axiom-reflect; SnakeYAML hidden behind internal.snakeyaml.*)
```

`axiom-text`/`axiom-datetime` were evaluated per `docs/architecture.md`'s own "does the JDK already solve this?" filter and **not created** — the one real gap found (human-readable `Duration` formatting) became `io.axiom.core.time.HumanDuration` inside `axiom-core` instead. `axiom-experimental`/`axiom-bench` remain unclaimed — nothing has needed them yet.

Key decisions to respect when discussing or extending this design:

- **No `axiom-control` module.** A fluent control-flow DSL (`If`/`When`/`Condition`/`Switch`-style) was deliberately excluded — neither JToolBox's `If`/`Condition` nor Obsidian's five `When` variants converged on a good design, and Java 21's expression `switch` + pattern matching already covers the need. Do not propose reintroducing this without flagging that it reverses a recorded decision.
- **No framework-scale modules.** DI container, HTTP server/MVC, ORM/JdbcTemplate clone, and test-runner are explicitly out of scope — this was the single biggest scope failure identified in JToolBox (`ioc/`, `http/server`, `jdbc/dao/JdbcTemplate`, `test/`).
- **One system per concern.** JToolBox had three uncoordinated configuration systems (`dotenv/`, `config/`, `ioc/environment/Environment`); Axiom consolidated all of that into `axiom-dotenv` alone, including `@Profile`/`@Reloadable`. Obsidian had `Maybe`/`Result`/`Try` coexisting without a documented boundary; `axiom-core`'s package-info documents the boundary explicitly.
- **No JDK-shadowing package names.** Obsidian's `obsidian-reflection` module used package `lang.reflect`, colliding with `java.lang.reflect`. Any new package name must be checked against `java.*`/`javax.*` first.
- **Engine encapsulation.** Modules that wrap a third-party engine (Gson for `axiom-json`, SnakeYAML for `axiom-yaml`) hide it behind an `internal.*` subpackage never exported in `module-info.java`, and never leak the third-party type through a public API return type.
- **No dependency without confirmed use.** Both audited libraries had dead dependencies (ASM in JToolBox, unused catalog entries in Obsidian's `libs.versions.toml`). Any dependency addition should be traceable to a real `import` — enforced by `scripts/audit-deps.sh` for `libs.versions.toml` entries (it does not currently check `project(":axiom-x")` internal dependencies for real use, only cataloged third-party ones).
- **One name, one concept.** `TypeReference` was independently duplicated in `axiom-json`/`axiom-yaml` before being consolidated into `axiom-core` during the Etapa 6 stabilization pass — a live example of the rule in `docs/architecture.md §4`/`docs/CONVENTIONS.md`, not just a stated principle.

For the full module-by-module breakdown (responsibilities, what's reused/refactored/discarded from each source library, naming conventions, testing strategy, documentation strategy, and the original phased implementation roadmap), read `docs/architecture.md` directly — it is the design record and should not be duplicated here. For what's actually built and its current status, read `README.md`'s module table and `CHANGELOG.md`.
