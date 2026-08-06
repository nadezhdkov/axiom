# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/),
organizado por módulo. Versionamento semântico estrito desde `0.x`.

## [Unreleased]

### Tooling / CI
- Added: GitHub Packages as a publish target for every module (`build.gradle.kts`, subprojects'
  `publishing.repositories`), alongside the existing Maven Central target — lets other projects
  consume `io.axiom:*` modules straight from this repository's package registry.
- Added: `.github/workflows/publish.yml`, triggered on GitHub Release or manual dispatch, runs
  `./gradlew build` then `publishAllPublicationsToGitHubPackagesRepository` using the
  automatically-provided `GITHUB_TOKEN` — no manual PAT needed for CI itself (only consumers
  need one, to read).
- Fixed: POM/SCM URLs in `build.gradle.kts` pointed to `rickmvi/axiom`, which doesn't match the
  actual GitHub remote (`nadezhdkov/axiom`) — corrected to avoid publishing packages with
  incorrect source-repository metadata.

### axiom-dotenv
- Fixed: `DotenvException` for a malformed `.env` line now includes the source file name and
  line number (`Malformed line in 'config/app.env' (line 4): "..."`), plus a hint when the line
  uses `:` instead of `=` (a common mistake for anyone coming from YAML/JSON-style config) —
  both were silently dropped before reaching the exception even though the caller already had
  them available. Comparison prompted by reviewing `docs/loom.md`'s Rust-style diagnostics
  (a Python config library from a separate learning project), scoped down to just this one gap
  rather than porting any of Loom's actual design.
- Fixed: `DotenvTypeConverter` threw a raw `IllegalArgumentException` for an unsupported `@Env`
  field type instead of a `DotenvException` — inconsistent with the rest of the module's
  exception hierarchy.
- Changed: `DotenvInjectionException`'s message now includes the raw value received and the
  target field type, not just the field name and key, to reduce how often `getCause()` needs
  inspecting.

### axiom-numeric
- Added (Etapa 7, novo módulo, zero dependência): `I8`/`I16`/`I32`/`I64` (signed),
  `U8`/`U16`/`U32`/`U64` (unsigned), `Bytes`/`Endian`. Aritmética *checked* por padrão
  (`ArithmeticException` em overflow, mesmo precedente de `Math.addExact`), com
  `*Wrapping`/`*Saturating` explícitos — decisão fechada durante a implementação, deixada em
  aberto na proposta original (`docs/axiom-numeric.md §Overflow`). Toda a lógica de
  overflow/wrapping/saturating/conversão compartilhada em uma única classe base interna
  (`io.axiom.numeric.internal.FixedWidth`, não exportada) via `BigInteger`, evitando duplicar
  bit-shifting manual entre os 8 tipos. Conversão entre tipos é genérica
  (`convertChecked/Wrapping/Saturating(target)`) em vez de métodos nomeados por par — só
  `I32.toI8Checked/Wrapping/Saturated()` existem como nomeados, por serem o exemplo literal da
  proposta original. `U64.of(long)` nunca lança (todo bit pattern de `long` é um U64 válido).

### axiom-csv
- Added (Etapa 7, novo módulo, zero dependência): `Csv`/`CsvConfig`/`CsvDocument`/`CsvRow`/
  `CsvException`. Parser RFC 4180 próprio (`io.axiom.csv.internal.CsvParser`) — sem motor de
  terceiros escondido atrás de `internal.*` como em `axiom-json`/`axiom-yaml`, já que CSV é
  simples o bastante para não precisar de um. Cabeçalho sempre opt-in
  (`CsvConfig.withHeader(true)`), nunca inferido. Documento inteiro carregado em memória —
  limitação documentada honestamente em `axiom-csv/README.md`, não escondida.

### axiom-id
- Added (Etapa 7, novo módulo, zero dependência): `Id.uuidV7()` (RFC 9562 UUIDv7, devolvendo
  `java.util.UUID` — o tipo da JDK já é uma representação adequada, só faltava a geração v7) e
  `Id.ulid()` (`Ulid`, tipo próprio para a forma canônica Base32-Crockford de 26 caracteres, sem
  equivalente na JDK). Ambos monotônicos dentro do mesmo milissegundo via
  `io.axiom.id.internal.MonotonicClock` (estado atrás de `AtomicReference`, sem depender de
  `axiom-concurrent`). Decisão registrada: sem anotação de injeção de campo — ver
  `axiom-id/README.md` § "O que não resolve".

