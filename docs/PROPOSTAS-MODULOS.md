# Propostas — módulos e utilitários candidatos

> **Natureza deste documento.** Log de propostas, não compromisso. Nada aqui é normativo:
> `docs/architecture.md` continua sendo a fonte normativa de fronteiras de módulo, e um item só
> passa a existir de fato quando implementado e registrado em `CHANGELOG.md`. O objetivo é permitir
> julgar cada candidato **vendo o código de uso** antes de decidir construir.
>
> Cada proposta abaixo declara: a lacuna real da JDK, o exemplo de uso, o custo, e — quando aplicável
> — o argumento contra.

Estado do repositório na escrita deste documento: **13 módulos** implementados
(`axiom-core`, `-collections`, `-concurrent`, `-reflect`, `-placeholder`, `-dotenv`, `-json`,
`-yaml`, `-io`, `-console`, `-id`, `-csv`, `-numeric`).

---

## Sumário e recomendação

| # | Candidato | Tipo | Já sancionado? | Recomendação |
|---|---|---|---|---|
| 1 | `Validator<T>` | utilitário em `axiom-core` | ✅ `architecture.md §5` | **Construir** |
| 2 | `Memoized<T>` | utilitário em `axiom-concurrent` | ✅ `architecture.md §5` | **Construir** |
| 3 | `@Buildable` | processador de anotação | ✅ §5 (empacotamento em aberto) | Construir — decidir empacotamento |
| 4 | `axiom-bench` | módulo | ✅ `architecture.md §13` | **Construir primeiro** |
| 5 | `axiom-toml` | módulo | ❌ proposta nova | Avaliar — ver §5.4 |
| 6 | `axiom-experimental` | módulo | ✅ §13 | Adiar até haver API real em incubação |

Os itens 1–4 **não abrem discussão de escopo** — já passaram pelo crivo do `architecture.md` e
simplesmente não foram construídos. Só o item 5 é decisão nova.

---

## 1. `Validator<T>` — validação composável

**Onde:** `axiom-core`, pacote `io.axiom.core.validation` · **Dependências:** nenhuma

### Lacuna

Sancionado em `architecture.md §5` como correção direta de um problema da auditoria: o
`config/core/EnvValidator.java` do JToolBox validava por cadeia de `instanceof`, fechada para
extensão. A JDK não oferece nada composável aqui — Bean Validation (`jakarta.validation`) existe,
mas é pesada, baseada em anotação e exige runtime provider.

### Uso

```java
import io.axiom.core.validation.Validator;
import io.axiom.core.result.Result;

record Signup(String email, int age) { }

Validator<Signup> emailPresent = Validator.check(
        s -> s.email() != null && s.email().contains("@"),
        "email inválido");

Validator<Signup> adult = Validator.check(
        s -> s.age() >= 18,
        "precisa ser maior de idade");

Validator<Signup> rules = emailPresent.and(adult);

// Acumula TODAS as falhas — não para na primeira.
Result<Signup, List<String>> r = rules.validate(new Signup("x", 15));

String out = r.fold(
        ok  -> "ok: " + ok.email(),
        err -> "falhou: " + String.join("; ", err));
// → "falhou: email inválido; precisa ser maior de idade"
```

### Decisão de design a tomar

O ponto interessante é **acumular vs. curto-circuito**. `and` que para na primeira falha é trivial
mas inútil em formulário; acumular exige que o tipo de erro seja um `List<E>`, o que muda a
assinatura. Recomendação: acumular por padrão (`and`), com `andThen` explícito para curto-circuito
quando a segunda regra depende da primeira ter passado.

Integração com `axiom-reflect` para uso declarativo por anotação fica **fora** desta primeira
versão — `architecture.md §5` já a descreve como "combinável depois", não agora.

---

## 2. `Memoized<T>` — memoização thread-safe

**Onde:** `axiom-concurrent`, pacote `io.axiom.concurrent.memo` · **Dependências:** nenhuma

### Lacuna

Sancionado em `architecture.md §5`. A JDK tem `Map#computeIfAbsent`, mas ele não resolve o caso de
**valor único e caro, calculado no máximo uma vez sob concorrência**, nem oferece invalidação. O
idioma manual é double-checked locking com `volatile` — exatamente o tipo de código que uma
biblioteca deve encapsular porque quase todo mundo escreve errado.

Encaixa em `axiom-concurrent` porque o módulo já é dono de `Box`/`AtomicBox` — o vocabulário de
"célula de valor com semântica de concorrência" já existe ali.

