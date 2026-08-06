# axiom-json

**Modelo de dados JSON próprio (`JsonElement`/`JsonObject`/`JsonArray`/`JsonPrimitive`/`JsonNull`)
com motor de parsing/serialização (Gson) escondido atrás de `internal.gson.*`.**

<p align="left">
  <img src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=flat-square" alt="Status"/>
  <img src="https://img.shields.io/badge/depende%20de-axiom--core%2C%20axiom--reflect-blue?style=flat-square" alt="Dependências"/>
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

`Json.defaultMapper()` (ou `Json.configure()...buildMapper()`) expõe `JsonMapper#parse/decode/encode/stringify`
sem jamais vazar um tipo Gson na API pública — nenhuma classe fora de `internal.gson.*` importa
`com.google.gson.*`. Mapeamento objeto↔JSON via anotações (`@JsonName`, `@JsonIgnore`,
`@JsonDefault`, `@JsonRequired`, `@JsonAdapter`), usando `axiom-reflect` internamente para
inspecionar campos em vez de reflection crua.

## O que não resolve (ainda)

`@JsonAdapter` (codec customizado por campo) está definido e o codec é resolvido
reflexivamente, mas a injeção automática dele no pipeline de decode/encode do Gson não está
conectada — mesma lacuna documentada na auditoria do `obsidian.json` original, que expunha o
hook (`GsonAnnotationProcessor#getCodecForField`) sem jamais chamá-lo. Diferente de
`@JsonRequired`/`@JsonDefault` (ver "Notas de Design" abaixo), esse gap **não** foi fechado nesta
etapa — para usar um `JsonCodec` hoje, chame-o manualmente antes/depois do mapper.

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-json:<versão>")
}
```

**Requisitos**: JDK 21+. Depende de `axiom-core`, `axiom-reflect` e Gson (única dependência de
terceiros do módulo, escondida atrás de `internal.gson.*`).

---

## Exemplo Rápido

```java
import io.axiom.json.*;
import io.axiom.json.annotations.*;
import io.axiom.json.io.JsonSource;

class ServerConfig {
    @JsonName("host_name") String host;
    @JsonDefault("8080") int port;
    @JsonRequired String apiKey;
}

JsonMapper mapper = Json.defaultMapper();
String json = mapper.toJson(config);
ServerConfig decoded = mapper.decode(JsonSource.of(json), ServerConfig.class);
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/json/examples/JsonExamples.java`](examples/src/main/java/io/axiom/json/examples/JsonExamples.java).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `Json.defaultMapper()` / `Json.configure()` | Pontos de entrada |
| `JsonMapper#parse/decode/encode/stringify/toJson` | Conversões entre texto, árvore e objeto Java |
| `JsonElement`/`JsonObject`/`JsonArray`/`JsonPrimitive`/`JsonNull` | Modelo de árvore imutável quanto à identidade do motor |
| `@JsonName`/`@JsonIgnore`/`@JsonDefault`/`@JsonRequired`/`@JsonAdapter` | Anotações de mapeamento |
| `JsonSource`/`JsonSink` | Origem/destino tagged-union (String, Reader/Writer, Path) |
| `JsonFiles` | Leitura/escrita direta contra `Path` |
| `JsonPrettyPrinter` | Impressão independente do motor (nunca formata via Gson) |

---

## Notas de Design

- **Encapsulamento do motor**: nenhum tipo público retorna ou aceita `com.google.gson.*`;
  `internal.gson` não é exportado em `module-info.java`.
- **`@JsonRequired`/`@JsonDefault` realmente aplicados**: no `obsidian.json` original, o
  processador de anotações expunha `isRequired`/`getDefaultValue` mas nunca os conectava ao
  pipeline de decode — a exceção documentada nunca era de fato lançada. Aqui,
  `internal.gson.AxiomTypeAdapterFactory` intercepta a árvore Gson antes da desserialização
  reflexiva delegada, injeta o valor padrão quando ausente/nulo, e lança
  `JsonValidationException` quando um campo `@JsonRequired` continua ausente.
- **`JsonPrettyPrinter` é próprio**, não delega ao pretty-printer do Gson — garante que a saída de
  `stringify` nunca muda de formato só porque o motor interno mudou.

---

## Testes

```bash
./gradlew :axiom-json:test
```

Cobertura: modelo de árvore (ordem de inserção, `deepCopy` independente, comparação numérica
entre tipos de `Number`), round-trip de mapeamento, `@JsonName`/`@JsonIgnore` aplicados na
codificação, `@JsonRequired` lançando em campo ausente e em campo nulo, `@JsonDefault` aplicado
só quando ausente (nunca sobrescreve valor explícito), saída compacta vs. pretty-print.

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-json).