### axiom-core
- Added: `Try<T>`, fusão de duas implementações anteriores (JToolBox +
  Obsidian), com `retry`/`retryWithBackoff` estáticos que sempre reexecutam
  o supplier original — corrige um bug conhecido de uma versão anterior de
  `retry(int)` de instância que não reexecutava a operação original.
- Added: `Result<T,E>` (`sealed interface`, `Ok`/`Err`).
- Added: `Maybe<T>` (`sealed interface`, `Some`/`None`), com razão de
  ausência opcional.
- Added: `Failable{Supplier,Function,Consumer,Runnable}`.
- Added: `AxiomException`, raiz unchecked para exceções de domínio.
- Added (Etapa 5): `io.axiom.core.time.HumanDuration`, conversão `Duration` ↔ forma legível por
  humano (`"2h 30m"`) — única peça retida da avaliação de `axiom-text`/`axiom-datetime` (ver
  seção correspondente abaixo).
- Added (Etapa 6, revisão de nomenclatura cruzada): `io.axiom.core.type.TypeReference`,
  consolidado a partir de duas implementações idênticas independentes em `axiom-json.codec` e
  `axiom-yaml.codec` — mesmo nome simples resolvendo o mesmo problema em pacotes diferentes,
  violação direta da regra "um nome, um conceito" de `axiom.md §4`. Ambos os módulos passaram a
  usar o tipo compartilhado; `axiom-json.codec`/`axiom-yaml.codec` (esta última removida por
  completo, já sem nenhum outro tipo) deixaram de ter uma cópia própria.

### axiom-collections
- Added: `PCollection`/`PMap`/`PSet`, interfaces núcleo das coleções
  persistentes (prefixo `P` de "Persistent", convenção documentada uma vez
  em `axiom-collections/README.md`).
- Added: `HashTrieMap`/`HashTrieSet`, porte do HAMT do Obsidian
  (`Hashing`, `CollisionNode`, `BitmapIndexedNode`) — path copying real,
  branching factor 32, MurmurHash3 mix.
- Added: suíte de testes de propriedade (jqwik) cobrindo path copying,
  igualdade estrutural e colisão de hash — requisito não negociável.
- Added (fatia final da Etapa 1): `PSequence` (contrato compartilhado por `PVector`/`PStack`),
  `PVector`/`ChunkedPVector` (vetor em chunks fixos de 32 elementos — decisão deliberada de não
  implementar uma RRB-tree real, documentada honestamente em vez de escondida, conforme o gate
  de decisão do próprio `axiom.md`), `PStack`/`ConsPStack` (cons-list, push `O(1)`),
  `PQueue`/`AmortizedPQueue` (fila de duas pilhas de Okasaki, amortizada `O(1)`),
  `PSortedMap`/`TreePMap` e `PSortedSet`/`TreePSet` (`TreeMap`/`TreeSet` + snapshot imutável,
  contrato `NavigableMap`/`NavigableSet` completo).
- Fixed (em relação ao `obsidian.collections` original, descobertos ao portar): `TreeOSet` tinha
  `descendingIterator()` retornando `null` incondicionalmente (`@Deprecated`, mas ainda assim uma
  armadilha de `NullPointerException` para qualquer chamador do contrato padrão de
  `NavigableSet`) — `TreePSet#descendingIterator()` agora delega de verdade. As sobrecargas de
  2 argumentos `subMap`/`headMap`/`tailMap` (`TreeOMap`) e `subSet`/`headSet`/`tailSet`
  (`TreeOSet`) tinham o mesmo problema — `TreePMap`/`TreePSet` delegam para as sobrecargas de
  4/3 argumentos em vez de retornar `null`. `AmortizedOQueue#element()` retornava `null` em vez
  de seguir o contrato de `Queue#element()` (lançar `NoSuchElementException` quando vazia) —
  corrigido em `AmortizedPQueue`.
- Added: testes de propriedade (jqwik) cobrindo a família inteira, não só o HAMT — imutabilidade
  da versão anterior para vetor/pilha (`PersistentSequencePropertyTest`) e ordem de iteração
  sempre ordenada independente da ordem de inserção para mapa/set ordenados
  (`TreePMapPropertyTest`) — mesmo padrão não-negociável já aplicado ao HAMT.

### axiom-concurrent
- Added: `Promise<T>`, porte quase integral de `obsidian-promise`
  (combinadores `all`/`any`/`race`, `CancellationToken`/`CancellationSource`,
  `RetryPolicy` com backoff fixo/exponencial/jitter).
