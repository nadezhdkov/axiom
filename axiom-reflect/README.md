# axiom-reflect

**Reflection fluente com cache de lookup — acesso a campos/métodos/anotações
sem `try/catch` manual em torno de exceções checked da API de reflection.**

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

`Reflect.on(Class)`/`Reflect.on(Object)` dá acesso fluente a campos, métodos
e anotações, com exceções checked (`IllegalAccessException`,
`NoSuchFieldException`, ...) convertidas em uma única exceção unchecked
(`ReflectException`). `ReflectBuilder<T>` permite construir/reconfigurar
instâncias por nome de campo.

## O que não resolve

Não é um framework de mapeamento objeto-dados — para JSON/YAML/`.env`, ver
`axiom-json`/`axiom-yaml`/`axiom-dotenv`, que usam este módulo internamente
em vez de reimplementar acesso a campo com `java.lang.reflect.Field` cru.

---

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-reflect:<versão>")
}
```

**Requisitos**: JDK 21+. Zero dependências de terceiros.

---

## Exemplo Rápido

```java
import io.axiom.reflect.Reflect;

class User {
    private String name;
    private int age;
}

User user = Reflect.on(User.class).create();
Reflect.on(user).field("name").set("Ada");

String name = Reflect.on(user).field("name").get();
var fieldNames = Reflect.on(user).fields().notStatic().names();
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/reflect/examples/ReflectExamples.java`](examples/src/main/java/io/axiom/reflect/examples/ReflectExamples.java).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `Reflect.on(Class<?>)` / `Reflect.on(Object)` | Ponto de entrada fluente |
| `Reflect#field(String)` / `#fields()` | Acesso a um campo, ou consulta fluente filtrável sobre todos |
| `Reflect#method(String)` / `#methods()` | Invocação de método, ou consulta fluente filtrável |
| `Reflect#annotations()` | Consulta de anotações de tipo |
| `ReflectBuilder.of(Class)` / `.from(instance)` | Construção/reconfiguração por nome de campo |

---

## Notas de Design

- Pacote é `io.axiom.reflect`, nunca `lang.reflect` — a implementação de
  referência usava esse nome e colidia com `java.lang.reflect`, obrigando a
  qualificação explícita em todo o código-fonte dela mesma. Correção
  deliberada, ver `docs/architecture.md §4`.
- **Cache de lookup** (`internal.LookupCache`): `Field`/`Method` resolvidos
  por `(classe, nome[, tipos de parâmetro])` são armazenados em
  `ConcurrentHashMap` e nunca reavaliados — lacuna confirmada na auditoria
  original (nenhuma versão anterior cacheava lookups). O cache nunca expira:
  membros refletidos são limitados pelo conjunto de classes carregadas, não
  por volume de chamadas.
- `Field#removeFinal()` da implementação original **não foi portado**: a
  técnica (reescrever o campo `modifiers` de `java.lang.reflect.Field` via
  reflection) já não funciona em JDKs modernos (encapsulamento forte desde o
  Java 12+) — não faz sentido portar uma API que falha em tempo de execução
  no piso mínimo de Java que a Axiom suporta.
- **Bug corrigido**: a varredura de hierarquia de classes (`fields()`,
  `methods()`, lookup por nome) da implementação original ia até
  `Object.class` inclusive, chamando `setAccessible(true)` em membros
  internos da JDK (ex.: `registerNatives`) — sob JPMS, isso lança
  `InaccessibleObjectException` sem `--add-opens`. A varredura agora para
  antes de `Object.class`, coberto por teste (todo o suite de `axiom-reflect`
  roda como módulo JPMS, então esse caminho é sempre exercido).

---

## Testes

```bash
./gradlew :axiom-reflect:test
```

A implementação de referência não tinha nenhum teste para este módulo —
suíte nova cobrindo acesso fluente a campo/método, filtros de `fields()`/
`methods()`, unificação de exceções, `ReflectBuilder`, e uma verificação
explícita de que o cache de lookup está de fato sendo usado (mesma
instância de `java.lang.reflect.Field` servida para lookups repetidos da
mesma chave).

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-reflect).