### Uso

```java
import io.axiom.concurrent.memo.Memoized;

// Supplier caro, chamado no máximo uma vez mesmo com N threads concorrendo.
Memoized<Config> config = Memoized.of(() -> carregarDoDisco());

Config c1 = config.get();   // calcula
Config c2 = config.get();   // devolve o mesmo, sem recalcular
assert c1 == c2;

config.invalidate();
Config c3 = config.get();   // recalcula
```

E a variante por chave, que é onde `computeIfAbsent` de fato falha (ele pode executar a função
mais de uma vez sob contenção em alguns cenários, e trava o mapa inteiro):

```java
Memoized.Keyed<UserId, User> users = Memoized.keyed(this::buscarNoBanco);
User u = users.get(new UserId(42));
```

### Argumento contra

Caffeine resolve isso e muito mais. Mas `architecture.md` já traçou essa linha: cache **com
política de evicção** fica de fora por não competir com solução madura; memoização simples e sem
evicção é utilitário, não cache. Manter `Memoized` sem TTL e sem tamanho máximo é o que mantém a
decisão coerente — **se surgir vontade de adicionar evicção, a resposta é usar Caffeine.**

---

## 3. `@Buildable` — cópia parcial de records

**Onde:** empacotamento em aberto — `axiom-codegen` (módulo novo) vs. `annotationProcessor` dentro
de `axiom-core`

### Lacuna

Records não têm o `.copy()` do Kotlin. Alterar um campo de um record de 6 componentes obriga a
reescrever os 6 no construtor, e o compilador não avisa se você trocar a ordem de dois campos do
mesmo tipo — é um bug silencioso real.

### Uso

```java
import io.axiom.codegen.Buildable;

@Buildable
public record Server(String host, int port, Duration timeout, boolean tls) { }
```

Gerado em tempo de compilação:

```java
Server base = new Server("localhost", 8080, Duration.ofSeconds(30), false);

Server prod = base.withHost("api.exemplo.com")
                  .withTls(true);

// ou via builder, para construção do zero
Server s = Server.builder()
                 .host("localhost")
                 .port(8080)
                 .timeout(Duration.ofSeconds(30))
                 .tls(false)
                 .build();
```

### A decisão de empacotamento

`architecture.md §5` deixou isso explicitamente em aberto. O argumento decisivo: um processador de
anotação é dependência **de tempo de compilação**, e o consumidor a declara com
`annotationProcessor(...)`, não `implementation(...)`. Enfiá-lo em `axiom-core` obrigaria todo
consumidor de `Result`/`Try` a arrastar um processador que não vai usar.

**Recomendação: módulo `axiom-codegen` separado**, com a anotação `@Buildable` e o processador
juntos. Custo: é o primeiro módulo do projeto com `annotationProcessor`, então o
`scripts/audit-deps.sh` provavelmente precisa aprender essa configuração — hoje ele só olha
`implementation(libs.x)`.

---

## 4. `axiom-bench` — benchmarks JMH

**Onde:** módulo novo, já previsto em `architecture.md §13` · **Dependências:** JMH + os módulos medidos

### Por que este vem primeiro

É a proposta com maior retorno imediato, porque **valida afirmações que o repositório já faz**:

1. `axiom-collections` entrega HAMT (`HashTrieMap`/`HashTrieSet`), `ChunkedPVector`,
   `AmortizedPQueue`, `ConsPStack`. Todos têm características de performance *afirmadas* na
   documentação e **nunca medidas**. "Amortized" e "Chunked" são promessas de complexidade — sem
   bench, são folclore.
2. `architecture.md §6` manda reimplementar a heurística do `Dynamic` (JToolBox) "com medição real,
   em vez de threshold fixo". **Essa instrução é literalmente inexecutável sem um módulo de bench.**
3. `axiom-numeric` faz operações de overflow checked/wrapping/saturating — o custo relativo delas
   contra aritmética crua é exatamente o tipo de coisa que o consumidor precisa saber.

### Uso

```java
package io.axiom.bench.collections;

import io.axiom.collections.HashTrieMap;
import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class HashTrieMapBench {

    @Param({"10", "1000", "100000"})
    int size;

    HashTrieMap<Integer, Integer> trie;
    java.util.Map<Integer, Integer> hashMap;

    @Setup
    public void setup() { /* popula ambos com `size` entradas */ }

    @Benchmark
    public Object trieInsert()    { return trie.put(size + 1, 0); }

    @Benchmark
    public Object hashMapInsert() { return hashMap.put(size + 1, 0); }
}
```