- Fixed: `Promise#retry(RetryPolicy)` recuperava sempre contra o mesmo
  future já resolvido em vez de reexecutar a operação original — mesma
  classe de bug do `Try.retry()` de `axiom-core`. Corrigido guardando o
  supplier original para reexecução por tentativa.
- Fixed: `Deferred#cancel(String)` fazia `state()` reportar `REJECTED` em
  vez de `CANCELLED` (o caminho de cancelamento com motivo nunca marca o
  `CompletableFuture` interno como cancelado via `Future#cancel`).
- Added: `Box`/`AtomicBox`/`PlainBox`/`AtomicVolatileBox`, porte direto do
  Obsidian (subsistema já verificado como correto na auditoria).
- Added: `Tasks`, executor de virtual threads compartilhado, usado como
  padrão de `Promises.async` (substitui `ForkJoinPool.commonPool()` da
  implementação original).
- Added: suíte de testes de concorrência real (múltiplas threads
  resolvendo/cancelando o mesmo `Deferred` simultaneamente) — lacuna
  crítica do `obsidian-promise` original, que não tinha nenhum teste.

### axiom-reflect
- Added: `Reflect`/`Field`/`Fields`/`ReflectMethod`/`ReflectMethods`/
  `ReflectAnnotations`/`ReflectBuilder`, porte do Obsidian renomeado de
  `lang.reflect` para `io.axiom.reflect` (elimina a colisão com
  `java.lang.reflect`).
- Added: cache de lookup de `Field`/`Method` por `(classe, nome[, tipos])`
  (`internal.LookupCache`) — lacuna confirmada na auditoria original.
- Fixed: varredura de hierarquia de classes ia até `Object.class` inclusive,
  causando `InaccessibleObjectException` sob JPMS ao chamar
  `setAccessible` em membros internos da JDK. Corrigido parando a
  varredura antes de `Object.class`.
- Removed (deliberadamente não portado): `Field#removeFinal()` — a técnica
  não funciona mais em JDKs modernos (Java 12+).
- Added: suíte de testes completa (módulo original não tinha nenhuma).

### axiom-placeholder
- Added: `PlaceholderSource`/`PlaceholderResolver`, módulo novo sem
  equivalente direto nas bibliotecas auditadas — sintaxe `${key}`,
  `${key:default}`, `${key|transform}`.
- Added: detecção obrigatória de referência circular
  (`CircularPlaceholderReferenceException`), com testes cobrindo ciclo
  direto, auto-referência, ciclo indireto multi-chave, e um caso "diamante"
  não cíclico que não deve ser falsamente sinalizado.

### axiom-json
- Added: `JsonElement`/`JsonObject`/`JsonArray`/`JsonPrimitive`/`JsonNull`, modelo de árvore JSON
  próprio; motor Gson escondido inteiramente atrás de `internal.gson.*` (não exportado em
  `module-info.java`) — nenhum tipo público retorna ou aceita `com.google.gson.*`.
- Added: `Json`/`JsonConfig`/`JsonMapper`, mapeamento objeto↔JSON via `@JsonName`/`@JsonIgnore`/
  `@JsonDefault`/`@JsonRequired`/`@JsonAdapter`, usando `axiom-reflect` internamente.
- Fixed (em relação ao `obsidian.json` original): `@JsonRequired`/`@JsonDefault` eram apenas
  hooks expostos pelo processador de anotações (`isRequired`/`getDefaultValue`), nunca conectados
  ao pipeline real de decode do Gson — a exceção documentada de `@JsonRequired` nunca era de fato
  lançada. `internal.gson.AxiomTypeAdapterFactory` fecha essa lacuna interceptando a árvore antes
  da desserialização delegada.
- Pendente (documentado, não escondido): `@JsonAdapter` é resolvido reflexivamente mas não está
  conectado automaticamente ao pipeline de encode/decode — mesma lacuna do hook original,
  mantida intencionalmente fora do escopo desta etapa.
- Added: `JsonSource`/`JsonSink`/`JsonFiles`, `JsonPrettyPrinter` (impressão independente do
  motor), `JsonPath`/hierarquia `JsonException`, `TypeReference`/`JsonCodec`.

### axiom-yaml
- Added: módulo novo (sem precedente em JToolBox/Obsidian), espelhando a arquitetura de
  `axiom-json`: `YamlNode`/`YamlMapping`/`YamlSequence`/`YamlScalar`/`YamlNull`, motor SnakeYAML
  escondido atrás de `internal.snakeyaml.*`.
