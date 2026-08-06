<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Gradle-9.x-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle"/>
  <img src="https://img.shields.io/badge/JPMS-modular-blue?style=for-the-badge" alt="JPMS"/>
  <img src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=for-the-badge" alt="Status"/>
  <!-- quando publicar: badge do Maven Central -->
  <!-- <img src="https://img.shields.io/maven-central/v/io.axiom/axiom-core?style=for-the-badge" alt="Maven Central"/> -->
</p>

# Axiom

**Biblioteca Java de utilitários e abstrações de uso geral**

Conjunto de módulos pequenos e independentes — tipos de resultado/erro,
coleções persistentes, concorrência leve, reflection fluente, configuração
declarativa e serialização — que qualquer projeto Java 21+ pode adotar
parcialmente, sem herdar um framework. Cada módulo se justifica sozinho: nada
de acoplamento estrutural entre `axiom-collections` e `axiom-json`.

> Este projeto nasceu de uma auditoria técnica de duas bibliotecas anteriores
> (JToolBox e Obsidian). Ver [`docs/architecture.md`](docs/architecture.md)
> para o racional completo de design.

---

## Índice

- [Visão Geral](#visão-geral)
- [Filosofia](#filosofia)
- [Módulos](#módulos)
- [Arquitetura](#arquitetura)
- [Getting Started](#getting-started)
- [Comandos Gradle](#comandos-gradle)
- [Exemplos](#exemplos)
- [Testes](#testes)
- [Status do Projeto](#status-do-projeto)
- [Versionamento e Changelog](#versionamento-e-changelog)
- [Documentação](#documentação)
- [Contribuindo](#contribuindo)
- [Licença](#licença)

---

## Visão Geral

- Tipos funcionais de resultado/erro (`Try`, `Result<T,E>`, `Maybe<T>`)
- Coleções persistentes/imutáveis (HAMT, vetor, pilha, fila, mapa/set ordenados)
- Concorrência leve (`Promise`, cancelamento cooperativo, `Box`/`AtomicBox`)
- Reflection fluente com cache de lookup
- Configuração declarativa (`.env` + injeção por anotação, profiles, reload)
- Modelo de dados JSON e YAML próprios, cada um com motor plugável (Gson/SnakeYAML escondidos)
- I/O de arquivos e scanner de console testável
- **Zero framework**: sem DI container, sem servidor HTTP, sem ORM, sem test
  runner — ver [`docs/architecture.md#2`](docs/architecture.md) para a lista
  explícita do que **não** pertence à Axiom.

---

## Filosofia

- **Um axioma é uma verdade mínima que não precisa de prova** — cada módulo
  se sustenta sozinho.
- **Biblioteca, não framework.**
- **Testado por padrão** — nenhum módulo entra sem suíte cobrindo caminho
  principal e casos de borda.
- **Poucas dependências, todas com uso confirmado por `import` real no PR**
  — verificado automaticamente em CI, ver `scripts/audit-deps.sh`.
- **Java 21 como piso** — `sealed`, records, pattern matching e virtual
  threads onde resolvem um problema real, nunca como enfeite.

Detalhamento completo em [`docs/architecture.md`](docs/architecture.md).

---

## Módulos

| Módulo | Responsabilidade | Depende de | Status |
|---|---|---|---|
| `axiom-core` | `Try`, `Result`, `Maybe`, `Failable*`, exceções base | — | Em desenvolvimento |
| `axiom-collections` | Coleções persistentes: HAMT, vetor, pilha, fila, mapa/set ordenados | — | Em desenvolvimento |
| `axiom-concurrent` | `Promise`, `Box`/`AtomicBox`, execução com virtual threads | `axiom-core` | Em desenvolvimento |
| `axiom-reflect` | Reflection fluente com cache de lookup | `axiom-core` | Em desenvolvimento |
| `axiom-dotenv` | Parsing + injeção declarativa de `.env`, com `@Profile`/`@Reloadable` | `axiom-core`, `axiom-reflect`, `axiom-placeholder` | Em desenvolvimento |
| `axiom-json` | Modelo JSON próprio + motor plugável (Gson escondido) | `axiom-core`, `axiom-reflect` | Em desenvolvimento |
| `axiom-yaml` | Modelo YAML próprio + motor plugável (SnakeYAML escondido) | `axiom-core`, `axiom-reflect` | Em desenvolvimento |
| `axiom-placeholder` | Resolução de placeholders/templates de string | `axiom-core` | Em desenvolvimento |
| `axiom-io` | Arquivos, hashing, compressão, busca em texto (`FileWatcher` pendente) | — | Em desenvolvimento |
| `axiom-console` | Scanner de console testável (`InputSource` plugável, `tryRead` não-lançador) | `axiom-core` | Em desenvolvimento |
| `axiom-text` / `axiom-datetime` | Utilitários que a JDK não resolve bem | — | **Avaliado, não criado** — ver `axiom-core` (`HumanDuration`) e `CHANGELOG.md` |
| `axiom-numeric` | Inteiros de largura fixa, unsigned, overflow explícito, bytes/endianness | — | Em desenvolvimento |
| `axiom-id` | Identificadores ordenáveis por tempo (UUIDv7, ULID) | — | Em desenvolvimento |
| `axiom-csv` | Parsing/escrita de CSV com modelo próprio | — | Em desenvolvimento |
| `axiom-experimental` | APIs em incubação | — | Planejado |

Status possíveis: Estável · Em desenvolvimento · Planejado · Proposto · Avaliar necessidade

---

## Arquitetura

```
axiom-core
   ↑
   ├── axiom-collections
   ├── axiom-reflect
   ├── axiom-concurrent
   ├── axiom-text
   ├── axiom-datetime
   ├── axiom-io
   ├── axiom-id
   ├── axiom-csv
   ├── axiom-numeric
   ├── axiom-console
   ├── axiom-placeholder
   ├── axiom-dotenv    ──┐
   ├── axiom-json         ├─ podem depender de axiom-reflect
   └── axiom-yaml         │  (e, no caso de dotenv/yaml, de axiom-placeholder)
                        ──┘
```

DAG estrito, sem ciclos. Nenhum módulo de capacidade depende de outro módulo
de capacidade fora dessa relação declarada. **Não há módulo `axiom-control`**
(DSL de controle de fluxo) — decisão explícita e definitiva, ver
[`docs/architecture.md#2`](docs/architecture.md#2-escopo-o-que-pertence-e-o-que-não-pertence-à-axiom).
Ver [`docs/architecture.md#3`](docs/architecture.md) para o racional completo.

---

## Getting Started

**Requisitos**: JDK 21+, Gradle 9.x

```kotlin
// build.gradle.kts do seu projeto
dependencies {
    implementation("io.axiom:axiom-core:<versão>")
    implementation("io.axiom:axiom-collections:<versão>") // opcional, por módulo
}
```

Cada módulo é independente — adote só o que precisar.

---

## Comandos Gradle

| Comando | Descrição |
|---|---|
| `./gradlew build` | Compila e testa todos os módulos (inclui `examples/`) |
| `./gradlew :axiom-core:test` | Testa só um módulo |
| `./gradlew publishToMavenLocal` | Publica localmente para testar integração |
| `make build` / `make test` / `make local` | Wrappers de conveniência (ver `Makefile`) |
| `bash scripts/audit-deps.sh` | Verifica que toda dependência declarada tem `import` real |

---

## Exemplos

Cada módulo tem um exemplo executável e compilado por CI em
`<modulo>/examples/src/main/java/...` (não são apenas trechos soltos no
README — são compilados e rodam como parte do build, para nunca ficarem
desatualizados). Ver o README de cada módulo, seção "Exemplo Rápido", e o
diretório `examples/` correspondente.

---

## Testes

- Cobertura mínima obrigatória por módulo antes de release `1.0`: caminho
  principal + casos de borda + (estruturas de dados) invariantes estruturais.
- CI roda build + testes em todo PR; nenhum módulo é publicado sem build
  verde.

```bash
./gradlew test
```

---

## Status do Projeto

Roadmap por etapas (ver [`docs/architecture.md#14`](docs/architecture.md)
para detalhes):

- [x] Etapa 0 — Fundação (`axiom-core`, convenções, CI)
- [x] Etapa 1 — Coleções persistentes (HAMT map/set, `PVector`, `PStack`, `PQueue`, `PSortedMap`/`PSortedSet` completos)
- [x] Etapa 2 — Concorrência (`Promise`, `Box`/`AtomicBox`, `Tasks`)
- [x] Etapa 3 — Reflection e configuração (`axiom-reflect`, `axiom-placeholder`, `axiom-dotenv`, incluindo `@Profile`/`@Reloadable`)
- [x] Etapa 4 — Dados e I/O (`axiom-json`, `axiom-yaml`, `axiom-io`)
- [x] Etapa 5 — Console e extras (`axiom-console`; `axiom-text`/`axiom-datetime` avaliados e não criados — `HumanDuration` foi para `axiom-core`)
- [x] Etapa 6 — Estabilização (auditoria de dependências, revisão de nomenclatura cruzada,
      documentação completa; nenhum bloqueador conhecido restante para a tag `1.0`)

---

## Versionamento e Changelog

Versionamento semântico estrito desde `0.x`. Mudanças documentadas em
[`CHANGELOG.md`](CHANGELOG.md), seguindo o formato
[Keep a Changelog](https://keepachangelog.com/). APIs experimentais só vivem
em `axiom-experimental` — o módulo declarado pelo consumidor já comunica o
nível de estabilidade.

---

## Documentação

- [`docs/architecture.md`](docs/architecture.md) — documento central de
  arquitetura e decisões de design (o "porquê"; para status atual, ver as
  seções acima e o `CHANGELOG.md`)
- [`docs/CONVENTIONS.md`](docs/CONVENTIONS.md) — checklist de nomenclatura,
  dependências e documentação aplicado a cada PR, incluindo o template de
  README por módulo (usar para qualquer módulo novo)
- [`auditoria.md`](auditoria.md) — auditoria técnica de JToolBox/Obsidian que originou este projeto
- Javadoc por módulo, publicado em `<link a definir>`
- README por módulo (`<modulo>/README.md`)

---

## Contribuindo

Toda dependência nova precisa de um `import` real linkado no diff (verificado
por `scripts/audit-deps.sh` em CI); nomenclatura não pode colidir com pacotes
da JDK; testes cobrindo caminho principal e casos de borda são obrigatórios.
Ver [`docs/CONVENTIONS.md`](docs/CONVENTIONS.md).

---

## Licença

<!-- Apache 2.0 / MIT / a definir -->
