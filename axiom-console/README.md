# axiom-console

**Scanner de console testável, graduado de `obsidian.experimental.io.scan` (experimental) para
módulo estável — `InputSource` plugável (`ConsoleSource`/`ReaderSource`/`StringSource`),
`Parser`/`Validator`/`Prompt` separados, leitura não-lançadora via `Result<T, ScanError>`.**

<p align="left">
  <img src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=flat-square" alt="Status"/>
  <img src="https://img.shields.io/badge/depende%20de-axiom--core-blue?style=flat-square" alt="Dependências"/>
</p>

---

## Índice

- [O que resolve](#o-que-resolve)
- [O que não resolve (ainda)](#o-que-não-resolve-ainda)
- [Instalação](#instalação)
- [Exemplo Rápido](#exemplo-rápido)
- [API Principal](#api-principal)
- [Notas de Design](#notas-de-design)
- [Testes](#testes)
- [Changelog](#changelog)

---

## O que resolve

`InputScanner.console()`/`fromReader(Reader)`/`fromString(String)` produzem um `InputHandler`
sobre a fonte de entrada correspondente. `read`/`until` são a forma interativa (lançam
`ParseFailureException`/`ValidationException`, ou repetem o prompt até um valor válido);
`tryRead` é a forma não-lançadora, devolvendo `Result<T, ScanError>` de `axiom-core` — reaproveita
o tipo `Result` já existente em vez de introduzir um segundo conceito "outcome" paralelo.
`StringSource` é o mecanismo de testabilidade: nenhum teste deste módulo toca `System.in`.

## O que não resolve (ainda)

Nenhuma pendência aberta desta etapa — o módulo já nasce "graduado", com a suíte de testes que
faltava no `obsidian.experimental.io.scan` original.

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-console:<versão>")
}
```

**Requisitos**: JDK 21+. Depende apenas de `axiom-core` (para `Result<T,E>`).

---

## Exemplo Rápido

```java
import io.axiom.console.*;
import io.axiom.console.parse.Parsers;
import io.axiom.console.validate.Validators;

InputHandler handler = InputScanner.console();

String name = handler.until("name", Parsers.string(), Validators.notBlank());
int age = handler.until("age", Parsers.i32(), Validators.range(0, 130));

var maybeCount = handler.tryRead("count", Parsers.i32());
maybeCount.fold(
    value -> "count: " + value,
    error -> "failed: " + error.pretty()
);
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/console/examples/ConsoleExamples.java`](examples/src/main/java/io/axiom/console/examples/ConsoleExamples.java) —
> usa `InputScanner.fromString(...)`, o mesmo mecanismo que a suíte de testes usa.

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `InputScanner.console()/fromReader()/fromString()` | Fábrica de `InputHandler` por fonte |
| `InputHandler#read/until` | Leitura interativa (lança ou repete o prompt) |
| `InputHandler#tryRead` | Leitura não-lançadora, devolve `Result<T, ScanError>` |
| `Parsers`/`Validators` | Conjunto padrão de parsers e validadores |
| `Scan` | Fachada estática de conveniência sobre um engine padrão trocável |
| `ScanError`/`ErrorCode` | Classificação da falha em `tryRead` |

---

## Notas de Design

- **`tryRead` fecha uma lacuna real do módulo original**: o `obsidian.experimental.io.scan`
  já definia `ScanResult`/`Error`/`ErrorCode` (a intenção declarada em `axiom.md` era "ScanResult
  com ErrorCode em vez de exceção crua"), mas nenhum caminho de leitura do módulo os usava de
  fato — todo `read`/`until` continuava lançando exceção crua internamente. Aqui, `tryRead` é um
  caminho de leitura de verdade que devolve `Result<T, ScanError>`, e reaproveita o `Result<T,E>`
  já existente em `axiom-core` em vez de duplicar um segundo tipo "outcome".
- **`Scan` é a única exceção documentada ao "sem estado estático mutável"**: o engine padrão é
  guardado em `AtomicReference`, não um campo solto — a troca via `Scan.use(...)` é segura sob
  acesso concorrente. Prefira `InputScanner` diretamente quando quiser uma instância explícita.
- **`ConsoleSource#close()` nunca fecha `System.in`** — é um recurso de processo, não do
  `InputHandler`.
- **Implementação interna isolada**: `ConfigurableLineScanner` vive em `internal`, não exportado
  em `module-info.java` — só alcançável via `InputScanner`.

---

## Testes

```bash
./gradlew :axiom-console:test
```

Cobertura: leitura sequencial via `StringSource`, `hasNextLine` sem consumir o próximo caractere,
`read` lançando em entrada inválida, `until` reprompando até valor válido, `tryRead` cobrindo
sucesso/erro de parse/erro de validação/EOF, `Scan` trocando o engine padrão de forma segura
(incluindo `use(null)` sendo ignorado), parsers (`i32`, `bool` com tokens PT-BR/EN, `ch` exigindo
exatamente um caractere) e validadores (`notBlank`, `range` inclusivo nas bordas).

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-console).
