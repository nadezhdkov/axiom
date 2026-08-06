# axiom-id

**Identificadores ordenáveis por tempo: UUIDv7 (RFC 9562) e ULID — a lacuna que `java.util.UUID`
(só v3/v4/v5) deixa em aberto.**

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

`java.util.UUID` gera v3 (name-based/MD5), v4 (aleatório) e v5 (name-based/SHA-1) — nenhum deles
ordenável pelo momento de criação, o que hoje é comum precisar como chave primária de banco ou
identificador de evento em log distribuído (índices B-tree fragmentam com v4 aleatório; logs
ficam fora de ordem sem um id que já carregue o tempo). `Id.uuidV7()` preenche essa lacuna
gerando UUIDs válidos (mesmo tipo `java.util.UUID` da JDK, mesma representação textual) cujo
prefixo já é um timestamp Unix em milissegundos. `Id.ulid()` oferece a mesma propriedade em um
formato mais compacto e legível (26 caracteres, Base32-Crockford, ordenação lexicográfica =
ordenação cronológica) — formato sem equivalente na JDK.

## O que não resolve

- Não substitui `UUID.randomUUID()` (v4) para os casos em que aleatoriedade pura, sem
  informação de tempo embutida, é desejável (ex.: tokens onde o tempo de criação não deve ser
  inferível externamente).
- Não oferece anotação de injeção de campo (`@GeneratedId` ou similar) para popular ids via
  reflection — isso exigiria depender de `axiom-reflect` sem uso real hoje, e aproximaria o
  módulo de comportamento de ORM, fora do escopo da Axiom (`docs/architecture.md §2`). A API é
  só os dois métodos estáticos de `Id`.
- Não é um gerador de IDs distribuído com coordenação entre nós (tipo Snowflake com máquina/
  worker ID) — cada chamada é independente, a garantia de ordenação é só dentro do mesmo
  processo/instância de `Id`.

---

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-id:<versão>")
}
```

**Requisitos**: JDK 21+. Zero dependências de terceiros e zero dependência de outro módulo
Axiom — só usa `java.util.UUID`/`java.security.SecureRandom`/`java.math.BigInteger`.

---

## Exemplo Rápido

```java
import io.axiom.id.Id;
import io.axiom.id.Ulid;

import java.util.UUID;

UUID orderId = Id.uuidV7();
Ulid traceId = Id.ulid();

Ulid parsed = Ulid.of(traceId.toString());
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/id/examples/IdExamples.java`](examples/src/main/java/io/axiom/id/examples/IdExamples.java).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `Id.uuidV7()` | Novo UUIDv7 (`java.util.UUID`), ordenável por tempo de criação |
| `Id.ulid()` | Novo `Ulid`, 26 caracteres, ordenável por tempo de criação |
| `Ulid.of(String)` | Parse; lança `IllegalArgumentException` em entrada inválida (mesmo contrato de `UUID.fromString`) |
| `Ulid#timestamp()` | `Instant` de criação embutido no ULID |
| `Ulid#compareTo(Ulid)` | Mesma ordem de `toString()` (lexicográfica = cronológica) |

---

## Quando usar (e quando não usar)

Use quando o identificador vira chave primária, índice ordenado, ou precisa preservar ordem de
criação em log/evento. Não use no lugar de `UUID.randomUUID()` quando esconder o momento de
criação é um requisito (tempo embutido é, por definição, extraível de qualquer UUIDv7/ULID).

---

## Notas de Design

- **Monotonicidade dentro do mesmo milissegundo**: duas chamadas a `Id.uuidV7()`/`Id.ulid()` no
  mesmo milissegundo não recebem aleatoriedade independente — a parte aleatória é incrementada
  (RFC 9562 "Método 3"), garantindo que a segunda sempre ordene depois da primeira. O estado
  (último timestamp + último valor) fica atrás de um único `AtomicReference` interno
  (`io.axiom.id.internal.MonotonicClock`), nunca um campo mutável não sincronizado — mesmo
  padrão já usado pela fachada `Scan` de `axiom-console`. Não depende de `axiom-concurrent`
  (`Box`/`AtomicBox`) para isso: um `AtomicReference` puro da JDK já resolve, e adicionar essa
  dependência não teria uso real além deste único ponto.
- **`Id.uuidV7()` devolve `java.util.UUID`, não um wrapper próprio**: o tipo da JDK já é uma
  representação de 128 bits perfeitamente adequada — o que faltava era só a geração correta da
  variante v7, não uma nova classe.
- **`Ulid` é um tipo próprio** porque sua forma canônica (string Base32-Crockford de 26
  caracteres, sem os caracteres `I`/`L`/`O`/`U` para evitar ambiguidade visual) é parte da
  identidade do formato — devolver `String` cru perderia a garantia de igualdade/comparação
  tipada.
- **Sem anotação de injeção de campo**, ver "O que não resolve" acima.

---

## Testes

```bash
./gradlew :axiom-id:test
```

Cobertura: formato de `uuidV7()` (nibble de versão = 7, variant IETF), timestamp embutido
plausível (dentro da janela de geração) para ambos os tipos, monotonicidade em chamadas
sucessivas (1000 IDs gerados em sequência, cada um estritamente maior que o anterior),
unicidade em volume (5000 IDs sem colisão), round-trip `Ulid.of(ulid.toString())`, rejeição de
comprimento/alfabeto inválido em `Ulid.of(...)`.

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-id).
