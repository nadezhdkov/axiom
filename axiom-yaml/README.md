# axiom-yaml

**Modelo de dados YAML próprio (`YamlNode`/`YamlMapping`/`YamlSequence`/`YamlScalar`) com motor
de parsing/serialização (SnakeYAML) escondido atrás de `internal.snakeyaml.*`, espelhando a
arquitetura já validada em `axiom-json`.**

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

`Yaml.defaultMapper()` expõe `YamlMapper#parse/decode/encode/stringify` sem jamais vazar um tipo
SnakeYAML na API pública. Mapeamento objeto↔YAML via anotações (`@YamlName`, `@YamlIgnore`,
`@YamlDefault`, `@YamlRequired`), usando `axiom-reflect` internamente — mesmo padrão de contrato
usado em `axiom-json`, porém com sua própria implementação (`internal.snakeyaml.ObjectBinder`),
já que o DAG do projeto não prevê `axiom-yaml` dependendo de `axiom-json`. Acesso por path
pontuado (`YamlMapping#getPath("db.host")`, `getString`/`getInt`/`getBoolean`/`getDouble`) —
API inspirada em `yaml/YamlConfig.java` do JToolBox, mas reimplementada sobre o modelo de árvore
próprio em vez de navegar um `Map<?,?>` cru.

## O que não resolve (ainda)

- **Preservação de comentários/formatação em round-trip** — meta de v2 explícita em `axiom.md`,
  não bloqueia esta v1. `stringify` sempre reformata via SnakeYAML.
- **`@YamlAdapter`** (codec customizado por campo, equivalente ao `@JsonAdapter` de `axiom-json`)
  não foi incluído nesta primeira fatia — o conjunto de anotações portado é o subconjunto que já
  cobre o uso real de configuração (`@YamlName`/`@YamlIgnore`/`@YamlDefault`/`@YamlRequired`).

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-yaml:<versão>")
}
```

**Requisitos**: JDK 21+. Depende de `axiom-core`, `axiom-reflect` e SnakeYAML (única dependência
de terceiros do módulo, escondida atrás de `internal.snakeyaml.*`).

---

## Exemplo Rápido

```java
import io.axiom.yaml.*;
import io.axiom.yaml.annotations.*;
import io.axiom.yaml.io.YamlSource;

class ServerConfig {
    @YamlName("host_name") String host;
    @YamlDefault("8080") int port;
    @YamlRequired String apiKey;
}

YamlMapper mapper = Yaml.defaultMapper();
String yaml = mapper.toYaml(config);
ServerConfig decoded = mapper.decode(YamlSource.of(yaml), ServerConfig.class);

YamlNode tree = mapper.parse(YamlSource.of("db:\n  host: localhost"));
tree.asMapping().getString("db.host"); // "localhost"
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/yaml/examples/YamlExamples.java`](examples/src/main/java/io/axiom/yaml/examples/YamlExamples.java).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `Yaml.defaultMapper()` / `Yaml.configure()` | Pontos de entrada |
| `YamlMapper#parse/decode/encode/stringify/toYaml` | Conversões entre texto, árvore e objeto Java |
| `YamlNode`/`YamlMapping`/`YamlSequence`/`YamlScalar`/`YamlNull` | Modelo de árvore |
| `YamlMapping#getPath/getString/getInt/getBoolean/getDouble` | Acesso por path pontuado |
| `@YamlName`/`@YamlIgnore`/`@YamlDefault`/`@YamlRequired` | Anotações de mapeamento |
| `YamlSource`/`YamlSink` | Origem/destino tagged-union (String, Reader/Writer, Path) |
| `YamlFiles` | Leitura/escrita direta contra `Path` |

---

## Notas de Design

- **Encapsulamento do motor**: nenhum tipo público retorna ou aceita `org.yaml.snakeyaml.*`;
  `internal.snakeyaml` não é exportado em `module-info.java` — corrige diretamente o
  anti-padrão do `yaml/YamlConfig.java` do JToolBox, que expunha `org.yaml.snakeyaml.Yaml`
  implicitamente.
- **Binder de objetos próprio** (`internal.snakeyaml.ObjectBinder`): o binding objeto↔POJO nativo
  do SnakeYAML não foi reaproveitado porque não dá controle fino suficiente sobre as anotações
  Axiom; o binder percorre campos via `axiom-reflect` recursivamente (POJOs aninhados,
  `List<T>`, `Map<String,V>`, enums), aplicando `@YamlDefault`/`@YamlRequired` da mesma forma que
  `axiom-json` aplica `@JsonDefault`/`@JsonRequired`.
- **`getPath` nunca navega `Map` cru**: ao contrário do `YamlConfig` original do JToolBox
  (`resolveValue` fazia cast não verificado em cada segmento), a resolução caminha sobre
  `YamlMapping`/`YamlNode` tipados.

---

## Testes

```bash
./gradlew :axiom-yaml:test
```

Cobertura: modelo de árvore (ordem de inserção, `deepCopy` independente, path pontuado com
segmento intermediário ausente), round-trip de mapeamento, `@YamlName`/`@YamlIgnore` aplicados na
codificação, `@YamlRequired` lançando em chave ausente, `@YamlDefault` aplicado só quando ausente,
decodificação de coleção genérica (`List<String>`), leitura de path pontuado a partir de uma
árvore recém-parseada.

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-yaml).
