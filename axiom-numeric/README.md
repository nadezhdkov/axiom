# axiom-numeric

**Inteiros de largura fixa (`I8`..`I64`, `U8`..`U64`), overflow explícito (checked/wrapping/
saturating) e leitura/escrita binária com endianness (`Bytes`/`Endian`) — a JDK não tem tipos
unsigned nem semântica de overflow explícita, e `ByteBuffer` resolve bytes/endianness sem
nenhuma expressividade semântica.**

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

Java não tem unsigned, não tem overflow explícito (`int`/`long` estouram silenciosamente por
padrão), e `putShort`/`getShort` de `ByteBuffer` não comunica intenção ("isso é uma porta de
rede"). `axiom-numeric` cobre exatamente essas três lacunas com uma API pequena: 8 tipos de
largura fixa (`I8`/`I16`/`I32`/`I64` signed, `U8`/`U16`/`U32`/`U64` unsigned), três semânticas de
overflow em cada operação aritmética, e `Bytes`/`Endian` para protocolos/formatos binários.

## O que não resolve

- Não compete com `BigInteger`/`BigDecimal` (precisão arbitrária) nem com conceitos de um futuro
  `axiom-math` (`Fraction`, `Money`, estatística) — este módulo representa e manipula valores
  numéricos em nível de representação; `axiom-math` trabalharia com conceitos matemáticos.
- Não substitui `int`/`long` em código Java comum: `I32.of(10).add(I32.of(20))` não deveria
  substituir `10 + 20` quando a largura/overflow explícito não importa para o problema em mãos.
- Não é streaming/zero-copy: `Bytes` é um buffer fixo em memória, não um `ByteBuffer` de alta
  performance para I/O de rede.

---

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-numeric:<versão>")
}
```

**Requisitos**: JDK 21+. Zero dependências de terceiros e zero dependência de outro módulo
Axiom — só usa `java.math.BigInteger`/`java.nio.ByteBuffer`.

---

## Exemplo Rápido

```java
import io.axiom.numeric.*;

U8 version = U8.of(1);
U16 port = U16.of(8080);
U32 packetLength = U32.of(4_000_000_000L);

I32 value = I32.of(300);
I8 wrapped = value.toI8Wrapping();     // 300 mod 256 = 44
I8 saturated = value.toI8Saturated();  // 127 (I8.MAX)
value.toI8Checked();                   // lança ArithmeticException

Bytes packet = Bytes.allocate(8)
    .writeU8(0, 1)
    .writeU16(1, 8080, Endian.BIG);

U8 readVersion = packet.readU8(0);
U16 readPort = packet.readU16(1, Endian.BIG);
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/numeric/examples/NumericExamples.java`](examples/src/main/java/io/axiom/numeric/examples/NumericExamples.java).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `I8`..`I64` | Inteiros signed de largura fixa |
| `U8`..`U64` | Inteiros unsigned de largura fixa (`U32`/`U64` sobre backing `long`) |
| `T.of(...)` | Construção *checked* — lança `ArithmeticException` fora do range |
| `T#add/subtract/multiply(T)` | Aritmética *checked* (padrão) |
| `T#{add,subtract,multiply}Wrapping(T)` | Aritmética modular, sem exceção |
| `T#{add,subtract,multiply}Saturating(T)` | Aritmética que satura no limite do tipo |
| `T#convertChecked/convertWrapping/convertSaturating(target)` | Conversão genérica entre dois tipos quaisquer |
| `T#hasBit/setBit/clearBit/toggleBit/countOnes/leadingZeros/trailingZeros/rotateLeft/rotateRight/and/or/xor/not` | Operações de bit |
| `Bytes.allocate(int)` | Buffer fluente para leitura/escrita binária |
| `Endian.BIG` / `Endian.LITTLE` | Sempre explícito, nunca padrão implícito |

---

## Quando usar (e quando não usar)

Use quando a largura/sinal do número carrega significado (portas, tamanhos de pacote,
timestamps unsigned), quando overflow precisa de tratamento explícito, ou ao ler/escrever
formatos binários com endianness definida. Não use para substituir `int`/`long` comuns onde o
Java já resolve bem — se a largura/sinal do número não carrega significado, um `int`/`long` já
basta.

---

## Notas de Design

