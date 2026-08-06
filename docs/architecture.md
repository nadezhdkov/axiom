# Axiom — Projeto Arquitetural

> Este documento é uma proposta de arquitetura, não uma implementação. Tudo aqui é **recomendação para a Axiom**, derivada da auditoria em [`auditoria.md`](../auditoria.md). Onde algo é citado como "observado em JToolBox/Obsidian", é fato levantado na auditoria; onde é decisão de design para Axiom, é proposta nova, sinalizada explicitamente.
>
> **Nota de status**: todas as etapas do roadmap (§14) foram implementadas — este documento permanece como o registro normativo de design (o "porquê"), não como rastreador de status. Para o estado atual da implementação, veja [`README.md`](../README.md) (tabela de módulos, roadmap) e [`CHANGELOG.md`](../CHANGELOG.md) (histórico detalhado por módulo, incluindo bugs corrigidos durante o port).

---

## 1. Objetivo e filosofia

Axiom é uma biblioteca Java 21 de **utilitários e abstrações de uso geral**: peças pequenas, coesas e compostas entre si, que qualquer projeto Java pode adotar parcialmente sem herdar um framework.

A filosofia central, em reação direta aos problemas observados na auditoria:

- **Um axioma é uma verdade mínima que não precisa de prova** — cada módulo da Axiom deve se justificar sozinho, sem depender de "o resto do framework" para fazer sentido. Nada de acoplamento estrutural entre `axiom-collections` e `axiom-http`, por exemplo.
- **Biblioteca, não framework.** Axiom nunca deve oferecer um container de DI, um servidor web, um ORM ou um test runner — esses domínios já têm soluções maduras (Spring, Micronaut, JUnit, jOOQ/JPA) e tentar recriá-los foi o maior erro de escopo identificado no JToolBox (`ioc/`, `http/server`, `jdbc/dao/JdbcTemplate`, `test/`).
- **Composição sobre herança de comportamento.** APIs pequenas que se combinam (`Try`, `Result`, `Maybe`, `Promise`, coleções persistentes) em vez de fachadas gigantes que fazem tudo.
- **Testado por padrão.** Nenhum módulo entra na Axiom sem suíte de testes cobrindo o caminho principal e os casos de borda. Isso ataca diretamente o maior risco identificado na auditoria: estruturas de dados HAMT sem teste, container de DI sem teste, `Promise` concorrente sem teste.
- **Poucas dependências, todas justificadas.** Cada dependência externa declarada deve ter uso confirmado por `grep` antes do merge — evita o problema do ASM morto (JToolBox) e do catálogo com `jackson`/`reflections`/`slf4j` nunca usados (Obsidian).
- **Java 21 como piso, não como enfeite.** Usar `sealed`, records, pattern matching e virtual threads onde eles resolvem um problema real (como Obsidian fez em `Maybe`/`Result` e JToolBox fez no `http/server`), não como diferencial de marketing.

---

## 2. Escopo: o que pertence e o que não pertence à Axiom

### Pertence
- Tipos funcionais de resultado/erro (`Try`, `Result`, `Maybe`/`Optional`-like).
- Coleções persistentes/imutáveis.
- Abstrações de concorrência leves (cancelamento, promises, boxes atômicos).
- Reflection fluente de conveniência.
- Parsing e injeção de configuração externa (env, `.env`, arquivos).
- Utilitários de I/O de arquivos e leitura de console testável.
- Serialização JSON com modelo próprio sobre um motor plugável.
- Serialização YAML com modelo próprio sobre um motor plugável, mesmo padrão do JSON.
- Resolução de placeholders/templates de string (`${key}`, `${key:default}`) como módulo próprio e reutilizável, em vez de reimplementado independentemente dentro de cada módulo que precisa disso.
- Utilitários pontuais que preenchem lacunas reais da JDK (cópia parcial de records, duração legível por humano, memoização simples, validação composável) — avaliados e adicionados individualmente, nunca em bloco.
- Utilitários de texto/número/data-hora que não seja apenas repetir a JDK.

**Decisão explícita: DSLs de controle de fluxo (`If`/`When`/`Condition`/`Switch` fluente) não pertencem à Axiom.** Nenhuma das duas bibliotecas auditadas resolveu esse domínio de forma definitiva — o `switch` de expressão e o pattern matching nativos do Java 21 já cobrem bem o problema que essas DSLs tentavam resolver, sem exigir mais uma API para o consumidor aprender. Ver §5 (módulo `axiom-control` removido) e §6 (descarte).

### Não pertence
- Container de injeção de dependência / ciclo de vida de aplicação (`ioc/`).
- Servidor HTTP ou framework MVC (`http/server`, `http/route`, anotações `@RestController`/`@GetMapping`).
- ORM ou clone de `JdbcTemplate`.
- Framework de testes (runner, anotações de ciclo de vida, assertions concorrentes com JUnit).
- DSL de controle de fluxo fluente (`If`/`When`/`Condition`/`Switch` estilo builder) — o `switch` de expressão e o pattern matching do Java 21 já resolvem esse problema nativamente; adicionar uma camada por cima não se justifica (ver §5).
- Logger completo (existe SLF4J + implementações maduras; Axiom no máximo oferece utilitários de formatação que um logger pode consumir, nunca um `Logger` estático global).
- Qualquer coisa que exija estado estático global mutável não sincronizado (motivo: essa foi a causa raiz dos problemas de concorrência encontrados em `Logger`, `EnvEngine` e `BeanRegistry.typeIndex` no JToolBox).

---

## 3. Arquitetura geral

Axiom é **multi-módulo Gradle desde o início**, seguindo o modelo do Obsidian (correto em princípio) mas corrigindo o erro de coesão observado em `obsidian-configuration` (três domínios não relacionados sob um nome). Cada módulo Axiom deve poder ser adotado **isoladamente**, com dependências mínimas e, sempre que possível, zero dependências de terceiros.

