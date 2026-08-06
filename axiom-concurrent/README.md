# axiom-concurrent

**Abstrações leves de concorrência: `Promise` assíncrono com cancelamento
cooperativo e retry, e a família `Box`/`AtomicBox` de containers mutáveis.**

<p align="left">
  <img src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=flat-square" alt="Status"/>
  <img src="https://img.shields.io/badge/depende%20de-JDK%20apenas-blue?style=flat-square" alt="Dependências"/>
</p>

---

## Índice

- [O que resolve](#o-que-resolve)
- [O que não resolve](#o-que-não-resolve)
- [Instalação](#instalação)
- [Exemplo Rápido](#exemplo-rápido)
- [API Principal](#api-principal)
- [Notas de Design](#notas-de-design)
- [Testes](#testes)
- [Changelog](#changelog)

---

## O que resolve

- `Promise<T>`: computação assíncrona com combinadores (`all`/`any`/`race`),
  cancelamento cooperativo (`CancellationToken`/`CancellationSource`),
  `RetryPolicy` com estratégias de backoff (fixo, exponencial, com jitter, ou
  nenhum), e interoperação com `CompletableFuture`.
- `Box<T>`/`AtomicBox<T>`/`PlainBox<T>`/`AtomicVolatileBox<T>`: containers
  mutáveis de um único valor com três níveis de garantia de concorrência —
  do zero overhead (`PlainBox`) ao CAS lock-free (`AtomicBox`).
- `Tasks`: executor compartilhado de virtual threads (Java 21) para trabalho
  assíncrono de propósito geral — usado como executor padrão de
  `Promises.async`.

## O que não resolve

Não é um framework de resiliência tipo circuit breaker — `RetryPolicy` cobre
o caso legítimo de retry sem precisar virar módulo à parte. Não é um pool de
threads configurável de uso geral — para isso, construa um
`ExecutorService` diretamente.

---

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-concurrent:<versão>")
}
```

**Requisitos**: JDK 21+. Zero dependências de terceiros.

---

## Exemplo Rápido

```java
import io.axiom.concurrent.promise.Promise;
import io.axiom.concurrent.promise.Promises;
import io.axiom.concurrent.promise.RetryPolicy;
import io.axiom.concurrent.box.Box;
import java.time.Duration;

Promise<Integer> loaded = Promises.async(() -> loadData())
        .retry(RetryPolicy.exponential(3, Duration.ofMillis(10)));

int value = loaded.get(Duration.ofSeconds(5));

Box<Integer> counter = Box.of(0);
counter.updateAndGet(n -> n + 1);
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/concurrent/examples/ConcurrentExamples.java`](examples/src/main/java/io/axiom/concurrent/examples/ConcurrentExamples.java).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `Promises.async(Supplier<T>)` | Executa em `Tasks.executor()` (virtual threads) por padrão |
| `Promise#map`/`#flatMap`/`#recover`/`#recoverWith` | Composição e recuperação |
| `Promise#retry(RetryPolicy)` | Reexecuta a operação original (não apenas a recuperação) a cada tentativa |
| `Promises.all`/`any`/`race` | Combinadores de múltiplas promises |
| `CancellationSource.create()` / `CancellationToken` | Cancelamento cooperativo |
| `Box.of`/`plain`/`volatileBox` | Container mutável com o nível de concorrência escolhido |

---

## Notas de Design

- `Promise` é essencialmente uma API mais ergonômica sobre `CompletableFuture`
  — a implementação padrão delega a ele internamente, sem expor o tipo
  `CompletableFuture` como implementação obrigatória (`toCompletableFuture()`
  existe só para interoperação).
- **Bug corrigido em `retry(RetryPolicy)`**: a versão original recuperava
  sempre contra o mesmo future já resolvido, sem nunca reexecutar a operação
  assíncrona real — mesma classe de bug do `Try.retry()` corrigido em
  `axiom-core`. A implementação aqui guarda o supplier original (quando a
  Promise foi criada via `Promises.async`) e o reexecuta a cada tentativa.
  Coberto por teste de regressão (`PromiseTest#retryReinvokesOriginalSupplierUntilSuccess`).
- **Bug corrigido em `state()`**: `Deferred#cancel(String)` completa o
  future interno via `completeExceptionally`, não via `Future#cancel` — sem
  tratar esse caminho explicitamente, `state()` reportava `REJECTED` em vez
  de `CANCELLED`. Corrigido e coberto por teste.
- Executor padrão de `Promises.async` é `Tasks.executor()` (virtual threads),
  não `ForkJoinPool.commonPool()` como na implementação original — decisão
  deliberada seguindo o princípio de usar virtual threads onde resolvem um
  problema real (`docs/architecture.md §7`).
- Nenhum tipo deste módulo tem estado estático mutável não sincronizado.

---

## Testes

```bash
./gradlew :axiom-concurrent:test
```

Requisito não negociável (`docs/architecture.md §11`): testes de
concorrência real, não só sequenciais — múltiplas threads competindo para
resolver/cancelar o mesmo `Deferred` simultaneamente
(`PromiseConcurrencyTest`), além de atualização concorrente de `AtomicBox`.
Esta é a lacuna crítica identificada no `obsidian-promise` original (zero
testes) que este módulo corrige desde o primeiro commit.

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-concurrent).
