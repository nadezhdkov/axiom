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
`IO.print`/`IO.println` (`io.axiom.console.print`) fazem o mesmo por saída: `{}` posicional
(estilo Python `.format`) e tags `[tag]...[/]` de cor (`[green]`), fundo (`[bg-green]`) e estilo
(`[bold]`, `[dim]`, `[italic]`, `[underline]`, `[strikethrough]`), combináveis por aninhamento
(`[bold][green]...[/][/]`) e resolvidas em duas passadas — placeholders primeiro, tags depois —
para que o valor de um argumento nunca seja interpretado como tag. Implementação própria, sem
depender de `axiom-placeholder`, para não abrir uma aresta nova no DAG de dependências.

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
import io.axiom.console.print.IO;
import io.axiom.console.validate.Validators;

InputHandler handler = InputScanner.console();

String name = handler.until("name", Parsers.string(), Validators.notBlank());
int age = handler.until("age", Parsers.i32(), Validators.range(0, 130));

var maybeCount = handler.tryRead("count", Parsers.i32());
maybeCount.fold(
    value -> "count: " + value,
    error -> "failed: " + error.pretty()
);

IO.println("Hello {}, you have [bold][green]{}[/][/] new messages", name, age);
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
| `IO.print/println/render` | Saída com placeholders `{}` e tags `[tag]...[/]` (cor/fundo/estilo) |
| `Color` | Cores ANSI para foreground (`[green]`) e background (`[bg-green]`) |
| `TextStyle` | Estilos de texto nas tags (`[bold]`, `[dim]`, `[italic]`, `[underline]`, `[strikethrough]`) |

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
  em `module-info.java` — só alcançável via `InputScanner`. `IO` segue o mesmo padrão: `Tag`
  (o `sealed interface` que unifica foreground/background/estilo) e o parser `TagRenderer` vivem
  em `io.axiom.console.print.internal`; só `Color`/`TextStyle`, os vocabulários públicos de tag,
  são exportados.
- **`IO` reaproveita o padrão `AtomicReference` de `Scan`**: o stream de saída (`System.out` por
  padrão) e o override de estilo são guardados em `AtomicReference`, trocáveis via `IO.use(...)`/
  `IO.setStylingEnabled(...)` sob acesso concorrente; `use(null)` é ignorado, igual a `Scan.use`.
- **Fechamento de tag não tenta desfazer um único atributo — re-renderiza o estado inteiro**:
  ANSI não tem "pop" por atributo, então em vez de guardar o código anterior, `TagRenderer` mantém
  uma pilha de tags abertas e, a cada abertura/fechamento, reemite `ESC[0;...m` com os códigos de
  *todas* as tags ainda na pilha, da mais externa pra mais interna. É por isso que
  `[bold][green]a[red]b[/]c[/]` volta corretamente pra `bold+green` (não pra "sem cor nenhuma")
  depois de fechar o `[red]` interno.
- **Argumento de placeholder nunca vira tag**: `IO.render` resolve `{}` primeiro, mas escapa
  `[`/`]` do valor de cada argumento para sentinelas de uso privado antes de rodar `TagRenderer`
  sobre o texto — só colchetes escritos no próprio template são reconhecidos como tag. Sem isso,
  `IO.println("{}", tokenDoUsuario)` deixaria o valor de entrada injetar estilo arbitrário na
  saída.
- **Estilo com autodetecção, não sempre ligado**: por padrão `IO` só emite ANSI se
  `System.console() != null` (sessão interativa) e `NO_COLOR` não estiver setado — saída
  redirecionada para arquivo/pipe/CI fica limpa sem exigir configuração. `IO.setStylingEnabled(true/
  false)` força o comportamento; `null` volta pra autodetecção.
- **Paleta ficou só nas 8 cores ANSI básicas de propósito** — 256 cores/RGB (`[#ff8800]`) foram
  avaliadas e descartadas nesta rodada por decisão explícita: mais valor vinha de fundo/estilo do
  que de mais tons de cor.

---

## Testes

```bash
./gradlew :axiom-console:test
```

Cobertura: leitura sequencial via `StringSource`, `hasNextLine` sem consumir o próximo caractere,
`read` lançando em entrada inválida, `until` reprompando até valor válido, `tryRead` cobrindo
sucesso/erro de parse/erro de validação/EOF, `Scan` trocando o engine padrão de forma segura
(incluindo `use(null)` sendo ignorado), parsers (`i32`, `bool` com tokens PT-BR/EN, `ch` exigindo
exatamente um caractere) e validadores (`notBlank`, `range` inclusivo nas bordas). `IOTest` cobre
substituição de placeholders (incluindo contagem incompatível de argumentos), tags de
cor/fundo/estilo aninhadas/combinadas/desabilitadas/malformadas, o escape que impede um argumento
de injetar tag, e a escrita via `IO.use(...)`.

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-console).