- Added: `internal.snakeyaml.ObjectBinder`, binder objeto↔árvore próprio (não reaproveita o
  binding POJO nativo do SnakeYAML, que não permite controle fino sobre as anotações Axiom) —
  suporta POJOs aninhados, `List<T>`, `Map<String,V>` e enums, aplicando `@YamlName`/
  `@YamlIgnore`/`@YamlDefault`/`@YamlRequired`.
- Added: `YamlMapping#getPath`/`getString`/`getInt`/`getBoolean`/`getDouble`, acesso por path
  pontuado (`"db.host"`) reimplementado sobre o modelo de árvore próprio — API inspirada em
  `yaml/YamlConfig.java` do JToolBox, mas sem o cast não verificado a `Map<?,?>` do original.
- Pendente: `@YamlAdapter` (equivalente a `@JsonAdapter`) e preservação de comentários/formatação
  em round-trip (meta de v2 já registrada em `axiom.md`).

### axiom-io
- Added: `FileHandle`/`Directory`/`File`, porte de `io.obsidian.file.*` — o único subsistema de
  configuração com testes reais pré-existentes em qualquer uma das duas bibliotecas auditadas.
- Added: `attribute.{FileAttributes,FileMetadata,FilePermissions}` (snapshot atômico via uma
  única leitura de `BasicFileAttributes`), `hash.{HashAlgorithm,Md5Hash,Sha256Hash,FileHasher}`
  (Strategy), `operation.{FileOperations,FileCompressor}` (GZIP), `search.FileSearch`
  (`filter`/`grep`/`replaceAll`/`count`), hierarquia `exception.FileOperationException` (sealed,
  cada subtipo carregando o `Path` afetado).
- Zero dependência de terceiros e zero dependência de outro módulo Axiom — não usa
  `Try`/`Result`/`Maybe` de `axiom-core` internamente, então não declara essa dependência.
- Pendente: `FileWatcher` (wrapper de `WatchService` do JToolBox) — avaliação separada, não
  descartado. Testes isolados de `FileCompressor`/`FilePermissions` (cobertos hoje só
  indiretamente via `FileHandleTest`) — mesma lacuna do módulo original.

### axiom-reflect (correção adicional na Etapa 4)
- Fixed: `Reflect#create()`/`create(Object...)` resolviam o construtor via
  `getDeclaredConstructor()` mas nunca chamavam `setAccessible(true)` nele — qualquer construtor
  não-público (o caso comum de classes de configuração/DTO com construtor `private`/
  package-private) lançava `IllegalAccessException`, descoberto ao escrever o binder de objetos
  de `axiom-yaml`. Corrigido chamando `setAccessible(true)` no construtor resolvido antes de
  `newInstance`, com testes de regressão cobrindo construtor `private` com e sem argumentos.

### axiom-dotenv
- Added: `Dotenv`/`DotenvBuilder`/`DotenvBinder` + anotações (`@Env`,
  `@EnvPrefix`, `@Default`, `@RequiredEnv`, `@EnvIgnore`), consolidando os
  três sistemas de configuração fragmentados do JToolBox
  (`dotenv/`, `config/`, `ioc/environment/Environment`) em um só.
- Added: injeção usando `axiom-reflect` internamente (corrige a
  reimplementação de reflection identificada na auditoria original) e
  interpolação `${OUTRA_CHAVE:default}` usando `axiom-placeholder`
  internamente (incluindo detecção de referência circular).
- Added (fatia final, pós-Etapa 5): `@Profile`/`@Reloadable`, do `config/` do JToolBox, com
  estado explícito por instância (`ReloadableDotenv`, criado só via
  `DotenvBuilder#reloadable()`), nunca um singleton estático mutável como o `EnvEngine` original.
  `DotenvBuilder#profile(String)` mescla `<filename>.<profile>` sobre o arquivo base e expõe
  `Dotenv#activeProfile()`; `@Profile` restringe `DotenvBinder.bind` à(s) profile(s) listada(s);
  `DotenvBinder.reload(target, dotenv)` exige `@Reloadable` na classe alvo antes de reinjetar.

### axiom-console
- Added: módulo novo, graduado de `obsidian.experimental.io.scan` (experimental) para estável.
  `InputHandler`/`InputScanner` sobre `InputSource` plugável (`ConsoleSource`/`ReaderSource`/
  `StringSource`), `Parser`/`Validator`/`Prompt` separados (pacotes `parse`/`validate`/`prompt`),
  `Scan` como fachada estática de conveniência sobre um engine padrão trocável (`AtomicReference`,
  não campo solto — mantém o princípio de "sem estado estático mutável não sincronizado").
