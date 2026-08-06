# axiom-csv

**Parsing e escrita de CSV com modelo próprio (`CsvDocument`/`CsvRow`) — a JDK não tem nenhum
suporte a CSV, e `String.split(",")` não trata aspas, delimitadores/quebras de linha embutidos
nem aspas escapadas.**

<p align="left">
  <img src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=flat-square" alt="Status"/>
  <img src="https://img.shields.io/badge/depende%20de-nenhum%20m%C3%B3dulo%20axiom-blue?style=flat-square" alt="Dependências"/>
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

Parsing CSV correto (RFC 4180 + tolerância prática): campos entre aspas contendo delimitador,
aspas escapada (`""`) ou quebra de linha embutida; `\r\n` e `\n` aceitos como fim de linha no
mesmo documento; delimitador configurável (`;` é comum em locales pt-BR/europeus). Diferente de
`axiom-json`/`axiom-yaml`, não há motor de terceiros escondido atrás de `internal.*` — CSV é
simples o bastante para a implementação própria em `internal.CsvParser`/`internal.CsvWriter` ser
o módulo inteiro.

## O que não resolve

- Não é uma API de streaming — o documento inteiro é carregado em memória (`String`/`Reader` lido
  por completo antes do parse). Adequado para arquivos de tamanho comum; não para CSVs de
  gigabytes. Ver "Notas de Design".
- Não infere tipos de coluna (número, data, etc.) — todo campo é `String`; conversão fica por
  conta de quem consome `CsvDocument`.
- Não detecta cabeçalho automaticamente — é opt-in explícito via `CsvConfig.withHeader(true)`,
  nunca um palpite (mesma disciplina de "nenhum comportamento implícito" usada em `Endian`
  explícito em `axiom-numeric`).

---

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-csv:<versão>")
}
```

**Requisitos**: JDK 21+. Zero dependências de terceiros e zero dependência de outro módulo
Axiom.

---

## Exemplo Rápido

```java
import io.axiom.csv.Csv;
import io.axiom.csv.CsvConfig;
import io.axiom.csv.CsvDocument;

CsvDocument document = Csv.parse("""
    name,city
    Ricardo,São Paulo
    "Jane, Doe","New York"
    """, CsvConfig.defaults().withHeader(true));

String firstName = document.get(0, "name");
String rewritten = Csv.write(document, CsvConfig.defaults().withHeader(true));
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/csv/examples/CsvExamples.java`](examples/src/main/java/io/axiom/csv/examples/CsvExamples.java).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `Csv.parse(String\|Reader, CsvConfig?)` | Parse para `CsvDocument` |
| `Csv.write(CsvDocument, Writer?, CsvConfig?)` | Escreve, citando só os campos que precisam |
| `CsvConfig` | Delimitador, caractere de aspas, presença de cabeçalho — todos explícitos |
| `CsvDocument#get(int, String)` | Acesso por linha + nome de coluna (requer cabeçalho) |
| `CsvRow#get(int)` / `#fields()` | Acesso por índice |
| `CsvException` | Raiz de exceção do módulo (aspas não fechadas, coluna desconhecida, etc.) |

---

## Quando usar (e quando não usar)

Use para ler/escrever arquivos CSV reais (com aspas, delimitador customizado, Unicode) de forma
correta sem reimplementar o parser à mão. Não use como banco de dados em memória ou fonte de
streaming para arquivos muito grandes — o módulo carrega tudo de uma vez.

---

## Notas de Design

- **Carrega o documento inteiro em memória antes de parsear** — decisão deliberada de
  simplicidade para a primeira versão, documentada honestamente em vez de escondida (mesmo
  precedente de `ChunkedPVector` documentando sua limitação em vez de fingir ser uma RRB-tree
  completa). Streaming fica para uma versão futura, se houver demanda real.
- **Cabeçalho é sempre opt-in** (`CsvConfig.withHeader(true)`) — nunca inferido pela forma da
  primeira linha, para não haver comportamento implícito ambíguo.
- **Escrita cita só o necessário**: um campo só é envolto em aspas se contiver o delimitador, o
  caractere de aspas ou uma quebra de linha — mantém a saída legível quando não há necessidade
  de aspas.
- **Sem dependência de nenhum outro módulo Axiom**: não há uso de `Try`/`Result`/`Maybe` de
  `axiom-core` aqui, então a dependência não é declarada.

---

## Testes

```bash
./gradlew :axiom-csv:test
```

Cobertura: campo simples, campo com delimitador entre aspas, aspas escapada, quebra de linha
embutida em campo, linha vazia (distinta de fim de documento), documento vazio, `CRLF`/`LF`
misturados no mesmo documento, delimitador customizado, conteúdo Unicode, aspas não fechadas
(`CsvException`), acesso por nome de coluna com e sem cabeçalho, parse a partir de `Reader`,
round-trip `Csv.write(Csv.parse(x))` preservando conteúdo semântico, e escrita citando só os
campos que precisam.

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-csv).
