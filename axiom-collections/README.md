# axiom-collections

**Coleções persistentes/imutáveis: mapa e set via HAMT, vetor em chunks, pilha em cons-list,
fila de duas pilhas amortizada, e variantes ordenadas sobre `TreeMap`/`TreeSet`.**

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

Estruturas de dados que nunca mudam depois de criadas: toda operação de "escrita"
(`plus`/`minus`) retorna uma **nova** coleção, preservando a anterior intacta e compartilhando a
maior parte da estrutura interna. O módulo cobre toda a família prevista na Etapa 1:

| Família | Tipo | Implementação | Estrutura |
|---|---|---|---|
| Mapa | `PMap<K,V>` | `HashTrieMap` | HAMT |
| Set | `PSet<E>` | `HashTrieSet` | HAMT |
| Vetor | `PVector<E>` | `ChunkedPVector` | chunks fixos de 32 |
| Pilha | `PStack<E>` | `ConsPStack` | cons-list |
| Fila | `PQueue<E>` | `AmortizedPQueue` | duas pilhas (Okasaki) |
| Mapa ordenado | `PSortedMap<K,V>` | `TreePMap` | `TreeMap` + snapshot |
| Set ordenado | `PSortedSet<E>` | `TreePSet` | `TreeSet` + snapshot |

## O que não resolve

Não é uma coleção mutável de uso geral: para isso, use `java.util.*` normalmente. `ChunkedPVector`
não é uma RRB-tree (ver "Notas de Design" abaixo) — inserção/remoção no meio é `O(n)`, não
`O(log n)`.

---

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-collections:<versão>")
}
```

**Requisitos**: JDK 21+. Zero dependências de terceiros em runtime.

---

## Exemplo Rápido

```java
import io.axiom.collections.*;

PMap<String, Integer> ages = HashTrieMap.<String, Integer>empty()
        .plus("alice", 30)
        .plus("bob", 25);
// ages.plus("carol", 40) retorna um novo PMap; ages continua com 2 entradas

PVector<Integer> vector = PVector.<Integer>empty().plus(1).plus(2).plus(3);
PStack<Integer> stack = PStack.<Integer>empty().plus(1).plus(2).plus(3); // topo = 3
PQueue<Integer> queue = PQueue.<Integer>empty().plus(1).plus(2).plus(3); // head = 1

PSortedMap<Integer, String> scores = TreePMap.<Integer, String>empty()
        .plus(3, "third").plus(1, "first").plus(2, "second");
scores.keySet(); // [1, 2, 3] — sempre em ordem, independente da ordem de inserção
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/collections/examples/CollectionsExamples.java`](examples/src/main/java/io/axiom/collections/examples/CollectionsExamples.java).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `PMap.empty()` / `HashTrieMap.empty()` | Mapa persistente vazio |
| `PSet.empty()` / `HashTrieSet.empty()` | Set persistente vazio |
| `PVector.empty()` / `ChunkedPVector.empty()` | Vetor persistente vazio |
| `PStack.empty()` / `ConsPStack.empty()` | Pilha persistente vazia |
| `PQueue.empty()` / `AmortizedPQueue.empty()` | Fila persistente vazia |
| `PSortedMap.empty([comparator])` / `TreePMap.empty(...)` | Mapa ordenado persistente vazio |
| `PSortedSet.empty([comparator])` / `TreePSet.empty(...)` | Set ordenado persistente vazio |
| `#plus`/`#plusAll`/`#minus`/`#minusAll` | Comuns a toda a família — nunca mutam o receptor |

---

## Notas de Design

- Convenção de nomenclatura do módulo: prefixo `P` ("Persistent") em toda a família de tipos,
  documentado aqui uma única vez e aplicado de forma consistente (`docs/architecture.md §4/§8`).
- **HAMT** (`HashTrieMap`/`HashTrieSet`): mix MurmurHash3, branching factor 32 (5 bits por
  nível), `CollisionNode` para hash total idêntico. `get`/`containsKey`/`plus`/`minus` são
  `O(1)` esperado (limitado por `O(log32 n)` de profundidade). Path copying verificado por teste
  de propriedade (`HashTrieMapPropertyTest`).
- **`ChunkedPVector` não é uma RRB-tree — decisão deliberada, não lacuna esquecida.**
  `axiom.md` coloca isso explicitamente como um gate de decisão ao portar do `ChunkedOVector`
  do Obsidian: implementar uma RRB-tree real, ou portar o design de 2 níveis como está e
  documentar a limitação honestamente. Esta é a segunda opção: `get`/`plus`/`with` são `O(1)`,
  mas `plus(int,E)`/`minus(int)`/`subList` são `O(n)` (reconstroem via lista intermediária) —
  uma RRB-tree real tornaria essas operações `O(log n)`.
- **`ConsPStack`**: `plus` (push no índice 0) é `O(1)` com compartilhamento total da cauda;
  todo o resto (`get`, `with`, inserção/remoção no meio) é `O(n)`, já que precisa percorrer a
  lista encadeada.
- **`AmortizedPQueue`**: fila funcional clássica de duas pilhas (Okasaki) — `plus`/`minus`/`peek`
  são amortizados `O(1)`; o passo caro (inverter `back` em `front`) só acontece quando `front`
  esvazia.
- **`TreePMap`/`TreePSet`**: cada mutação copia o `TreeMap`/`TreeSet` inteiro e produz um novo
  snapshot imutável (`Collections.unmodifiableNavigableMap/Set`) — não há compartilhamento
  estrutural fino como no HAMT, mas o contrato `NavigableMap`/`NavigableSet` completo (incluindo
  `descendingMap`/`descendingSet`, `floor`/`ceiling`/`lower`/`higher`) fica realmente utilizável.
- **Correções em relação ao `obsidian.collections` original**, descobertas ao portar (com testes
  de regressão): `TreeOSet#descendingIterator()` e as sobrecargas de 2 argumentos
  `subMap`/`headMap`/`tailMap`/`subSet`/`headSet`/`tailSet` retornavam `null`
  incondicionalmente (mesmo estando "só" `@Deprecated`, isso é uma armadilha para qualquer
  chamador que siga o contrato padrão de `NavigableMap`/`NavigableSet` — aqui todas delegam de
  verdade); `AmortizedOQueue#element()` também retornava `null` em vez de seguir o contrato de
  `Queue#element()` (lançar `NoSuchElementException` quando vazia).

---

## Testes

```bash
./gradlew :axiom-collections:test
```

Requisito não negociável (`docs/architecture.md §11`): testes de propriedade (via jqwik) para
toda a família, não só o HAMT — path copying/imutabilidade da versão anterior nunca afetada
(`PersistentSequencePropertyTest` para vetor/pilha, `HashTrieMapPropertyTest` para mapa/set),
ordem sempre ordenada independente da ordem de inserção (`TreePMapPropertyTest`), além de
suítes unitárias por tipo cobrindo limites de chunk, FIFO sob enqueue/dequeue intercalados,
navegação (`floor`/`ceiling`/`lower`/`higher`/`descendingSet`), e as regressões de bugs
encontrados durante o port (ver "Notas de Design").

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-collections).