- Added: suíte de testes completa que faltava no módulo original — nenhum teste toca `System.in`,
  todos usam `StringSource`.
- Fixed (em relação ao `obsidian.experimental.io.scan` original): `ScanResult`/`Error`/
  `ErrorCode` já existiam no módulo original mas nenhum caminho de leitura os usava de fato —
  toda leitura continuava lançando exceção crua internamente. `InputHandler#tryRead` agora é um
  caminho de leitura real e não-lançador, devolvendo `Result<T, ScanError>` — reaproveitando o
  `Result<T,E>` já existente em `axiom-core` em vez de duplicar um segundo tipo "outcome"
  paralelo (`ScanError`/`ErrorCode` substituem a dupla `Error`/`ErrorCode` original).
- Not ported: `console/{Scan,Scanf,IO}` do JToolBox — acoplado diretamente a
  `java.util.Scanner`/`System.in`, sem abstração de fonte de entrada plugável equivalente a
  `InputSource`.

### axiom-text / axiom-datetime (avaliação da Etapa 5 — decisão registrada, módulos não criados)
- Avaliados com o crivo de `axiom.md`: "isso a JDK já não resolve?". `datetime/DateTime.java` do
  JToolBox (~1500 linhas) é um wrapper sobre `ZonedDateTime` que reinventa uma API que o
  `java.time` já resolve bem desde o Java 8 — não portado. `text/StringFormatter.java`
  (formatação com placeholders `{}` estilo SLF4J) está fortemente acoplado a subsistemas já
  descartados da Axiom (`control/`, `logger/`) e não demonstrou ganho real sobre
  `String.format`/`java.text` — não portado.
- Única lacuna real da JDK identificada — formatação de duração legível por humano
  (`Duration#toString()` produz ISO-8601, não `"2h 30m"`) — tinha volume de API pequeno demais
  para justificar um módulo `axiom-datetime` próprio, então virou `io.axiom.core.time.HumanDuration`
  dentro de `axiom-core`, conforme o próprio `axiom.md` já previa como desfecho possível.

### Etapa 6 — Estabilização
- Fixed: `axiom-concurrent` não tinha `package-info.java` no pacote raiz — único módulo com essa
  lacuna na checagem de documentação completa.
- Fixed: `TypeReference` (ver seção `axiom-core` acima) — duplicação de nome/conceito entre
  `axiom-json` e `axiom-yaml`, consolidada.
- Fixed: `sourceSets["examples"]` em `axiom-dotenv`/`axiom-json`/`axiom-yaml`/`axiom-console`
  só herdava `output` do `main`, não seu `compileClasspath`/`runtimeClasspath` — funcionava por
  acidente até um exemplo referenciar um tipo de outro módulo Axiom (primeiro caso real:
  `ConsoleExamples` usando `Result` de `axiom-core`). Corrigido de forma consistente em todos os
  módulos afetados.
- Registered (não corrigido unilateralmente — decisão do mantenedor pendente, ver
  [`docs/CONVENTIONS.md`](docs/CONVENTIONS.md#hierarquia-de-exceções-tensão-em-aberto-registrada-na-etapa-6)):
  a maioria das exceções raiz por módulo (`JsonException`, `YamlException`,
  `FileOperationException`, `ReflectException`, `PromiseException`,
  `ParseFailureException`/`ValidationException`) estende `RuntimeException` diretamente, não
  `AxiomException` como o javadoc deste último promete — só `PlaceholderException` e
  `DotenvException` seguem a convenção hoje. Tensão real entre "toda exceção raiz deveria
  estender `AxiomException`" e "nenhuma dependência sem uso confirmado" (`axiom-reflect`/
  `axiom-io`/`axiom-concurrent`/`axiom-console` não dependiam de `axiom-core` antes disso).
- Verificado: auditoria de dependências (`scripts/audit-deps.sh`), ausência de colisão de pacote
  com `java.*`/`javax.*`, `package-info.java`/`README.md`/`examples/` presentes em todo módulo,
  `./gradlew build` e `make local` verdes nos 10 módulos.
- `PVector`/`PQueue`/`PStack`/`PSortedMap`/`PSortedSet` de `axiom-collections` (Etapa 1)
  completados logo em seguida — ver seção `axiom-collections` acima. Nenhum bloqueador conhecido
  restante para a tag `1.0`.