- **Overflow: checked por padrão, métodos explícitos para wrapping/saturating.** A proposta
  original deixou em aberto se isso seria representado por factories, tipos distintos ou
  métodos. Decisão tomada na implementação: `add`/`subtract`/
  `multiply` lançam `ArithmeticException` em overflow (mesmo comportamento de
  `Math.addExact`/`Math.multiplyExact`, precedente já existente na própria JDK);
  `addWrapping`/`addSaturating` (e equivalentes) são métodos explícitos alternativos no **mesmo**
  tipo — nunca uma instância "lembrando" uma política implícita, o que seria fonte de bug (o
  mesmo valor se comportando diferente dependendo de como foi construído).
- **Toda a aritmética de overflow passa por `BigInteger`** internamente
  (`io.axiom.numeric.internal.FixedWidth`), em vez de bit-shifting manual através de `int`/`long`.
  Isso custa performance (alocação por operação) em troca de eliminar uma classe inteira de bugs
  sutis de overflow-de-overflow — o mesmo tipo de bug que já apareceu uma vez neste projeto em
  outro módulo por lógica de bits escrita à mão. Aceitável para a v1; se benchmarks futuros
  mostrarem que isso importa, o `axiom-bench` planejado é o lugar para medir antes de otimizar.
- **Conversão entre tipos é genérica, não 56 métodos nomeados.** Em vez de
  `toI8Checked()`/`toU16Wrapping()`/... para cada um dos 8×7 pares possíveis, `FixedWidth`
  expõe `convertChecked(target)`/`convertWrapping(target)`/`convertSaturating(target)`, onde
  `target` é qualquer instância existente do tipo desejado (tipicamente sua constante `MIN`, ex.
  `value.convertChecked(I8.MIN)`) — API pequena e composável em vez de dezenas de sobrecargas
  quase idênticas (`docs/architecture.md §7`, princípio 4). Os três métodos nomeados
  `toI8Checked()`/`toI8Wrapping()`/`toI8Saturated()` em `I32` existem só porque eram o exemplo
  literal do documento de proposta original (já incorporado a este README) — todo outro par usa
  a API genérica.
- **`U32`/`U64` não têm um primitivo Java menor equivalente.** `U32` usa backing `long` (todo o
  range `[0, 4294967295]` cabe confortavelmente); `U64` usa o próprio bit pattern de dois
  complementos de um `long` como representação — todo `long` já é um valor unsigned de 64 bits
  válido, então `U64.of(long)` nunca lança exceção, e a conversão para `BigInteger` usa
  `Long.toUnsignedString` (JDK, desde o Java 8) em vez de reimplementar aritmética unsigned.
- **Bits são operações de baixo nível sobre o bit pattern**, não sobre o "valor" — `leadingZeros`/
  `rotateLeft`/etc. delegam a `Long.numberOfLeadingZeros`/`Long.rotateLeft` da JDK sempre que
  possível, reaproveitando em vez de reimplementar.
- **`Endian` é sempre parâmetro explícito** em toda leitura/escrita multi-byte — nenhum padrão
  implícito, mesma disciplina aplicada ao cabeçalho opt-in de `axiom-csv`.

---

## Testes

```bash
./gradlew :axiom-numeric:test
```

Cobertura: min/max/zero por tipo; rejeição de construção fora do range; as três semânticas de
overflow (checked lança, wrapping dá aritmética modular, saturating limita) para soma/subtração/
multiplicação, incluindo os casos assimétricos unsigned (`U8.MIN.subtract(U8.of(1))` lança,
nunca "wrap para negativo" silenciosamente); `U32` com valor além do range de `int` (exemplo
exato do documento de proposta); `U64` nunca lançando na construção mas lançando em overflow de
soma; conversões nomeadas (`I32.toI8Checked/Wrapping/Saturated`) e genéricas
(`convertChecked/Wrapping/Saturating` entre pares arbitrários, incluindo cruzando
signed↔unsigned); operações de bit (set/clear/toggle — incluindo o bit de sinal de um tipo
signed —, count/leading/trailing zeros, rotate, AND/OR/XOR/NOT) com composição encadeada;
`Bytes` — leitura/escrita BIG e LITTLE (incluindo verificação de que produzem bytes diferentes),
buffer vazio, índice fora dos limites, e a cadeia exata do exemplo do documento de proposta
(`writeU8(0,1).writeU16(1,8080,BIG)`) validada como operação composta.

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-numeric).
