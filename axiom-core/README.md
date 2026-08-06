# axiom-core

**Tipos de resultado/erro fundamentais — `Try`, `Result`, `Maybe` — e as
interfaces `Failable*` que os sustentam. Base de todos os outros módulos.**

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
- [Quando usar (e quando não usar)](#quando-usar-e-quando-não-usar)
- [Notas de Design](#notas-de-design)
- [Testes](#testes)
- [Changelog](#changelog)

---

## O que resolve

`axiom-core` oferece três tipos imutáveis para modelar ausência de valor,
falha de domínio e falha de execução — sem recorrer a `null` ou a
`Exception` genérica capturada cedo demais. Também expõe `Failable*`
(`FailableSupplier`, `FailableFunction`, `FailableConsumer`,
`FailableRunnable`), interfaces funcionais que aceitam `throws Exception`,
para escrever lambdas contra APIs Java que lançam checked exceptions sem
precisar de try/catch dentro do lambda.

## O que não resolve

Não é uma biblioteca de validação (para regras de negócio compostas, ver um
futuro `Validator<T>`) nem um logger. `axiom-core` não decide **como** um
erro é tratado — só dá o vocabulário de tipos para representá-lo.

---

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-core:<versão>")
}
```

**Requisitos**: JDK 21+. Zero dependências de terceiros.

---

## Exemplo Rápido

```java
import io.axiom.core.result.Try;
import io.axiom.core.result.Result;
import io.axiom.core.result.Maybe;

// Try — captura de exceção de API Java que lança Throwable
Try<Integer> parsed = Try.of(() -> Integer.parseInt("42"))
        .map(n -> n * 2)
        .recover(NumberFormatException.class, ex -> 0);

// Result — falha de domínio esperada e tipada
Result<Integer, String> validated = validateAge(17);
String message = validated.fold(
        age -> "accepted: " + age,
        error -> "rejected: " + error
);

// Maybe — ausência sem causa associada
Maybe<String> nickname = Maybe.none();
String display = nickname.orElse("anonymous");
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/core/examples/CoreExamples.java`](examples/src/main/java/io/axiom/core/examples/CoreExamples.java).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `Try.of(FailableSupplier<T>)` | Captura exceção de uma operação que pode falhar |
| `Try#map`, `Try#flatMap`, `Try#recover` | Composição e recuperação de falha |
| `Try.retry(int, Duration, Supplier)`, `Try.retryWithBackoff(...)` | Reexecução da operação original em caso de falha |
| `Result.ok(T)` / `Result.err(E)` | Constrói um resultado de sucesso ou falha tipada |
| `Result#fold(Function<T,R>, Function<E,R>)` | Desestrutura sem `instanceof` manual |
| `Maybe.some(T)` / `Maybe.none()` | Presença/ausência, alternativa a `Optional` com API própria |
| `Failable{Supplier,Function,Consumer,Runnable}` | Interfaces funcionais com `throws Exception` |
| `HumanDuration.format(Duration)` / `.parse(String)` | Conversão `Duration` ↔ forma legível (`"2h 30m"`) |
| `TypeReference<T>` | "Super type token" para decodificar em tipos genéricos; usado por `axiom-json`/`axiom-yaml` |

---

## Quando usar (e quando não usar)

| Situação | Tipo |
|---|---|
| Ausência de valor sem causa a comunicar | `Maybe<T>` |
| Falha de domínio esperada, que quem chama precisa tratar de forma tipada | `Result<T,E>` |
| Chamar uma API Java que lança `Throwable`/checked exception | `Try<T>` |

Esta tabela existe **exatamente** para evitar o problema identificado na
auditoria original: `Maybe`, `Result` e `Try` convivendo sem fronteira de
uso documentada. Qualquer ambiguidade nova encontrada em uso real deve ser
resolvida aqui, não deixada implícita.

---

## Notas de Design

- `Try<T>` funde os pontos fortes de duas implementações anteriores
  (`retryWithBackoff`, `sequence`, `traverse`, `combine`, `withTimeout` de
  uma; `mapTry`, `checkedGet()`, `recover` tipado por classe de exceção da
  outra). O bug conhecido de uma versão anterior de `retry(int)` de
  instância (não reexecutava o supplier original) **não foi portado** — só
  os dois pontos de entrada estáticos que sempre reexecutam o supplier
  original (`retry(int, Duration, Supplier)`, `retryWithBackoff(...)`)
  existem nesta implementação, cobertos por teste de regressão específico.
- `Result<T,E>` e `Maybe<T>` são `sealed interface` (Java 21), garantindo
  exaustividade em `switch` pattern matching no ponto de consumo.
- Nenhum estado estático mutável em nenhum tipo deste módulo — todos são
  imutáveis após construção.
- **`HumanDuration` vive aqui em vez de um módulo `axiom-datetime`**: avaliado com o crivo de
  `axiom.md` ("isso a JDK já não resolve?"). Formatação de duração legível
  (`"2h 30m"`, algo que `Duration#toString()` de fato não oferece — produz ISO-8601 como
  `"PT2H30M"`) é a única lacuna real identificada; volume de API insuficiente para justificar
  módulo próprio. Um wrapper `DateTime` sobre `ZonedDateTime` (como o do JToolBox,
  ~1500 linhas) foi deliberadamente **não portado** — reinventa uma API que o `java.time` já
  resolve bem desde o Java 8. `axiom-text` (formatação de string com placeholders `{}` estilo
  SLF4J) também não foi criado: a implementação de origem é fortemente acoplada a subsistemas já
  descartados da Axiom (`control/`, `logger/`), e o ganho sobre `String.format`/`java.text` não
  se justificou nesta avaliação.
- **`TypeReference<T>` (Etapa 6, revisão de nomenclatura)**: `axiom-json` e `axiom-yaml`
  reimplementavam, cada um de forma independente, a mesma classe "super type token" com o mesmo
  nome simples resolvendo o mesmo problema em pacotes diferentes — exatamente o padrão que
  `axiom.md` proíbe ("um nome, um conceito"). Consolidada aqui, já que ambos os módulos já
  dependem de `axiom-core`; cada módulo mantém sua própria interface `JsonMapper`/`YamlMapper`,
  só o tipo de captura de generics é compartilhado.

---

## Testes

```bash
./gradlew :axiom-core:test
```

Cobertura obrigatória: caminho de sucesso, caminho de falha, e
recuperação/composição encadeada (`map` após `recover`, etc.) para os três
tipos. Teste de regressão específico para o bug de `retry()` corrigido
(`TryTest#staticRetryReinvokesOriginalSupplierEachAttempt`).

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-core).