Regra de dependência entre módulos: **um DAG estrito, sem ciclos**, ancorado em um módulo `axiom-core` do qual os demais podem depender, mas que não depende de nenhum outro módulo Axiom. Nenhum módulo de "capacidade" (collections, promise, json...) depende de outro módulo de capacidade — isso existia por acidente no Obsidian (nenhum módulo consumia o outro, nem quando fazia sentido, como `DotenvInjector` reimplementando reflection em vez de usar `obsidian-reflection`); na Axiom, essa composição deve ser **decidida deliberadamente**, não deixada ao acaso.

```
axiom-core            (Try, Result, Maybe, exceções base, interfaces funcionais Failable*)
   ↑
   ├── axiom-collections     (estruturas persistentes)
   ├── axiom-reflect         (reflection fluente)
   ├── axiom-concurrent      (Promise, Box/AtomicBox, cancelamento, Memoized)
   ├── axiom-text            (formatação de string/número)
   ├── axiom-datetime        (wrappers de data/hora, duração legível por humano)
   ├── axiom-io              (arquivos, hashing, watch)
   ├── axiom-console         (scanner de console testável)
   ├── axiom-placeholder     (resolução de placeholders/templates)   ──┐
   ├── axiom-dotenv          (parsing + injeção de .env)                ├─ podem depender de axiom-reflect
   ├── axiom-json            (modelo JSON + motor plugável)             │  e/ou axiom-placeholder
   └── axiom-yaml            (modelo YAML + motor plugável)  ──────────┘
```

`axiom-dotenv`, `axiom-json` e `axiom-yaml` podem depender de `axiom-reflect` (para injeção anotada e mapeamento objeto↔dados) — essa é uma dependência de capacidade **intencional**, ao contrário da integração perdida no Obsidian. `axiom-dotenv` e `axiom-yaml` podem depender também de `axiom-placeholder`, se expuserem interpolação de variáveis dentro de valores.

**Não há módulo de DSL de controle de fluxo (`axiom-control`) na Axiom.** JToolBox e Obsidian dedicaram pacotes inteiros a esse problema (`control/{If,Condition,For,Switch}` e `obsidian.control.{When,DecisionChain,ChooseChain,MatchPattern}`, respectivamente) sem nenhuma das duas versões se firmar como solução definitiva — o `If` do JToolBox coexistia com `Condition` deprecated ainda em uso interno, e o Obsidian acumulou 5 variantes concorrentes de `When`. O `switch` de expressão e o pattern matching nativos do Java 21 resolvem a mesma necessidade sem exigir API adicional; a Axiom não reintroduz esse domínio.

---

## 4. Organização de pacotes e convenção de nomenclatura

- Group ID proposto: `io.axiom` (ou equivalente do autor). Pacote raiz por módulo: `io.axiom.<modulo>`, ex. `io.axiom.collections`, `io.axiom.result`, `io.axiom.reflect`.
- **Nunca reutilizar nomes de pacote do JDK** (correção direta do problema `lang.reflect` do Obsidian, que colide com `java.lang.reflect`). Regra: antes de nomear um pacote, checar se o nome completo colide com algo em `java.*`/`javax.*`.
- **Sem sufixos decorativos sem significado documentado** (correção do sufixo "X" inconsistente do JToolBox — `JsonX`, `AssertX`). Se um prefixo/sufixo for adotado (ex.: prefixo curto para famílias de tipos, como o "O" de `OMap` no Obsidian, inspirado em PCollections), deve ser documentado uma única vez no README do módulo e aplicado com 100% de consistência.
- **Um nome, um conceito.** Proibido ter duas classes com o mesmo nome simples resolvendo o mesmo problema em pacotes diferentes (correção do `TypeConverter` duplicado em `util/` e `config/core/` do JToolBox).
- Nome de módulo Gradle reflete exatamente seu conteúdo — nunca um nome genérico (`configuration`) cobrindo domínios não relacionados.
- API pública em `io.axiom.<modulo>.api` (ou raiz do pacote, se pequeno) — implementação interna sempre em subpacote `internal` não exportado no `module-info.java` (Axiom deve usar JPMS — ver §11), replicando o bom padrão observado em `obsidian.json` (motor Gson escondido atrás de `internal.gson.*`).

---

## 5. Módulos propostos e responsabilidade de cada um

### `axiom-core`
Tipos de resultado/erro fundamentais, sem dependência de nada além da JDK.
- `Try<T>` — funde o melhor de `util/Try.java` (JToolBox: `retryWithBackoff`, `sequence`, `traverse`, `combine`, `withTimeout`, `parallel`) com `functional/Try.java` (Obsidian: `mapTry`, `checkedGet()`, `recover` tipado por classe de exceção). O bug confirmado em `Try.retry(int)` de instância do JToolBox (não reexecuta o supplier original) deve ser corrigido, não herdado.
- `Result<T, E>` — herdado do design do Obsidian (`sealed interface Result<T,E> permits Ok, Err`), com erro tipado explícito.
- `Maybe<T>` (ou adoção direta de `java.util.Optional` com extensões via métodos estáticos, a decidir na implementação) — herdado do padrão `sealed` do Obsidian.
- **Decisão de design explícita que a Axiom precisa tomar e documentar** (o Obsidian não tomou): quando usar `Try` vs `Result` vs `Optional`/`Maybe`. Proposta: `Optional`/`Maybe` para ausência sem causa; `Result<T,E>` para falhas de domínio esperadas e tipadas; `Try<T>` para capturar exceções de APIs Java que lançam `Throwable`. Documentar isso no Javadoc do pacote (`package-info.java`) do `axiom-core`, não deixar implícito.
- `Failable{Consumer,Function,Runnable,Supplier}` — herdado do Obsidian (`functional/failable/*`), interfaces funcionais que permitem `throws Exception`.
- Exceções base (`AxiomException` unchecked como raiz opcional para exceções de domínio específicas de cada módulo — seguindo o padrão consistente de exceção-por-domínio observado como ponto forte em ambas as bibliotecas).

