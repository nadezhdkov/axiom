# axiom-placeholder

**Resolução de placeholders/templates de string (`${key}`, `${key:default}`,
`${key|transform}`) contra uma fonte plugável — com detecção obrigatória de
referência circular.**

<p align="left">
  <img src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=flat-square" alt="Status"/>
  <img src="https://img.shields.io/badge/depende%20de-axiom--core-blue?style=flat-square" alt="Dependências"/>
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

Módulo novo, sem equivalente direto nas bibliotecas auditadas — a
resolução de placeholder aparecia reimplementada de forma independente em
pelo menos três lugares diferentes (parser `{}` estilo SLF4J, resolvedor de
`${prop:default}`, formatação de I/O com sintaxe própria). `PlaceholderResolver`
consolida isso em uma peça pequena e reutilizável, consumida deliberadamente
por `axiom-dotenv` (e, no futuro, `axiom-yaml`) em vez de cada módulo
reimplementar seu próprio mini-parser.

## O que não resolve

Não suporta aninhamento (`${outer.${inner}}`) na v1 — fica documentado como
recurso avançado, não bloqueante. Não é um motor de template completo
(sem laços, condicionais, etc.) — só substituição de placeholders.

---

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-placeholder:<versão>")
}
```

**Requisitos**: JDK 21+. Depende só de `axiom-core` (para a hierarquia de
exceções `AxiomException`).

---

## Exemplo Rápido

```java
import io.axiom.placeholder.PlaceholderResolver;
import java.util.Map;

var resolver = PlaceholderResolver.of(Map.of(
        "host", "localhost",
        "port", "5432"
));

resolver.resolve("jdbc:postgresql://${host}:${port}/${db:mydb}");
// "jdbc:postgresql://localhost:5432/mydb"

resolver.resolve("Hello, ${name|upper}!"); // aplica transformação registrada
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/placeholder/examples/PlaceholderExamples.java`](examples/src/main/java/io/axiom/placeholder/examples/PlaceholderExamples.java).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `PlaceholderSource.of(Map)` / `.environment()` / `.systemProperties()` / `.function(...)` | Fontes de resolução plugáveis |
| `PlaceholderSource#orElse(PlaceholderSource)` | Encadeia fontes com fallback |
| `PlaceholderResolver.of(PlaceholderSource\|Map)` | Cria o resolvedor |
| `PlaceholderResolver#resolve(String)` | Resolve todos os placeholders de um template |
| `PlaceholderResolver#withTransform(String, UnaryOperator<String>)` | Registra transformação custom para `\|nome` |

Transforms embutidos: `upper`, `lower`, `trim`.

---

## Notas de Design

- **Detecção de referência circular é obrigatória, não opcional**: o valor
  resolvido de uma chave é recursivamente reinterpolado (para suportar
  `a="${b}"`, `b="valor"`), e uma pilha de chaves em resolução detecta
  ciclos (`${a}` → `${b}` → `${a}`) lançando
  `CircularPlaceholderReferenceException` em vez de estourar a stack.
- Sem fonte + sem `:default` → `UnresolvedPlaceholderException` (falha
  explícita, não substituição silenciosa por string vazia).
- Gramática de conteúdo: `key[:default][|transform]`, nessa ordem fixa —
  simplifica o parser às custas de não permitir `:`/`|` livres dentro da
  chave (aceitável para chaves de configuração).
- Placeholders aninhados (`${outer.${inner}}`) ficam fora do escopo da v1,
  documentado deliberadamente (`docs/architecture.md §5`).

---

## Testes

```bash
./gradlew :axiom-placeholder:test
```

Teste obrigatório e não negociável (`docs/architecture.md §11`): detecção de
referência circular — direta (`a↔b`), auto-referência (`a→a`), indireta
através de múltiplas chaves (`a→b→c→a`), e verificação de que resolução
legítima em "diamante" (duas chaves compartilhando uma terceira, sem ciclo)
**não** é falsamente sinalizada como circular.

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-placeholder).