### Custo e cuidado

- **Não entra no `./gradlew build`.** JMH leva minutos; o bench roda por task própria
  (`./gradlew :axiom-bench:jmh`). O `Makefile` ganha um alvo separado.
- **Nunca é publicado no Maven Central.** Precisa de `mavenPublishing { }` desabilitado — é o
  primeiro módulo do repo que não é artefato público, então a configuração no `build.gradle.kts`
  raiz (hoje aplicada a *todos* os subprojetos) precisa de uma exceção.

---

## 5. `axiom-toml` — proposta nova

**Onde:** módulo novo · **Dependências:** `axiom-core`, `axiom-reflect` (espelhando `axiom-json`)

### 5.1 Lacuna

A JDK não tem parser de TOML. O formato virou padrão de fato em configuração de ferramentas —
inclusive **neste repositório**, em `gradle/libs.versions.toml`.

### 5.2 Por que encaixa

O padrão já está validado três vezes: `axiom-json` (Gson escondido), `axiom-yaml` (SnakeYAML
escondido), `axiom-csv` (implementação pura). `axiom-toml` seria o quarto membro da mesma família,
com a mesma forma de API — nenhum conceito novo entra no projeto.

### 5.3 Uso

Espelhando a fachada de `axiom-json` (`Json.defaultMapper()` → `decode(source, TypeReference<T>)`):

```java
import io.axiom.toml.Toml;
import io.axiom.core.type.TypeReference;

record Database(String host, int port, List<String> replicas) { }
record AppConfig(String name, Database database) { }

String src = """
    name = "axiom"

    [database]
    host = "localhost"
    port = 5432
    replicas = ["r1", "r2"]
    """;

AppConfig cfg = Toml.defaultMapper().decode(TomlSource.of(src), AppConfig.class);

cfg.database().port();      // 5432
cfg.database().replicas();  // ["r1", "r2"]
```

Acesso sem mapear para tipo, quando o schema não é conhecido:

```java
TomlDocument doc = Toml.parse(src);
int port = doc.getInt("database.port").orElse(5432);
```

### 5.4 Argumento contra — leia antes de aprovar

1. **Consumo real ou simetria estética?** O risco é construir porque "falta a peça TOML na
   coleção", não porque alguém precisa ler TOML em Java. YAML e JSON aparecem em API e config de
   aplicação; TOML aparece majoritariamente em *tooling* (Cargo, pyproject, Gradle catalogs) — e
   ferramenta em Java raramente lê TOML.
2. **Escolha de motor.** Não há um "Gson do TOML" dominante. As opções (`toml4j` abandonado,
   `tomlj`, `night-config`) são todas menos maduras que Gson/SnakeYAML. Implementação pura, como em
   `axiom-csv`, é viável mas TOML é **bem** mais complexo que CSV — datas, tabelas aninhadas,
   arrays de tabelas, tipagem forte. É semanas, não dias.
3. Se a resposta a (1) for "não tenho caso de uso concreto agora", a decisão coerente com o resto
   do projeto é **não construir** — foi exatamente assim que `axiom-text`/`axiom-datetime` foram
   barrados.

**Recomendação:** só aprovar contra um caso de uso real. É o único item deste documento onde a
resposta padrão deveria ser "não ainda".

---

## 6. `axiom-experimental` — adiar

Previsto em `architecture.md §13` para APIs em incubação. Criar um módulo vazio à espera de
conteúdo é cerimônia — o próprio valor do módulo (tornar o compromisso de estabilidade visível na
dependência declarada) só existe quando há uma API instável real morando nele. **Reavaliar quando
houver a primeira.**

---

## O que foi considerado e descartado nesta rodada

Registrado para não ser reproposto — nenhum destes reverte decisão anterior, apenas a reafirma:

| Candidato | Motivo |
|---|---|
| `axiom-http` (cliente) | `java.net.http.HttpClient` (JDK 11+) já resolve; e chega perto do escopo de framework barrado em §3 |
| `axiom-log` (facade) | SLF4J domina — mesmo argumento usado para barrar cache com evicção: não competir com solução madura |
| `axiom-xml` | JAXP/DOM já na JDK; API ruim, mas "ruim" não é "ausente" |
| `axiom-math` (`BigDecimal`, `Fraction`, `Money`) | Já explicitamente barrado em §5, na descrição de `axiom-numeric` |
| `axiom-di` | Fora de escopo por princípio (§3). Proposta separada, em repositório próprio — ver o projeto **Cradle** |