### `axiom-collections`
Estruturas de dados persistentes/imutáveis, herdadas majoritariamente do Obsidian.
- Interfaces núcleo equivalentes a `OCollection`/`OMap`/`OSet`/`OSequence`/`OStack`/`OQueue`/`OVector`/`OSortedMap`/`OSortedSet`, renomeadas para não depender do prefixo "O" sem necessidade (avaliar `PersistentMap`/`PersistentSet` ou manter um prefixo curto documentado — decisão de nomenclatura a ser tomada na implementação, não neste documento).
- HAMT real (`Hashing`, implementação equivalente a `HashTriePMap`/`HashTrieOSet`) — reaproveitar o algoritmo verificado como corretamente implementado (MurmurHash3, path copying real, branching factor 32).
- Vetor persistente: reavaliar `ChunkedOVector` antes de portar — a auditoria identificou que **não é uma árvore RRB real** (só 2 níveis, inserção no meio é O(n)). Se Axiom quiser inserção arbitrária eficiente, precisa implementar uma RRB-tree de verdade; caso contrário, documentar honestamente a limitação (como o Obsidian já fez).
- Fila persistente de duas pilhas (`AmortizedOQueue`) — reaproveitar, é implementação de livro-texto correta.
- **Não** reaproveitar `collections/Dynamic.java` do JToolBox como está (é mutável, pertence a outra categoria de problema — ver `axiom-adaptive` opcional abaixo), mas **reaproveitar sua API fluente** (`map`/`flatMap`/`filter`/`groupBy`/`partition`/`zip`/`union`/`intersection`/`difference`) como inspiração de superfície de API para operações sobre as coleções persistentes.
- Requisito não negociável: suíte de testes de corretude (não só benchmark JMH) cobrindo invariantes de path copying, colisão de hash, e igualdade estrutural — a lacuna nº 1 identificada no Obsidian.

### `axiom-concurrent`
- `Promise<T>` — herdado quase integralmente do `obsidian-promise` (API mais madura de toda a auditoria): cancelamento cooperativo (`CancellationToken`/`CancellationSource`), `RetryPolicy` com estratégias de backoff (fixo, exponencial, jitter, nenhum), combinadores (`all`/`any`/`race`), interop com `CompletableFuture`. Corrigir a inconsistência de licenciamento identificada (`Promise.java` sem header) e **adicionar a suíte de testes que faltava** (zero testes no original) antes de considerar estável.
- `Box<T>`/`AtomicBox<T>`/`PlainBox<T>`/`AtomicVolatileBox<T>` — herdado do Obsidian como está; é o único trio de abstrações de concorrência da auditoria com garantias verificadas batendo exatamente com a documentação.
- Utilitário de execução assíncrona simples com virtual threads (Java 21) como pool padrão — inspirado na decisão acertada de `http/server/Server.java` do JToolBox (`Executors.newVirtualThreadPerTaskExecutor()`), mas isolado como utilitário de propósito geral (`axiom.concurrent.Tasks` ou similar), não como parte de um servidor.
- **Não** herdar `concurrent/Threader.java` como está (pool `newCachedThreadPool()` sem limite) — se um wrapper de `ExecutorService` fizer sentido, deve nascer com pool dimensionável e virtual threads como padrão.
- `Memoized<T>` — `Supplier<T>` com cache de resultado (com ou sem TTL), lacuna real da JDK (não existe equivalente nativo). Peça pequena, sem dependência externa; pode ser reaproveitada internamente pelo cache de lookup de `axiom-reflect` (§5, `axiom-reflect`) em vez de cada módulo reimplementar memoização própria.

### `axiom-reflect`
- Reflection fluente inspirada em `lang.reflect.Reflect` do Obsidian, **renomeada** para `io.axiom.reflect` (elimina a colisão com `java.lang.reflect`).
- Adicionar cache de lookup de `Method`/`Field` por `(Class, nome)` — lacuna identificada na auditoria (Obsidian não cacheia).
- API: `Reflect.on(Class<?>)`/`Reflect.on(Object)`, acesso a campos/métodos/anotações com exceções checked convertidas em uma exceção unchecked única.

### `axiom-placeholder`
Módulo novo, sem equivalente direto nas duas bibliotecas auditadas — proposto para corrigir uma fragmentação real: resolução de placeholders/templates de string aparece reimplementada de forma independente em pelo menos três lugares nas bibliotecas originais (parser de `{}` estilo SLF4J em `text/StringFormatter.java` do JToolBox, resolvedor de `${prop:default}` em `ioc/environment/Environment.java` do JToolBox, e formatação de I/O com sintaxe própria em `console/IO.java`), mas a resolução de placeholder em si nunca virou peça própria — é o mesmo padrão de fragmentação que motivou consolidar `axiom-dotenv` em um único sistema.
- Fonte de resolução plugável (`Map`, variáveis de ambiente, `System.getProperties()`, função custom).
- Sintaxe suportada: `${key}`, `${key:default}`; transformação simples via pipe (`${key|upper}`) como extensão opcional; aninhamento (`${outer.${inner}}`) documentado como avançado, não obrigatório na v1.
- **Detecção de referência circular obrigatória** — ponto que implementações caseiras tendem a esquecer (nenhuma das duas bibliotecas auditadas tratava isso explicitamente) e que vira `StackOverflowError` em produção se ignorado.
- Depende apenas de `axiom-core`. Consumido deliberadamente por `axiom-dotenv`, `axiom-yaml` e, se fizer sentido, mensagens de erro de `axiom-io` — em vez de cada módulo reinventar seu próprio mini-parser, replicando o problema de integração perdida identificado no Obsidian (`DotenvInjector` reimplementando reflection ao invés de usar `obsidian-reflection`).

### `axiom-dotenv`
- Parsing de `.env` herdado da camada de baixo nível do JToolBox (`dotenv/util/{DotenvParser,DotenvReader,ClasspathResourceLoader}`) combinado com o modelo de injeção declarativa por anotação do Obsidian (`@Env`, `@EnvPrefix`, `@RequiredEnv`, `@Default`, `@EnvIgnore` — mais expressivo que o `config/annotations/*` do JToolBox nesse ponto específico).
- Adicionar de volta os recursos do `config/` do JToolBox que o Obsidian não tinha e que fazem sentido para uso real: profiles (`@Profile`) e reload (`@Reloadable`) — mas com o estado do "engine" **não estático global**, e sim uma instância explícita (`DotenvContext`/`EnvSource`) que o usuário cria e injeta, eliminando o problema de `EnvEngine` estático mutável sem sincronização.
- Consolidar em **um único sistema**, eliminando a fragmentação de três sistemas de config do JToolBox (`dotenv/`, `config/`, `ioc/environment/Environment`) desde a concepção — `axiom-dotenv` é o único lugar onde "configuração externa" é resolvida.
- Usar `axiom-reflect` internamente para a injeção por campo (em vez de `java.lang.reflect.Field` cru, como o `DotenvInjector` do Obsidian fazia) — integração deliberada entre módulos, corrigindo a falta de coesão observada.
- Usar `axiom-placeholder` internamente para resolver `${prop:default}` dentro de valores de `.env`, em vez de reimplementar esse parser localmente (substitui o resolvedor próprio que existia em `ioc/environment/Environment.java` do JToolBox).

### `axiom-json`
- Modelo de dados JSON próprio (`JsonElement`/`JsonObject`/`JsonArray`/`JsonPrimitive`/`JsonNull`) com motor de parsing/serialização **escondido atrás de uma interface interna plugável** — reaproveitando o padrão de encapsulamento do `obsidian.json.api.Json` (que não expõe Gson na API pública). Gson como motor padrão inicial (`internal.gson.*`), mas a interface interna deve permitir trocar por outro motor sem quebrar API pública.
- Mapeamento objeto↔JSON via anotações (`@JsonName`, `@JsonIgnore`, `@JsonDefault`, `@JsonRequired`, `@JsonAdapter` — herdados do Obsidian) usando `axiom-reflect` internamente.
- **Não** herdar `json/JsonX.java` do JToolBox como está — cria uma nova instância de `Gson` a cada chamada estática, custo evitável; e não encapsula o motor (expõe Gson implicitamente).

### `axiom-yaml`
Módulo novo, proposto para preencher um nicho real ainda mal resolvido no ecossistema Java: bibliotecas de YAML existentes fazem bom parse, mas raramente oferecem round-trip preservando comentários/formatação ao editar um arquivo programaticamente. Segue o mesmo padrão já validado em `axiom-json`:
- Modelo próprio (`YamlNode`/`YamlMapping`/`YamlSequence`/`YamlScalar`), motor de parsing escondido atrás de interface interna plugável (`internal.snakeyaml.*` como implementação inicial).
- Acesso por path pontuado (`get("db.host")`) — recurso já validado em `yaml/YamlConfig.java` do JToolBox, mas reimplementado aqui sobre o modelo próprio em vez de delegar a um `Map` aninhado navegado manualmente.
- Mapeamento objeto↔YAML via `axiom-reflect`, reaproveitando a mesma arquitetura de anotações desenhada para `axiom-json` (evita a duplicação de conceito que existiria se cada formato de serialização tivesse seu próprio sistema de anotações).
- Preservação de comentários (parser próprio) fica como meta de v2, não bloqueia a v1.
- Depende de `axiom-core`, `axiom-reflect`; pode depender de `axiom-placeholder` se expuser interpolação de variáveis dentro de valores YAML.

### `axiom-io`
- Operações de arquivo herdadas de `io.obsidian.file.*` (único subsistema de configuração do Obsidian com testes reais): `FileHandle` (API fluente), `Directory`, `attribute/*`, `hash/*` (Strategy: MD5/SHA-256), `operation/{FileCompressor,FileOperations}`, `search/FileSearch`.
- `FileWatcher` do JToolBox (wrapper de `WatchService`) pode ser incorporado aqui se não houver equivalente no Obsidian.
- Manter a hierarquia de exceções especializadas por operação (padrão bom observado em ambas as libs).

### `axiom-console`
- Herdar a arquitetura em camadas de `obsidian.experimental.io.scan` **integralmente e "graduada" de experimental para estável** — é, segundo a auditoria, o melhor design de scanner de console das duas bibliotecas: `InputSource` plugável (`ConsoleSource`/`ReaderSource`/`StringSource`), `Parser`/`Validator`/`Prompt` separados, `ScanResult` com `ErrorCode` em vez de exceção crua.
- Antes de "graduar", adicionar a suíte de testes que faltava — a testabilidade da arquitetura (via `StringSource`) deve ser efetivamente exercida, não só possibilitada.
- **Não** herdar `console/{Scan,Scanf,IO}` do JToolBox — arquitetura estruturalmente inferior (acoplado direto a `Scanner`/`System.in`, sem fonte de entrada plugável).

### `axiom-text` / `axiom-datetime`
- Formatação de string/número com placeholders (`{}` estilo SLF4J) herdada de `text/StringFormatter.java` do JToolBox, avaliando se não é melhor delegar a `java.text`/`String.format` sempre que a JDK já resolver o problema — só portar o que agrega valor real. O parser de placeholder em si não deve ser reimplementado aqui: se `axiom-text` precisar de interpolação, consome `axiom-placeholder` em vez de duplicar o parser.
- `DateTime` — avaliar se um wrapper é necessário ou se a Axiom deve apenas oferecer formatos nomeados como constantes/utilitários sobre `java.time` puro, sem encapsular `LocalDateTime`/`ZonedDateTime` atrás de uma classe própria (evitar reinventar API que a JDK já resolve bem desde o Java 8).
- **Duração legível por humano** — `java.time.Duration` não converte nativamente de/para strings como `"2h 30m"`. Utilitário pequeno e sem dependência externa, candidato natural a `axiom-datetime` (só justifica o módulo existir se, somado aos formatos nomeados acima, houver volume de API suficiente — caso contrário, avaliar se cabe como classe utilitária dentro de `axiom-core`).

### `axiom-numeric`, `axiom-id`, `axiom-csv` (avaliados e implementados pós-1.0, Etapa 7)

Três módulos avaliados depois que as etapas 0–6 já estavam completas, aplicando o mesmo
crivo usado para `axiom-text`/`axiom-datetime` ("o JDK já resolve isso adequadamente?") —
mas aqui o resultado da avaliação foi o oposto: os três passam no crivo. Todos os três foram
implementados na Etapa 7 (ver `CHANGELOG.md` para o histórico e cada `README.md` de módulo para
o racional completo — a proposta original de `axiom-numeric` que existia como
`docs/axiom-numeric.md` foi incorporada a `axiom-numeric/README.md` e removida deste diretório
uma vez implementada, para não duplicar documentação).

- **`axiom-numeric`** — tipos de largura fixa (`I8`…`I64`, `U8`…`U64`), semântica de overflow
  explícita (checked/wrapping/saturating), e leitura/escrita binária com endianness
  (`Bytes`/`Endian`). Lacuna real: a JDK não tem unsigned, não tem overflow explícito, e
  `ByteBuffer` resolve bytes/endianness sem nenhuma expressividade semântica (`putShort` não
  comunica "isso é uma porta de rede"). Zero dependência, não colide com `java.*`. Não deve
  absorver `BigInteger`/`BigDecimal`/`Fraction`/`Money` — esses ficam fora por princípio (ver
  `axiom-numeric/README.md` §"O que não resolve"); se algum dia forem propostos, pertencem a um
  futuro `axiom-math`, não aqui.
- **`axiom-id`** — geração de identificadores ordenáveis por tempo (UUIDv7, ULID). Lacuna real:
  `java.util.UUID` só cobre v3/v4/v5, nenhum deles ordenável por tempo de criação, algo hoje
  comum em chave primária de banco e em log distribuído. API pequena (`Id.uuidV7()`,
  `Id.ulid()`), zero dependência de terceiros necessária.
- **`axiom-csv`** — parsing e escrita de CSV com modelo próprio, seguindo o mesmo padrão já
  validado em `axiom-json`/`axiom-yaml` (mas sem precisar esconder motor de terceiros — o
  formato é simples o bastante para implementação pura). Lacuna real: a JDK não tem nenhum
  parser de CSV, e `Scanner`/`split(",")` não trata aspas, escapes nem quebra de linha
  embutida em campo corretamente.

Todos os três só dependem de `axiom-core` opcionalmente (nenhum precisa de fato, a decisão de
depender ou não fica para a implementação, seguindo a mesma disciplina de "nenhuma dependência
sem uso confirmado" aplicada ao resto do projeto).

Avaliados e **descartados** na mesma sessão, por já estarem cobertos por outra decisão já
registrada neste documento: `axiom-decimal`/`Money` (`BigDecimal` já resolve precisão decimal
arbitrária; ver acima), parser de linha de comando e cache com política de evicção completa
(ambos já listados em "O que fica deliberadamente de fora", logo abaixo) — nenhuma dessas
avaliações reverte uma decisão anterior, apenas a reafirma com um exemplo concreto a mais.

### Utilitários pontuais sem módulo dedicado
Candidatos pequenos que preenchem lacunas reais da JDK, avaliados e incorporados individualmente — nenhum deles justifica um módulo Gradle próprio pelo volume de API, então vivem dentro do módulo cuja responsabilidade já é mais próxima:
- **Cópia parcial de records** (`@Buildable` ou nome equivalente) — `java.lang.Record` não tem equivalente ao `data class.copy()` do Kotlin. Um processador de anotação leve que gera builder + métodos `withX(...)` para records resolve uma dor ergonômica real sem reinventar o que records já resolvem bem (imutabilidade, `equals`/`hashCode`/`toString`). Por ser um processador de anotação (dependência só em tempo de compilação, não em runtime), deve viver isolado do restante de `axiom-core` — decisão de empacotamento (módulo próprio `axiom-codegen` vs. `annotationProcessor` dentro de `axiom-core`) a tomar na implementação, não neste documento.
- **Validação composável** (`Validator<T>`) — correção direta do problema identificado na auditoria original: `config/core/EnvValidator.java` do JToolBox valida via cadeia de `instanceof`, fechada para extensão. Um `Validator<T>` pequeno e composável (`and`/`or`), sem exigir anotação mas combinável com `axiom-reflect` depois para uso declarativo, vive em `axiom-core` (zero dependência externa).
- **Memoização** — ver `Memoized<T>` em `axiom-concurrent` (§5, acima).

### O que fica deliberadamente de fora (reforço de escopo)
Para não repetir o erro de escopo do JToolBox, ficam fora por princípio: parser de linha de comando, cache sofisticado com política de evicção completa (existem soluções maduras como Caffeine — não é papel da Axiom competir com elas), qualquer framework de resiliência tipo circuit breaker (`RetryPolicy` já dentro de `axiom-concurrent` cobre o caso de uso legítimo de retry sem precisar virar módulo à parte), e DSL de controle de fluxo fluente (`axiom-control` — ver §2 e §3, decisão de não incluir).

---

## 6. O que reaproveitar, refatorar ou descartar — resumo

### Reaproveitar (com ajustes pontuais)
- `Try` — fundir JToolBox + Obsidian, corrigir bug de `retry()`.
- HAMT (`Hashing`, `HashTriePMap`/`HashTrieOSet`) — Obsidian.
- `obsidian-promise` inteiro — Obsidian, adicionar testes.
- `Maybe`/`Result` sealed — Obsidian.
- `Box`/`AtomicBox`/`PlainBox`/`AtomicVolatileBox` — Obsidian.
- `obsidian.experimental.io.scan` — Obsidian, graduar de experimental, adicionar testes.
- `io.obsidian.file.*` — Obsidian (já tem testes).
- Dotenv com anotações declarativas — Obsidian como base, JToolBox para profiles/reload.
- `obsidian.json` (modelo próprio + motor escondido) — Obsidian como base de design.
- `Jdbc`/`URLBuilder` (só o builder de conexão, não `JdbcTemplate`) — JToolBox, **se e somente se** Axiom decidir que builders de conexão JDBC fazem parte do escopo (avaliar: talvez pertença mais a um projeto de aplicação do que a uma lib de utilitários gerais — recomendação: deixar de fora do escopo inicial, revisitar depois).
- `RoutePattern` (compilação de `/users/:id` para regex) — JToolBox, como utilitário isolado de parsing de padrão, não parte de um servidor.
- Reflection fluente — Obsidian, renomeada e com cache adicionado.
- Acesso a config por path pontuado (`getString("db.host")`) — JToolBox (`yaml/YamlConfig.java`), como inspiração de API para `axiom-yaml`, reimplementado sobre modelo de dados próprio em vez de `Map` aninhado navegado manualmente.

### Refatorar antes de incorporar
- `Dynamic` (JToolBox) — extrair só a API fluente (map/filter/groupBy/zip/etc.), descartar a heurística de auto-otimização por threshold fixo ou reimplementá-la com medição real.
- `Threader` (JToolBox) — reconstruir com pool dimensionável e virtual threads por padrão.
- `ChunkedOVector` (Obsidian) — decidir entre implementar RRB-tree real ou documentar explicitamente a limitação de 2 níveis.
- `EnvEngine` (JToolBox) — extrair a ideia (fachada de conveniência), mas eliminar o estado estático global mutável.

### Descartar
- `ioc/` inteiro (JToolBox).
- `http/server` e `http/route` como servidor completo (JToolBox) — no máximo `ClientRequest`/`ClientResponse` como cliente HTTP fino, se houver demanda real.
- `jdbc/dao/JdbcTemplate` (JToolBox).
- `test/` inteiro (JToolBox).
- `config/examples/UsageExamples.java` (JToolBox).
- Dependências ASM sem uso (JToolBox).
- `obsidian-tests` como módulo vazio (Obsidian).
- Entradas mortas de `libs.versions.toml` (Obsidian).
- Nome de pacote `lang.reflect` (Obsidian) — a funcionalidade fica, o nome não.
- `ioc/environment/Environment.java` (JToolBox) — config fragmentada em 3 sistemas, substituída por `axiom-dotenv` + `axiom-placeholder`.
- **Todo o domínio de DSL de controle de fluxo fluente**: `control/{If,Condition,For,Switch}` (JToolBox) e `obsidian.control.{When,DecisionChain,ChooseChain,MatchPattern}` (Obsidian). Decisão explícita e definitiva: a Axiom não tem módulo `axiom-control`. Nenhuma das duas implementações auditadas se firmou como solução (JToolBox manteve `Condition` deprecated em uso interno ativo; Obsidian acumulou 5 variantes concorrentes de `When` sem consolidar em uma). O `switch` de expressão e o pattern matching nativos do Java 21 já resolvem bem o problema original — reintroduzir esse domínio na Axiom seria repetir o padrão de "funcionalidade adicionada porque parece interessante" que o projeto quer evitar por princípio (ver §2).

### Novo, sem equivalente direto nas duas bibliotecas
- Camada de teste de corretude para `axiom-collections` (property-based testing seria uma boa escolha para validar invariantes de estruturas persistentes — decisão a tomar na fase de implementação).
- Cache de reflection lookup em `axiom-reflect`.
- `module-info.java` por módulo (JPMS) — nenhuma das duas bibliotecas usa módulos Java explicitamente.
- `axiom-placeholder` — resolução de placeholders/templates como módulo próprio, com detecção de referência circular obrigatória; nenhuma das duas bibliotecas tratou esse problema como capacidade isolada e reutilizável (cada uma reimplementou fragmentos dele em lugares diferentes).
- `axiom-yaml` — modelo de dados YAML próprio com motor plugável, seguindo o padrão já usado em `axiom-json`; nenhuma das duas bibliotecas tinha essa separação (o `yaml/` do JToolBox expõe SnakeYAML sem esconder o motor).
- `Memoized<T>`, `Validator<T>` composável, cópia parcial de records (`@Buildable`) — utilitários pontuais sem equivalente em nenhuma das duas bibliotecas.

---

## 7. Princípios de design

1. **Nenhum estado estático mutável não sincronizado.** Toda fachada de conveniência estática deve ser imutável após construção ou delegar a uma instância que o usuário controla.
2. **Imutabilidade por padrão** nos tipos de dados (`Try`, `Result`, `Maybe`, coleções persistentes) — mutação é exceção, não regra.
3. **Fail-fast e tipado.** Erros de domínio são tipos (`Result<T,E>`), não `Exception` genérica, sempre que a informação de erro for previsível.
4. **API pequena e composável** — preferir 3 métodos ortogonais a 12 sobrecargas.
5. **Zero dependência de terceiros por padrão**; cada exceção precisa de justificativa registrada (ex.: Gson em `axiom-json`, e mesmo assim escondido atrás de uma interface interna substituível).
6. **Nenhuma dependência declarada sem uso confirmado** — regra de processo: PR que adiciona dependência deve linkar ao menos um `import` real no diff.
7. **Javadoc de qualidade como parte da definição de "pronto"** — replicar o padrão observado como ponto forte do Obsidian (`<h2>Overview</h2>`, `<h2>Complexity</h2>`, `<h2>Design notes</h2>`, exemplos `{@code}`), não o padrão fragmentado de READMEs por pacote do JToolBox.
8. **JPMS desde o início** — `module-info.java` por módulo, expondo só pacotes de API, escondendo `internal.*`.

---

## 8. Convenções de nomenclatura

- Pacotes: `io.axiom.<modulo>[.subpacote]`, minúsculos, sem abreviação obscura.
- Nunca nomear um pacote igual a um pacote da JDK (`lang.reflect` é proibido; `io.axiom.reflect` é o padrão).
- Classes de fachada estática, se existirem, terminam em nome descritivo do domínio, nunca com sufixo decorativo sem significado (nada de "X" solto).
- Interfaces de "família de tipo" (ex. coleções persistentes) usam um prefixo/sufixo único, documentado uma vez, aplicado 100% consistentemente — decisão concreta de nome a tomar na implementação (ex.: `PersistentMap` vs. prefixo curto), mas nunca duas convenções coexistindo como aconteceu entre `json/JsonX*` (com X) e `yaml/Yaml*` (sem X) no JToolBox.
- Um conceito, um nome, em todo o projeto — proibido ter duas classes `TypeConverter` fazendo coisas parecidas em módulos diferentes; se a conversão de tipo é um conceito central, ela mora em `axiom-core` e é reusada, não duplicada.

---

## 9. Estratégia para evitar dependências desnecessárias

- Lista de dependências permitidas por módulo revisada em cada PR; CI falha se uma dependência declarada não tiver `import` correspondente em `src/main`.
- Preferir implementar com a JDK pura sempre que o ganho de uma lib externa for marginal (ex.: parsing de `.env`, hashing MD5/SHA-256 via `java.security.MessageDigest`, HTTP client via `java.net.http`).
- Lombok: decidir uma vez, aplicar em 100% do projeto ou em 0% — a inconsistência do Obsidian (disponível globalmente, usado em 1 de 6 módulos) não deve se repetir. Dado que Java 21 tem records e a JDK moderna reduz boilerplate nativamente, a recomendação é **avaliar se Lombok é necessário antes de adicioná-lo**, não adicioná-lo por hábito.
- Motor de JSON (Gson) e qualquer outra dependência "de motor" deve ficar isolada atrás de uma interface interna substituível, para que trocar de motor no futuro não seja uma mudança de API pública.

---

## 10. Estratégia de compatibilidade e evolução da API

- Versionamento semântico estrito desde `0.x` (deixar claro no changelog quando uma API é experimental).
- Um módulo `axiom-experimental` explícito (em vez do padrão do Obsidian de marcar só um pacote como `experimental` dentro de um módulo "estável") para qualquer API em incubação — quando graduar, muda de módulo, o que torna o compromisso de estabilidade visível na própria dependência declarada pelo consumidor (`implementation("io.axiom:axiom-experimental")` vs `implementation("io.axiom:axiom-console")`).
- Depreciação sempre com `@Deprecated(since=..., forRemoval=...)` e um caminho de migração documentado no Javadoc — nunca deixar uma API deprecated sendo usada internamente pela própria biblioteca (o erro do `control.Condition` ainda referenciado por `Logger`/`IO`/`Array`/`Threader` no JToolBox não deve se repetir).
- Nenhuma classe pública deve expor implementação de terceiros (Gson, SnakeYAML) no tipo de retorno de método público — sempre atrás do modelo de dados próprio do módulo.

---

## 11. Estratégia de testes

- Cobertura mínima obrigatória por módulo antes de release `1.0`: caminho principal + casos de borda + (para estruturas de dados) testes de invariante estrutural.
- `axiom-collections`: testes de propriedade (property-based) para confirmar invariantes de HAMT (path copying não deve afetar a versão anterior; igualdade estrutural; comportamento correto sob colisão de hash) — lacuna crítica identificada no Obsidian.
- `axiom-concurrent`: testes de concorrência real (múltiplas threads cancelando/resolvendo `Promise` simultaneamente) — lacuna crítica identificada no `obsidian-promise`.
- `axiom-console`: usar a própria fonte de entrada plugável (`StringSource`) para testar `axiom-console` sem tocar em stdin — a arquitetura já habilita isso, só falta exercer.
- `axiom-io`: seguir o padrão de teste já existente e aprovado em `io.obsidian.file.*` (único subsistema de configuração do Obsidian com testes reais).
- `axiom-placeholder`: teste obrigatório de detecção de referência circular (`${a}` → `${b}` → `${a}`) — é justamente o tipo de caso que implementações caseiras nas duas bibliotecas auditadas não cobriam.
- `axiom-yaml`: mesma disciplina de teste de `axiom-json` (parse/serialize round-trip sobre o modelo próprio, não sobre o motor interno).
- CI obrigatório: build + testes em cada PR; nenhum módulo é publicado sem build verde (correção direta do estado atual do JToolBox, que não compila).

---

## 12. Estratégia de documentação

- Javadoc de módulo (`package-info.java`) explicando o propósito e as fronteiras de uso de cada tipo — replicar o padrão de qualidade do Obsidian (`Result` documentando explicitamente sua diferença de `Optional`), estendendo a mesma clareza para a relação `Try`/`Result`/`Maybe` que o Obsidian nunca documentou explicitamente.
- Um README por módulo Gradle (não fragmentos soltos dentro de `src/main/java` como no JToolBox), com exemplos mínimos de uso.
- Um documento central de arquitetura (este projeto, evoluído) mantido junto ao repositório, atualizado a cada módulo novo.
- **Padrão de documentação formalizado em [`CONVENTIONS.md`](./CONVENTIONS.md)**: template de README por módulo (seções fixas: O que resolve / O que não resolve / Instalação / Exemplo Rápido / API Principal / Quando usar e quando não usar / Notas de Design / Testes / Changelog) e um exemplo preenchido de referência (`axiom-core`). A seção "Quando usar (e quando não usar)" é obrigatória em qualquer módulo com sobreposição conceitual (ex.: `Try`/`Result`/`Maybe` em `axiom-core`) — existe especificamente para não repetir o problema identificado no Obsidian de três tipos de "outcome" convivendo sem fronteira de uso documentada.
- Cada módulo deve ter um exemplo **compilado por CI** (não apenas trecho solto no README) em `<modulo>/examples/src/main/java/...`, garantindo que o "como" nunca fica desatualizado em relação ao "o quê"/"por quê" descritos em README e Javadoc.

---

## 13. Sugestão de estrutura do projeto

```
axiom/
├── README.md                        (raiz do projeto)
├── CHANGELOG.md                     (Keep a Changelog, seção por módulo)
├── build.gradle.kts                 (config raiz: toolchain Java 21, plugins comuns)
├── settings.gradle.kts              (include de todos os módulos)
├── gradle/libs.versions.toml        (catálogo — revisado a cada PR, sem entradas mortas)
├── axiom-core/
│   ├── README.md                    (template em docs/CONVENTIONS.md)
│   ├── examples/src/main/java/io/axiom/core/examples/...
│   └── src/{main,test}/java/io/axiom/core/{result,option,fn}/...
├── axiom-collections/
│   └── src/{main,test}/java/io/axiom/collections/...
├── axiom-concurrent/
│   └── src/{main,test}/java/io/axiom/concurrent/{promise,box,memoized}/...
├── axiom-reflect/
│   └── src/{main,test}/java/io/axiom/reflect/...
├── axiom-placeholder/
│   └── src/{main,test}/java/io/axiom/placeholder/...
├── axiom-dotenv/
│   └── src/{main,test}/java/io/axiom/dotenv/...
├── axiom-json/
│   └── src/{main,test}/java/io/axiom/json/{api,internal.gson}/...
├── axiom-yaml/
│   └── src/{main,test}/java/io/axiom/yaml/{api,internal.snakeyaml}/...
├── axiom-io/
│   └── src/{main,test}/java/io/axiom/io/...
├── axiom-console/
│   └── src/{main,test}/java/io/axiom/console/...
├── axiom-text/  axiom-datetime/    (avaliar necessidade real antes de criar)
├── axiom-experimental/              (qualquer API em incubação, isolada)
├── axiom-bench/                     (benchmarks JMH, análogo a obsidian-collections-bench)
└── docs/
    ├── architecture.md              (este documento)
    └── CONVENTIONS.md               (checklist de PR + template de README por módulo)
```

Cada módulo (`axiom-*/`) segue a mesma estrutura de `axiom-core/` acima: `README.md` próprio + diretório `examples/` compilado por CI. Omitido nos demais itens da árvore só por brevidade.

---

## 14. Roadmap de implementação por etapas

**Etapa 0 — Fundação**
- Configurar `axiom-core` (Try, Result, Maybe/Optional wrapper, Failable*, exceções base).
- Definir convenções de nomenclatura, checklist de dependência, CI com build+test obrigatório.

**Etapa 1 — Coleções persistentes**
- Portar HAMT (`Hashing`, mapa/set persistentes) do Obsidian com suíte de testes de propriedade.
- Decidir e implementar (ou documentar limitação de) vetor persistente.
- Fila e pilha persistentes.

**Etapa 2 — Concorrência**
- `Promise` com testes de concorrência reais.
- `Box`/`AtomicBox` family.
- Utilitário de execução com virtual threads.

**Etapa 3 — Reflection, placeholders e configuração**
- `axiom-reflect` com cache de lookup.
- `axiom-placeholder` (resolução de templates + detecção de referência circular) — construído antes de `axiom-dotenv`, já que este passa a consumi-lo.
- `axiom-dotenv` unificando parsing + injeção declarativa + profiles/reload, usando `axiom-reflect` e `axiom-placeholder` internamente.

**Etapa 4 — Dados e I/O**
- `axiom-json` (modelo próprio + motor plugável).
- `axiom-yaml` (modelo próprio + motor plugável, reaproveitando a arquitetura de anotações de `axiom-json`).
- `axiom-io` (arquivos, hashing, watch) portando o subsistema já testado do Obsidian.

**Etapa 5 — Console e extras**
- `axiom-console` graduando a arquitetura experimental do Obsidian, com testes.
- Avaliar `axiom-text`/`axiom-datetime` com o crivo de "isso a JDK já não resolve?" antes de implementar — inclui a decisão sobre duração legível por humano.
- Utilitários pontuais (`Memoized<T>` em `axiom-concurrent`, `Validator<T>` em `axiom-core`, `@Buildable`/cópia de records) incorporados aos módulos existentes, avaliados individualmente.

**Etapa 6 — Estabilização para 1.0**
- Auditoria de dependências (nenhuma sem uso).
- Revisão de nomenclatura cruzada entre módulos (nenhum conceito duplicado).
- Documentação completa (`package-info.java` + README por módulo, seguindo o template de [`CONVENTIONS.md`](./CONVENTIONS.md) + exemplo compilado por CI em `examples/` de cada módulo).
- Congelamento de API pública e versionamento semântico formal.
