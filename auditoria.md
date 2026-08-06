# Auditoria Técnica — JToolBox e Obsidian

> Documento de auditoria pura (somente leitura). Nenhum arquivo das bibliotecas originais foi alterado durante esta análise. Todos os caminhos citados são relativos à raiz de cada projeto (`JToolBox/` ou `Obsidian/`), salvo indicação contrária. Achados de código foram verificados por leitura direta dos arquivos-fonte (não são inferências a partir de nomes).

---

## 0. Visão geral rápida

| | **JToolBox** | **Obsidian** |
|---|---|---|
| Estrutura | Monolito de módulo único (`build.gradle.kts` na raiz) | Multi-módulo Gradle (`obsidian-configuration`, `obsidian-reflection`, `obsidian-promise`, `obsidian-collections`, `obsidian-collections-bench`, `obsidian-tests` + módulo raiz) |
| Escopo declarado | "Framework Java de produtividade com IoC, Web Services e utilidades" | "Framework modular focado em ergonomia, reflection avançada, configuração flexível, abstrações de concorrência e APIs utilitárias" |
| Tamanho | ~166 arquivos `.java`, ~21k linhas | Espalhado em 6 módulos + raiz, superfície comparável mas mais fragmentada |
| Dependências externas reais | gson, snakeyaml, JetBrains annotations, H2 (produção!), 4 artefatos ASM (não usados), lombok | Apenas gson (só em `obsidian-configuration`) e JetBrains annotations; lombok aplicado mas quase não usado |
| Testes | 2 arquivos de teste em todo o projeto | 9 arquivos de teste, concentrados só em `io.obsidian.file.*` |
| Estado de build | **Não compila** (`./gradlew compileJava` falha — ver §1.9) | Não foi executado build completo nesta auditoria, mas escopo por módulo é mais contido |
| Filosofia observada | "faça tudo, do zero, estilo Spring/JUnit" | "várias abstrações funcionais/de dados bem desenhadas, mas fragmentadas e majoritariamente sem teste" |

Os dois projetos nasceram do mesmo tipo de impulso (coleção pessoal de utilitários que foi crescendo), mas divergiram: JToolBox tentou reconstruir frameworks inteiros (DI, servidor web, ORM, test runner); Obsidian manteve módulos menores e mais focados, com destaque para estruturas de dados persistentes e um sistema de Promises maduro, mas sofre de fragmentação de nomenclatura/módulos e cobertura de teste quase nula.

---

## 1. JToolBox

### 1.1 `collections/`

- `collections/Dynamic.java` (~1270 linhas) — coleção "adaptativa": embrulha `ArrayList`/`LinkedList`/`LinkedHashSet` (`StorageMode.ARRAY/LINKED/SET/AUTO`) e troca de implementação em runtime com base em contadores de acesso (`randomAccessCount`, `headOperationCount`, `modificationCount`, limiar fixo `OPTIMIZATION_THRESHOLD=100`). Builder interno, API fluente estilo Stream (`map`, `flatMap`, `filter`, `groupBy`, `partition`, `zip`, `union`/`intersection`/`difference`). É a peça mais ambiciosa e mais bem executada do pacote — a heurística de auto-otimização é ingênua (threshold fixo, sem medir custo real), mas a ideia e a API são sólidas.
- `collections/array/Array.java` — utilitário estático (`@UtilityClass`) construído sobre `Dynamic`, acoplado a `control.For`, `control.Condition` (deprecated) e `control.If`.
- `collections/map/Mapping.java` — classe de 25 linhas com 2 métodos sem relação clara entre si (`returning`: dispatch por `Map<K,Supplier>`; `applyReplacements`: template string). Nome desproporcional ao conteúdo.

### 1.2 `concurrent/`

- `concurrent/Threader.java` — `@UtilityClass` fluente sobre `ExecutorService`. Pool padrão estático `Executors.newCachedThreadPool()` **sem limite configurável de tamanho** (risco clássico de crescimento descontrolado sob carga). Builder (`ThreadBuilder<T>`: `named`, `daemon`, `useSimpleThread`, `useExecutor`, `onCompletion`, `onError`) retorna `Future<T>`. Sem circuit-breaker nem rate limiting — wrapper fino, correto para uso simples.

### 1.3 `config/` — sistema de env declarativo por anotação

- `config/core/EnvEngine.java` — fachada estática que orquestra tudo (`initialize`, `inject`, `reload`, `dump`, `entries`). Estado mutável estático (`dotenv`, `injector`, `activeProfile`) **sem sincronização** — não thread-safe.
- `config/core/EnvInjector.java` — injeta campos `@Env` via reflection, resolve `@EnvPrefix`/`@EnvGroup`, aplica `@Required`/`@Decrypt`, dispara `EnvValidator`, mantém registro de campos `@Reloadable`.
- `config/core/EnvProxy.java` — proxies JDK dinâmicos (`Proxy.newProxyInstance`) para interfaces anotadas, com cache por `Method` (exceto quando `@Reloadable`).
- `config/core/EnvValidator.java` — validação declarativa (`@NotNull`, `@NotEmpty`, `@Min`, `@Max`, `@Regex`, `@MinLength`, `@MaxLength`) via cadeia de `instanceof` — não extensível sem editar a classe.
- `config/examples/UsageExamples.java` — classe demo executável (`main`), claramente código morto dentro de `src/main`.

### 1.4 `dotenv/` — parser de `.env` puro

`dotenv/Dotenv.java` + `dotenv/DotenvBuilder.java` (fluente: `filename`, `directory`, `systemProperties`, `throwIfMissing`/`ignoreIfMissing`, `throwIfMalformed`/`ignoreIfMalformed`) + `dotenv/util/{DotenvParser,DotenvReader,ClasspathResourceLoader}.java`. `DotenvBuilder.DotenvImpl` faz merge do `.env` com `System.getenv()`, com precedência do ambiente real do SO.

**`config/` usa `dotenv/` internamente** (`EnvEngine.initialize()` instancia `new DotenvBuilder()...load()`) — portanto não são dois sistemas paralelos redundantes, mas camadas corretas (parsing puro vs. DI declarativo).

**Porém existe um terceiro sistema, desconectado dos outros dois**: `ioc/environment/Environment.java`, usado pelo container de IoC, reimplementa do zero carregamento de `application.properties`, detecção de profile (`jtoolbox.profiles.active`/`JTOOLBOX_PROFILES_ACTIVE`), variáveis de ambiente (prefixo `JTOOLBOX_`), argumentos de linha de comando (`--key=value`) e resolução de placeholders `${prop:default}` — **sem usar `dotenv/` nem `config/`**. São três formas distintas e não interoperáveis de resolver configuração externa dentro da mesma biblioteca — duplicação real, não apenas camadas.

### 1.5 `console/`, `control/`, `datetime/`, `text/`, `util/`

- `console/IO.java` — fachada estática de I/O (`print`, `println`, `format` com placeholders `{}`/`%s`, `withOut`/`withErr`), acoplada a `control.Condition` (deprecated) e `control.If`.
- `control/If.java` (766 linhas) — DSL de controle de fluxo fluente e bem documentada: `ActionBuilder`/`ElseBuilder` (`when().then().otherwise()`), `ValueBuilder`/`ValueResolver` (`whenValue().compute().orElse()`), `PatternMatcher` (`match(value).when(pattern, supplier).orElse()`), mais `throwIf`, `requireNonNull`, `choose`, `ifElse`.
- `control/Condition.java` — **a própria interface está anotada `@Deprecated`**, mas ainda é referenciada ativamente por `console/IO.java`, `collections/array/Array.java`, `concurrent/Threader.java`, `logger/Logger.java`. Migração interna incompleta: código novo (`If`) e código deprecated (`Condition`) coexistem sendo usados pela própria lib.
- `control/For.java`, `control/Switch.java` — DSLs equivalentes para loop e switch fluente.
- `datetime/DateTime.java` — wrapper de `LocalDateTime`/`ZonedDateTime` com formatos nomeados, usado por `Logger`.
- `text/{StringFormatter,Stringifier,StringEnhancer,NumberFormat}.java` — `StringFormatter.format` implementa um mini-template engine com placeholders `{}` (estilo SLF4J), usado por `Logger`, `AssertX`, `Serializer`.
- `util/Try.java` (819 linhas) — monad `Try<T>` completo estilo Vavr (`map`, `flatMap`, `recover`, `recoverIf`, `retry`, `retryWithBackoff`, `sequence`, `traverse`, `combine`, `withTimeout`, `parallel`, `sneakyThrow`). **Bug real confirmado**: `Try.retry(int)` de instância (`util/Try.java:509-519`) contém o comentário do próprio autor `// This won't work properly - need original supplier` — o método não reexecuta a operação original, apenas recria um `Try` de sucesso já resolvido, virando um no-op em caso de falha. A sobrecarga estática `Try.retry(maxAttempts, delay, supplier)` está correta.
- `util/timer/{Timer,Scheduler,TimerBuilder,TimerObserver,Mode}.java` — `Timer` usa `synchronized` nos métodos de ciclo de vida + `AtomicBoolean` + `volatile long` + `CopyOnWriteArrayList` de observers: **é o único ponto do código com desenho consciente de thread-safety**, o que reforça que os problemas de concorrência achados em outros pacotes (§1.7) são falta de revisão, não de conhecimento.

### 1.6 `file/`

`file/File.java` delega a `file/util/{Paths,Files,Directory}.java`; `file/util/FileWatcher.java` embrulha `WatchService` do NIO.

### 1.7 `http/` — mini framework web construído do zero

- `http/server/Server.java` — construído sobre `com.sun.net.httpserver.HttpServer` (JDK embutido, não Netty/Undertow). Builder fluente (`Server.port(8080).get(...).use(middleware).enableCors().start()`). **Usa `Executors.newVirtualThreadPerTaskExecutor()`** (`http/server/Server.java:162`) — o uso mais moderno de Java 21 em todo o repositório. Roteamento por **busca linear** em `List<Route>` (`findRoute`) — O(n) por request, sem trie/radix tree. `staticFiles(...)` é placeholder não implementado.
- `http/route/RoutePattern.java` — compila padrões `/users/:id` para regex, com parâmetros opcionais (`:id?`) e wildcard (`/*`).
- `http/client/{ClientRequest,ClientResponse}.java` — cliente fluente sobre `java.net.http.HttpClient`.
- `http/{Http,HttpContext,Method}.java`, `http/middleware/`, `http/handler/`, `http/status/StatusCode.java`.

### 1.8 `ioc/` — clone estrutural do núcleo do Spring

- `ioc/JToolboxApplication.java` — bootstrap estilo `SpringApplication.run()`, com banner ASCII próprio.
- `ioc/context/ApplicationContext.java` — ciclo de vida em fases (`LifecyclePhase`: PREPARING → LOADING_BEAN_DEFINITIONS → PROCESSING → INSTANTIATING → INITIALIZING → REFRESHING → READY → SHUTTING_DOWN → CLOSED), `@ConditionalOnClass`/`@ConditionalOnProperty` implementados como classes internas.
- `ioc/context/BeanFactory.java` — criação de beans via construtor ou `@Bean`, detecção de dependência circular via `ConcurrentHashMap.newKeySet()`, resolução de `@Autowired`/`@Inject`/`@Qualifier`/`@Value`.
- `ioc/context/BeanRegistry.java` — usa `ConcurrentHashMap`, mas os `List<String>` dentro de `typeIndex` são `ArrayList` mutados sem lock adicional (`BeanRegistry.java:26-27,39-40`) — corrida potencial em registro concorrente.
- `ioc/context/PackageScanner.java` — scanner de classpath **próprio e primitivo**, baseado em `File.listFiles()` recursivo sobre `URL` de classloader. **Não funciona dentro de JARs** — só funciona com `.class` soltos em diretório, limitação séria fora de ambiente de desenvolvimento.
- `ioc/environment/Environment.java` — terceiro sistema de config, ver §1.4.
- `ioc/web/WebServerInitializer.java` integra `ioc/` com `http/server/Server` (padrão MVC do Spring: `@RestController`, `@GetMapping`, `@PathVariable`, `@RequestBody`).
- `ioc/annotations/` — ~25 anotações espelhando Spring quase 1:1 (`@Autowired`, `@Bean`, `@Component`, `@Configuration`, `@Service`, `@Repository`, `@Qualifier`, `@Primary`, `@Lazy`, `@Order`, `@Value`, `@PostConstruct`, `@PreDestroy`, `@ConditionalOnClass`, `@ConditionalOnProperty`, `@ComponentScan`, `@EnableAutoConfiguration`, `@Import`, `@RestController`, `@*Mapping`, `@PathVariable`, `@RequestBody/Header/Param`).

**Avaliação:** não é um utilitário de DI leve — é uma reimplementação estrutural do núcleo do Spring (BeanFactory + ApplicationContext + ciclo de vida em fases + conditional beans + component scan + MVC) dentro de uma "toolbox". Sem suporte a AOP, sem funcionamento correto em JAR, sem testes de integração aparentes. Escopo excessivo e risco de manutenção alto — qualquer uso real tende a preferir Spring Boot.

### 1.9 `jdbc/` — mini Spring JDBC

- `jdbc/Jdbc.java` — builder **imutável** (`@Contract(value = "_ -> new")` em cada método) para montar URL/conexão por `DatabaseType` (POSTGRESQL, MYSQL, SQLITE, ORACLE, SQLSERVER, H2) com resolução automática de driver; `toDataSource()` retorna implementação anônima de `javax.sql.DataSource` que lança `UnsupportedOperationException` em métodos não usados.
- `jdbc/dao/JdbcTemplate.java` — clone direto do `JdbcTemplate` do Spring (`update`, `query`, `queryForObject`, `insertAndReturnKey`, `batchUpdate`, `inTransaction`, `execute`) com uso correto de try-with-resources.
- `jdbc/dao/{NamedParameterJdbcTemplate,BeanPropertyRowMapper,RowMapper,PreparedStatementSetter,DataAccessException}.java`.
- **Gradle task morta**: `build.gradle.kts` registra `runExample` apontando para `com.github.rickmvi.jtoolbox.jdbc.runner.DbExampleRunner` — **classe inexistente em todo o source tree**. Task de build quebrada.
- `com.h2database:h2:2.1.214` está declarado como `implementation` (dependência de **produção**), aparentemente só para exemplos/testes — deveria ser `testImplementation` ou removida.

**Avaliação:** clone competente de `JdbcTemplate`, mas redundante com o Spring JDBC real que os usuários já conhecem.

### 1.10 `json/` — wrapper sobre Gson

`json/JsonX.java` (fachada com métodos de instância e estáticos equivalentes que criam nova `Gson` a cada chamada estática — custo de alocação repetida em hot path), `json/{JsonXBuilder,JsonXConfig,JsonXMapper,JsonXPath}.java`, `json/exception/JsonXException.java`.

### 1.11 `logger/`

`logger/Logger.java` — `@UtilityClass` 100% estático. **Estado mutável global sem sincronização**: `ENABLED_LEVELS` (`EnumSet`, mutado por `enable`/`disable`/`enableAll`/`disableAll` sem lock) e `useAnsiColor` (`@Setter` público estático) — em uso concorrente (`http/server` com virtual threads, `ioc/` multi-thread) é uma race condition clássica. Mistura `Condition` (deprecated) e `If` no mesmo arquivo (`Logger.java:20,25,96-99`).

**Bug de API confirmado por compilação real**: `test/TestRunner.java:25` chama `Logger.getLogger(TestRunner.class)`, método **inexistente** em `Logger`. Rodando `./gradlew compileJava` diretamente, o projeto **falha ao compilar**:
```
test/TestRunner.java:25: error: cannot find symbol
    private static final Logger LOGGER = Logger.getLogger(TestRunner.class);
symbol:   method getLogger(Class<TestRunner>)
location: class Logger
```
Isso significa que o estado atual do repositório JToolBox não compila e não pode ter passado por CI recente — qualquer avaliação de maturidade deve considerar isso um sinal vermelho forte.

### 1.12 `serializable/`

`serializable/Serializer.java` combina JSON (via `JsonX`), serialização binária Java (`ObjectOutputStream`/`ObjectInputStream`) e Base64. `saveToBinaryFile` serializa para bytes e depois passa por `Stringifier.toString(bytes)` para gravar como "texto" (`Serializer.java:163-170`) — semanticamente questionável; se `Stringifier.toString(byte[])` não fizer Base64 internamente, é um risco de corrupção de dados binários.

### 1.13 `test/` — mini framework de testes do zero

`test/TestRunner.java` (runner com `@BeforeAll/@BeforeEach/@AfterEach/@AfterAll`, `@Disabled`, `@DisplayName`, relatório colorido — **não compila**, ver §1.11), `test/AssertX.java` (fachada fluente estilo AssertJ com overload por tipo, delegando a wrappers em `test/wrapper/*`), `test/Mocks.java`, `test/annotations/*` (`@JTest`, ciclo de vida, `@FastTest`/`@SlowTest`/`@RepeatedTest`/`@Tag` — espelha JUnit 5 quase anotação por anotação).

**Avaliação:** reimplementação completa de JUnit 5 dentro de uma toolbox que **já declara JUnit real como dependência de teste** (`testImplementation(libs.junitjupiter)`) — redundância pura sem justificativa de nicho.

### 1.14 `yaml/`

`yaml/YamlConfig.java` embrulha SnakeYAML com acesso por path pontuado (`getString("db.host")`); duas classes chamadas `Yaml` no mesmo arquivo (`org.yaml.snakeyaml.Yaml` importada sem alias ao lado de `com.github.rickmvi.jtoolbox.yaml.Yaml`, ver `YamlConfig.java:7`) exigem cuidado de import.

### 1.15 Uso de ASM — dependências mortas

`grep -rn "objectweb"` em todo `src/main/java` retorna **zero resultados**. As quatro dependências `org.ow2.asm:{asm,asm-commons,asm-tree,asm-util}:9.6` (`build.gradle.kts:29-32`, sob comentário "ASM para manipulação de bytecode") **não são usadas em nenhum lugar do código**. Provavelmente planejadas para proxies/AOP dinâmicos no `ioc/` e nunca implementadas. Peso morto real no artefato e no tempo de build.

### 1.16 Nomenclatura

- Sufixo "X" inconsistente: usado em todo `json/` (`JsonX`, `JsonXBuilder`, `JsonXMapper`, `JsonXPath`, `JsonXConfig`, `JsonXException`) e isoladamente em `test/AssertX.java`, mas ausente em `yaml/` — sem significado documentado, parece estilo, não convenção.
- `control.Condition` (deprecated) vs `control.If` coexistindo, com a própria lib usando o deprecated internamente.
- Duas classes de conversão de tipo com o mesmo nome simples em pacotes diferentes: `util/TypeConverter.java` e `config/core/TypeConverter.java` — candidatas certas a unificação.
- Prefixo "J" inconsistente (`JsonX`, `JTest`, `JToolboxApplication`) usado só em parte da API.

### 1.17 Uso de Lombok

Extensivo (33 arquivos) e idiomático: `@UtilityClass` em quase todas as fachadas estáticas, `@Getter`/`@Setter` com `AccessLevel` customizado, `@RequiredArgsConstructor` em builders internos, `@EqualsAndHashCode` pontual. Configuração `compileOnly` + `annotationProcessor` correta. Não há `@Data` genérico espalhado — uso disciplinado.

### 1.18 Concorrência e encapsulamento — problemas concretos

- `logger/Logger.java`: `EnumSet` e `boolean` estáticos mutados sem sincronização, usados por código multi-thread (`http/server`, `ioc/`).
- `config/core/EnvEngine.java`: campos estáticos mutados sem sincronização (`initialize`/`reload`/`reset` concorrentes corrompem estado).
- `ioc/context/BeanRegistry.java`: `ArrayList` dentro de `ConcurrentHashMap` mutado sem lock adicional.
- `concurrent/Threader.DEFAULT_POOL`: `newCachedThreadPool()` sem limite.
- Contraponto positivo: `util/timer/Timer.java` mostra desenho de concorrência correto quando há atenção — reforça que os problemas acima são falta de revisão, não de capacidade.
- Não há campos `public static` mutáveis (zero ocorrências confirmadas via grep), mas há vários `private static` mutáveis sem sincronização — mesmo risco, escondido.
- Padrão de exceções por domínio (`ConfigException`, `InjectionException`, `ProxyException`, `ValidationException`, `DotenvException`, `DataAccessException`, `JsonXException`, `AssertionException`) é consistente e bom. Alguns `catch` silenciosos que engolem exceção sem log (`EnvEngine.java:131-133`; `Try.java:553,567`).

### 1.19 O que é morto, exemplo ou não essencial

- `config/examples/UsageExamples.java` — demo executável dentro de `src/main`.
- Dependências ASM — peso morto total.
- Gradle task `runExample` — aponta para classe inexistente.
- `control/Condition.java` — interface inteira deprecated, mantida só por compat incompleta.
- `http/server/Server.staticFiles(...)` — placeholder não implementado.
- READMEs fragmentados por pacote (`collections/README.md`, `console/README.md`, `control/README.md`, `logger/README.md`, `text/README.md`) — nem todo pacote tem, documentação inconsistente.
- `com.h2database:h2` como dependência de produção sem justificativa clara.

---

## 2. Obsidian

### 2.0 Build e dependências

- `Obsidian/build.gradle.kts` (raiz): `java-library`, `maven-publish`, `com.vanniktech.maven.publish`, `signing`; publicação real para Maven Central + GitHub Packages; toolchain Java 21 aplicado via bloco `subprojects {}`.
- `Obsidian/gradle/libs.versions.toml` declara `jackson`, `dotenv` (io.github.cdimascio), `snakeyaml`, `reflections` (org.reflections), `slf4j` — **nenhuma dessas é usada em qualquer `build.gradle.kts` real**. Catálogo com entradas mortas/planejadas e nunca executadas.
- Dependências efetivamente usadas: raiz e `obsidian-promise`/`obsidian-reflection` só `libs.annotations`; `obsidian-collections` e `obsidian-tests` **zero dependências externas**; `obsidian-configuration` é o único módulo com dependência de terceiros real (`gson`); `obsidian-collections-bench` usa JMH 1.37 + `project(":obsidian-collections")` (única dependência inter-módulo de todo o repositório).
- Lombok aplicado globalmente via Gradle, mas só efetivamente **usado** em `obsidian-configuration` (7 arquivos, majoritariamente `@Getter` e um `@UtilityClass`) — todo o resto do código escreve boilerplate manual apesar de ter Lombok disponível.

### 2.1 `obsidian-collections` — estruturas de dados persistentes

Interfaces núcleo (`obsidian.collections`): `OCollection`, `OMap`, `OQueue`, `OSequence`, `OSet`, `OSortedMap`, `OSortedSet`, `OStack`, `OVector`, todas estendendo as interfaces `java.util.*` equivalentes e adicionando `plus`/`plusAll`/`minus`/`minusAll` (retornam nova instância); mutadores herdados de `java.util` são `@Deprecated`/lançam `UnsupportedOperationException`. `Empty.java` centraliza instâncias vazias; `P.java` (385 linhas) é facade estática estilo `List.of`/`Set.of`, incluindo builders de mapa com resolução de chaves duplicadas (`failOnDuplicateKeys()`, `keepFirst()`, `keepLast()`).

**Implementação verificada como séria, não superficial:**
- `hamt/Hashing.java` (165 linhas) — MurmurHash3 real (finalizador de mixagem `h ^= h>>>16; h *= 0x85ebca6b; ...`), extração de segmentos de 5 bits, bitmap de presença, índice populacional via `Integer.bitCount`.
- `HashTriePMap.java` (644 linhas) — HAMT completo, 4 tipos de nó (`EmptyNode`, `LeafNode`, `CollisionNode`, `BitmapIndexedNode`), branching factor 32, **path copying real** (`put`/`remove` clonam só o array de filhos e bitmap do nó afetado, retornam `this` quando não há mudança). Nome quase idêntico a `HashPMap` da lib real `org.pcollections`, sugerindo inspiração direta.
- `HashTrieOSet.java` — delega para `HashTriePMap<E,Object>` interno com sentinel, exatamente como Clojure implementa `PersistentHashSet` sobre `PersistentHashMap`.
- `ChunkedOVector.java` (457 linhas) — vetor persistente, mas **não é uma árvore RRB/bitmapped-vector completa**: é um array 2D de blocos fixos de 32 elementos (2 níveis apenas). `get`/`append` são O(1) real, mas inserção/remoção no meio (`plus(int,E)`, `minus(int)`) são O(n) documentados (reconstroem via lista).
- `AmortizedOQueue.java` (331 linhas) — fila persistente clássica de duas pilhas (Okasaki), amortização O(1) corretamente implementada.
- `ConsOStack.java`, `TreeOMap.java`, `TreeOSet.java` — presentes, com javadoc consistente.

**Risco crítico**: **zero testes unitários** (`obsidian-collections/src/test` não existe) — só há benchmarks JMH em `obsidian-collections-bench`, que medem performance, não corretude.

**Overlap com `collections/` do JToolBox**: nenhum real — JToolBox (`Array`, `Dynamic`, `Mapping`) é sobre coleções *mutáveis* utilitárias; Obsidian é sobre coleções *persistentes/imutáveis*. Capacidades complementares, não duplicadas.

### 2.2 `obsidian-configuration` — três domínios sob um nome

O módulo mistura três domínios de responsabilidade sem relação de acoplamento entre si:

**a) `io.obsidian.dotenv`** — `Dotenv`/`DotenvBuilder` (fluente: `filename`, `directory`, `systemProperties`, `throwIfMissing`/`ignoreIfMissing`, `throwIfMalformed`/`ignoreIfMalformed`), anotações `@Env`, `@EnvPrefix` (`@Inherited`), `@RequiredEnv`, `@Default`, `@EnvIgnore`, `DotenvInjector` (usa `java.lang.reflect.Field` **puro**, não a própria API `obsidian-reflection` — oportunidade de reuso interno perdida). Sem testes.

**b) `io.obsidian.file`** — `File.java` (204 linhas, fachada de entrada), `FileHandle.java` (541 linhas, API fluente encadeável, usa Lombok `@Getter`), `Directory.java` (176 linhas, Lombok), `attribute/*`, `hash/*` (Strategy pattern: `Md5Hash`, `Sha256Hash`), `io/{FileReader,FileWriter}`, `operation/{FileCompressor,FileOperations}`, `search/FileSearch`, 6 exceções especializadas. **Único subsistema do módulo com testes reais** (9 arquivos em `src/test/java/io/obsidian/file/`).

**c) `obsidian.json`** — facade `Json.java` com javadoc explícito: *"usa Gson como motor interno mas não o expõe na API pública"*. Modelo de dados JSON próprio (`JsonArray`, `JsonElement`, `JsonObject`, `JsonPrimitive`, `JsonNull` — não reexporta tipos do Gson), implementação real isolada em pacote `internal.gson.*` (`GsonEngine`, `GsonMapper`, `GsonElementBridge`, `GsonAnnotationProcessor`, `GsonCodecAdapter`) que converte entre o modelo próprio e o do Gson. Boa separação (encapsulamento correto do motor de serialização). Sem testes.

**Avaliação de coesão**: o nome "obsidian-configuration" **não reflete o conteúdo real** — dotenv, file I/O e JSON não têm dependência lógica entre si. Um consumidor que só quer ler `.env` arrasta transitivamente `gson` (usado só pelo JSON) e todo o código de file I/O. Problema real de modularização Gradle.

**Comparação com JToolBox**: o dotenv do Obsidian tem injeção declarativa por anotação com conversor de tipos dedicado (`DotenvTypeConverter`) e política de erro configurável — design mais "Spring-like"/declarativo que o `dotenv/` isolado do JToolBox, mas sem testes (enquanto o `dotenv/` do JToolBox também não tem testes visíveis). O `config/` do JToolBox é mais completo em recursos (profiles, `@Reloadable`, `@Decrypt`, validação) mas sofre da fragmentação em três sistemas de config (§1.4).

### 2.3 `obsidian-promise` — Promise/Future estilo JavaScript

`api/Promise.java` (294 linhas — **sem header de licença Apache**, inconsistente com o resto do código): interface imutável e rica — `map`, `flatMap`/`then`, `tap`, `filter`, `recover`/`recoverWith`, `catchError` (tipado por classe de exceção e genérico), `mapError`, `finallyDo`, `timeout(Duration)`, `delay(Duration)`, `retry(RetryPolicy)`/`retry(int)`, estado (`isPending`/`isFulfilled`/`isRejected`/`isCancelled`), `cancel()`/`cancel(reason)`, callbacks, bloqueantes (`get()`, `get(Duration)`, `getOrDefault`, `getOrElse`), interop `toCompletableFuture()`.

- `RetryPolicy` — fábricas `simple(n)`, `exponential(n, initialDelay)`, builder fluente completo (`maxAttempts`, `fixedDelay`, `exponentialBackoff`, `maxDelay`, `withJitter()`, `retryIf`, `retryOn`).
- `internal/backoff/{ExponentialBackoff,FixedBackoff,JitteredBackoff,NoBackoff}.java` — Strategy pattern real; `ExponentialBackoff` implementa `initialDelay * multiplier^(attempt-1)` com cap de `maxDelay` e jitter opcional matematicamente correto.
- `internal/cancellation/{DefaultCancellationSource,DefaultCancellationToken,NoCancellationToken,PreCancelledToken}.java` — cancelamento cooperativo maduro, análogo a `CancellationToken` do .NET/`AbortController` do JS.
- `combinators/{PromiseAll,PromiseAny,PromiseRace}.java`, `error/{AggregateException,CancellationException,PromiseException,TimeoutException}.java`.

**Avaliação**: a API mais completa e "pronta para produção" em superfície de todo o Obsidian. JToolBox não tem equivalente (só `Threader`, mais primitivo) — capacidade inteiramente nova. **Risco**: zero testes — a robustez real sob concorrência (race conditions em `cancel()`, propagação de estado) não está verificada, só a superfície de API.

### 2.4 `obsidian-reflection` — pacote `lang.reflect`

Pacote real (confirmado por leitura de diretório): **`lang.reflect`**, não `obsidian.reflect`. `Reflect.on(Class)`/`Reflect.on(Object)`, `create()`/`create(Object...)`, `field(name)`→`Field`, `fields()`→`Fields`, `method(name)`→`ReflectMethod`, `methods()`→`ReflectMethods`, `annotations()`→`ReflectAnnotations`, `bind(Object)`, exceções checked convertidas em `ReflectException` unchecked.

**Problema de nome real e confirmado**: o pacote `lang.reflect` colide textualmente com `java.lang.reflect`. Java não permite importar `Field` de ambos sob o mesmo nome simples no mesmo arquivo — e isso já afeta o **próprio código-fonte** do módulo: `Reflect.java` precisa referenciar `java.lang.reflect.Modifier`/`Proxy` de forma totalmente qualificada enquanto define sua própria `Field` no pacote `lang.reflect`.

**Valor real**: agrega valor genuíno (API fluente, unificação de exceções checked em uma unchecked, abstrações de coleção `Fields`/`ReflectMethods` para operar sobre múltiplos membros). Não há evidência de cache de lookups de `Method`/`Field` nas assinaturas lidas — oportunidade de melhoria de performance perdida. Sem testes.

### 2.5 `obsidian.control` (módulo raiz) — DSL de decisão

`control/When.java` (126 linhas) — fachada única com várias famílias de API: predicados (`when(boolean)`, `whenNotNull`, `whenPresent(Optional)` → `ActionWhen`), encadeamento if/elseif/else (`When.chain()` → `DecisionChain`), seleção de valor tipado (`When.value(boolean)` → `ChooseWhen<T>`, `When.choose()` → `ChooseChain<T>`), pattern matching por igualdade (`When.match(value)` → `MatchPattern<T>`), preconditions delegadas a `Preconditions`, atalhos imperativos (`ifElse`, `choose`, `onlyIf`, `unless`).

`control/util/action/DecisionChain.java` (110 linhas) — if/elseif/else fluente com **avaliação antecipada (eager)** das condições (mesmo a sobrecarga `when(BooleanSupplier)` avalia imediatamente) — só o corpo de `then(Runnable)` é lazy. Validações de uso incorreto via `IllegalStateException`.

**Comparação com `control/` do JToolBox**: overlap conceitual real (ambos são "DSL de controle de fluxo"), mas o Obsidian tem design mais rico em variantes (5: `ActionWhen`, `ChooseWhen`, `ChooseChain`, `DecisionChain`, `MatchPattern`) contra a abordagem mais direta de `If`/`Switch`/`For`/`Condition` do JToolBox. Nenhuma das duas bibliotecas resolveu esse domínio de forma definitiva — ambas têm versões concorrentes internas (Obsidian: várias variantes de `When`; JToolBox: `Condition` deprecated coexistindo com `If`).

### 2.6 `obsidian.experimental.io.scan` — scanner de console testável

Todo o pacote vive sob `obsidian.experimental.io.scan` — **rótulo `experimental` explícito do próprio autor**. `InputScanner.console()`/`console(PromptEnvironment)`/`fromString(String)`/`fromReader(Reader)`; `InputSource` (interface `AutoCloseable`) com implementações `ConsoleSource`, `ReaderSource`, **`StringSource`** — esta última é o que torna o scanner **unit-testável de verdade** (simula entrada de usuário sem tocar em stdin real). Arquitetura em camadas: fonte (`InputSource`) → parsing tipado (`Parser`/`Parsers`) → validação (`Validator`/`Validators`) → prompt (`Prompt`/`Prompts`/`Messages`) → resultado estruturado (`ScanResult` com `ErrorCode`/`Error`, não exceção crua).

**Avaliação**: arquitetura claramente superior em testabilidade e separação de responsabilidades a um wrapper direto sobre `Scanner`/`System.in`. Contraditoriamente, é o componente de melhor design do repositório e está rotulado como "experimental" — sugere interrupção antes de "graduar" para estável, não falta de qualidade. Sem testes formais confirmados apesar da testabilidade da arquitetura.

### 2.7 `obsidian.functional` (módulo raiz) — Try e Failable*

`functional/Try.java` (663 linhas) — tipo imutável com `Success`/`Failure` como únicas subclasses. `Try.success`/`Try.failure`/`Try.of(FailableSupplier)` (relança `Error` para não mascarar erros fatais da JVM) /`Try.run`/`Try.withResources`/`Try.flatten`. Transformações: `map`, `flatMap`, `mapTry` (permite exceção checked dentro do mapper — distinção real de `map`), `filter`, `recover`/`recoverWith` (genérico e tipado por classe de exceção), `toOptional`, `getOrElse`, `orElse`, `transform`, `andThen`, `peek`/`peekFailure`/`onFailure`, `fold`, `mapFailure`, `getOrThrow`, `checkedGet()` (rethrow fiel do tipo original). `functional/failable/{FailableConsumer,FailableFunction,FailableRunnable,FailableSupplier}.java` permitem `throws Exception` em lambdas.

**Overlap direto com `util/Try.java` do JToolBox** (mesmo propósito, "Try monad"). A implementação do Obsidian é notavelmente mais completa em algumas dimensões (`mapTry`, `checkedGet()`, `recover` tipado por classe de exceção) — candidata natural a implementação canônica, mas ambas precisam ser comparadas a fundo antes de decidir qual herdar (a do JToolBox tem `retryWithBackoff`, `sequence`, `traverse`, `combine`, `withTimeout`, `parallel`, que não foram confirmados no Obsidian).

### 2.8 `obsidian.util` (módulo raiz) — Maybe, Result, Box/AtomicBox, Range/Sequence

- `util/Maybe.java` — **`sealed interface Maybe<T> permits Maybe.Some, Maybe.None`** (uso real de `sealed` do Java 17+/21). Equivalente a `Optional`.
- `util/Result.java` — **`sealed interface Result<T, E> permits Result.Ok, Result.Err`**, com erro **tipado** `E` (estilo Rust). Javadoc do próprio arquivo distingue explicitamente: *"Optional<T> → talvez um valor; Result<T,E> → ou um valor ou um erro."*
- **Três tipos de "outcome" convivendo no mesmo projeto**: `Maybe` (presença/ausência), `Result<T,E>` (sucesso/erro tipado arbitrário), `Try<T>` em `functional/` (sucesso/`Throwable`). Cada um cobre um caso ligeiramente diferente, mas é uma superfície conceitual grande — risco de redundância interna, não apenas frente ao JToolBox.
- `util/concurrent/{Box,atomic/PlainBox,atomic/AtomicVolatileBox,atomic/AtomicBox}.java` — três garantias de concorrência **verificadas por leitura de código, batendo exatamente com a documentação**: `PlainBox` (campo simples, zero garantia), `AtomicVolatileBox` (`volatile`, visibilidade sem CAS), `AtomicBox` (`AtomicReference` interno, CAS real via `compareAndSet`/`getAndSet`).
- `util/stream/Range.java` — `intRange`/variantes long via `IntStream.iterate(...).takeWhile(...)`, suporta ranges ascendentes/descendentes pelo sinal relativo.
- `util/stream/Sequence.java` — wrapper sobre `Stream` com recriação lazy via `Supplier<Stream>` (permite reprocessar, ao contrário de `Stream` cru single-use), integra com `Try` e `CompletableFuture`.

### 2.9 `obsidian-tests` — módulo fantasma

`obsidian-tests/build.gradle.kts` contém apenas `plugins { java }`. **Não existe diretório `src`** dentro do módulo. Scaffold morto, nunca populado.

### 2.10 Convenções e sinais de incompletude

- **Cobertura de teste por módulo**: `obsidian-configuration` tem 9 testes, mas só cobrindo `io.obsidian.file.*` (nem dotenv nem json têm testes); `obsidian-collections`, `obsidian-promise`, `obsidian-reflection` e o módulo raiz têm **zero testes**; `obsidian-tests` está vazio; `obsidian-collections-bench` só tem benchmarks JMH (não testam corretude).
- Prefixo "O" em collections (`OMap`, `OSet`, etc.) é consistente e não colide com nada do JDK — ao contrário de `lang.reflect`.
- Javadoc extremamente denso e padronizado (`<h2>Overview</h2>`, `<h2>Complexity</h2>`, `<h2>Design notes</h2>`, exemplos `<pre>{@code ...}</pre>`) na maioria dos arquivos centrais — qualidade acima da média para um projeto pessoal.
- Inconsistência de licenciamento: `Promise.java` não tem o cabeçalho Apache 2.0 presente nos demais arquivos.
- Lombok disponível globalmente mas usado só em `obsidian-configuration` — inconsistência de padrão.
- Pacote `experimental` reflete cronologia/intenção do autor, não maturidade real medida por teste (o componente de melhor design, `io.scan`, está lá; módulos sem nenhum teste como `promise`/`reflection` estão fora e aparentam "estáveis").
- Nenhum `build.gradle.kts` de submódulo depende de outro submódulo do próprio Obsidian, exceto `obsidian-collections-bench` → `obsidian-collections` — os módulos foram desenvolvidos de forma isolada (ex.: `DotenvInjector` não reusa `obsidian-reflection`).

---

## 3. Código duplicado ou sobreposto entre as duas bibliotecas

| Capacidade | JToolBox | Obsidian | Overlap |
|---|---|---|---|
| Try/monad de erro | `util/Try.java` (819 linhas, `retryWithBackoff`, `sequence`, `traverse`, `combine`, `withTimeout`, `parallel`; bug em `retry()` de instância) | `functional/Try.java` (663 linhas, `mapTry`, `checkedGet()`, `recover` tipado) | **Direto** — mesma proposta de valor, implementações diferentes, cada uma com pontos fortes que a outra não tem |
| Parser de `.env` | `dotenv/` (builder + parser) | `obsidian-configuration/.../dotenv/` (builder + injeção por anotação + conversor de tipo) | **Direto**, mas Obsidian é mais declarativo/rico em anotações |
| DSL de controle de fluxo | `control/{If,Condition,For,Switch}` | `obsidian.control.{When,DecisionChain,ChooseChain,MatchPattern}` | **Conceitual**, implementações e filosofias distintas |
| Scanner/leitura de console | `console/{Scan,Scanf,IO}` (sobre `Scanner`/`System.in` direto) | `obsidian.experimental.io.scan.*` (arquitetura em camadas com `InputSource` plugável) | **Direto**, Obsidian estruturalmente superior em testabilidade |
| Conversão de tipo | `util/TypeConverter.java` + `config/core/TypeConverter.java` (duplicado *dentro* do próprio JToolBox) | `DotenvTypeConverter` (Obsidian) | Overlap triplo de fato |
| JSON | `json/JsonX*` (wrapper fino sobre Gson, expõe pouco encapsulamento) | `obsidian.json.*` (modelo de dados próprio, Gson escondido em `internal.gson.*`) | **Direto** — Obsidian tem melhor encapsulamento do motor |
| Collections | `collections/{Array,Dynamic,Mapping}` (mutáveis, adaptativas) | `obsidian-collections/*` (persistentes/imutáveis, HAMT) | **Nenhum overlap real** — complementares |
| Promise/Future | `concurrent/Threader` (wrapper simples de `ExecutorService`) | `obsidian-promise/*` (Promise completo com retry/backoff/cancelamento/combinators) | **Assimétrico** — Obsidian tem capacidade que JToolBox não tem |
| Reflection fluente | Uso direto de `java.lang.reflect` espalhado (`EnvInjector`, `BeanFactory` etc.) | `obsidian-reflection` (`lang.reflect.Reflect` fluente) | **Assimétrico** — Obsidian tem abstração dedicada que JToolBox não tem |
| Result/Optional/Either | Nenhum tipo dedicado (`Try` cobre parcialmente) | `Maybe`, `Result<T,E>`, `Try` (três tipos!) | **Assimétrico**, com redundância *interna* ao Obsidian |

---

## 4. Código potencialmente obsoleto, desnecessário ou removível

**JToolBox:**
- `config/examples/UsageExamples.java` (demo em `src/main`).
- Dependências ASM (`org.ow2.asm:*`) — zero uso confirmado.
- Gradle task `runExample` (aponta para classe inexistente).
- `control/Condition.java` (interface inteira deprecated).
- `http/server/Server.staticFiles(...)` (placeholder).
- `ioc/`, `http/`, `jdbc/`, `test/` inteiros, como reimplementações de frameworks já resolvidos — candidatos a **não** ir para Axiom (ver §6).
- `com.h2database:h2` como dependência de produção.

**Obsidian:**
- `obsidian-tests` (módulo vazio, scaffold morto).
- Entradas mortas em `libs.versions.toml` (`jackson`, `dotenv` cdimascio, `snakeyaml`, `reflections`, `slf4j`).
- Redundância interna `Maybe`/`Result`/`Try` sem fronteira de uso documentada além do javadoc pontual de `Result`.

---

## 5. Problemas de arquitetura, organização, nomenclatura ou API

1. **Escopo excessivo (JToolBox)**: `ioc/`, `http/`, `jdbc/`, `test/` tentam recriar Spring + JUnit inteiros dentro de uma "toolbox". Cada um desses domínios sozinho já é um projeto de anos de maturidade em outras libs. Isso é a maior fonte de risco de manutenibilidade do JToolBox.
2. **Fragmentação de configuração (JToolBox)**: três sistemas não interoperáveis para resolver "configuração externa" (`dotenv/`, `config/`, `ioc/environment/Environment`).
3. **Migração incompleta (JToolBox)**: `Condition` deprecated ainda usado internamente ao invés de `If`.
4. **Nomenclatura sem convenção documentada (JToolBox)**: sufixo "X" aplicado parcialmente; duas classes `TypeConverter`; duas classes `Yaml` colidindo por nome simples no mesmo pacote.
5. **Nome de pacote colidindo com JDK (Obsidian)**: `lang.reflect` em `obsidian-reflection` — problema real, já visível no próprio código-fonte do módulo.
6. **Módulo com nome que não reflete o conteúdo (Obsidian)**: `obsidian-configuration` mistura dotenv + file I/O + JSON, três domínios sem relação de dependência lógica entre si.
7. **Redundância conceitual interna (Obsidian)**: `Maybe` vs `Result` vs `Try` sem fronteira de uso clara para quem consome a lib de fora.
8. **Falta de coesão entre módulos irmãos (Obsidian)**: nenhum módulo consome outro (exceto o par benchmark→collections); `DotenvInjector` reimplementa reflection ao invés de reusar `obsidian-reflection`.
9. **PackageScanner que não funciona em produção (JToolBox)**: `ioc/context/PackageScanner.java` não suporta JAR — inviabiliza uso real do container de DI fora do ambiente de desenvolvimento.

---

## 6. Problemas de manutenibilidade e extensibilidade

- **JToolBox não compila no estado atual** (§1.11) — sinal de ausência de CI/build gate, risco alto de regressões silenciosas acumuladas.
- Validação de `config/core/EnvValidator.java` via cadeia de `instanceof` não é extensível sem editar a classe (fechado para extensão).
- Cobertura de teste extremamente baixa nas duas bibliotecas: JToolBox tem 2 arquivos de teste para ~166 arquivos de produção; Obsidian tem 9, concentrados em 1 de ~7 módulos. As áreas mais arriscadas do ponto de vista de corretude (estruturas HAMT do Obsidian, concorrência do `obsidian-promise`, o container de IoC do JToolBox) são justamente as **sem nenhum teste**.
- Dependências não utilizadas em ambos os projetos (ASM no JToolBox; catálogo morto no Obsidian) aumentam superfície de manutenção sem benefício.

---

## 7. Possíveis problemas de performance, concorrência ou segurança

- **Concorrência (JToolBox)**: estado estático mutável sem sincronização em `Logger`, `EnvEngine`, `BeanRegistry.typeIndex`; pool de threads sem limite em `Threader`.
- **Concorrência (Obsidian)**: `obsidian-promise` (cancelamento, propagação de estado) e `obsidian-collections` (estruturas usadas de forma concorrente, já que são imutáveis mas não foi verificado se a construção/mutação estrutural interna é segura sob leitura concorrente) não têm nenhum teste que comprove corretude sob concorrência real — risco latente, não confirmado.
- **Performance**: roteamento HTTP O(n) por request no `http/server/Server.java` do JToolBox; criação de nova instância `Gson` a cada chamada estática em `JsonX` (JToolBox); ausência de cache de lookup de `Method`/`Field` em `obsidian-reflection`.
- **Segurança**: nada crítico identificado nesta auditoria (não houve escopo para fuzzing/pentest), mas vale nota: `Serializer.saveToBinaryFile` do JToolBox grava bytes arbitrários "como string" (§1.12) — se não passar por Base64 corretamente, é um risco de corrupção de dados, não de segurança per se. `EnvValidator`/`@Decrypt` do JToolBox merece revisão cuidadosa antes de reaproveitar — não foi auditada em profundidade a implementação real de decriptação.

---

## 8. Funcionalidades generalizáveis e reaproveitáveis

1. `Dynamic` (JToolBox) — API fluente rica sobre coleção mutável adaptativa.
2. `If` (JToolBox) — DSL de controle de fluxo fluente, bem documentada.
3. `Try` (ambas) — fundir os pontos fortes de `util/Try.java` (JToolBox: `retryWithBackoff`, `sequence`, `traverse`, `combine`, `withTimeout`) com `functional/Try.java` (Obsidian: `mapTry`, `checkedGet()`, `recover` tipado), corrigindo o bug de `retry()` de instância do JToolBox.
4. `Jdbc` — builder imutável de URL/conexão (não o `JdbcTemplate` inteiro).
5. `RoutePattern` (JToolBox) — compilação de rota simples e correta.
6. Uso de virtual threads em `http/server/Server.java` (JToolBox) — referência de boa prática Java 21.
7. HAMT persistente (`Hashing`, `HashTriePMap`, `HashTrieOSet` — Obsidian) — base sólida para módulo de collections persistentes.
8. `obsidian-promise` inteiro — capacidade nova sem equivalente competitivo no JToolBox.
9. `obsidian.experimental.io.scan` — arquitetura de scanner testável, superior ao `console/` do JToolBox.
10. `Maybe`/`Result` sealed (Obsidian) — uso correto de `sealed` moderno.
11. Família `Box`/`AtomicBox`/`PlainBox`/`AtomicVolatileBox` (Obsidian) — três garantias de concorrência corretamente implementadas e documentadas.
12. Facade JSON com motor escondido (`obsidian.json.api.Json` sobre `internal.gson.*`) — bom padrão de encapsulamento a replicar.
13. Dotenv com injeção declarativa por anotação (melhor do Obsidian) combinado com profiles/reload (melhor do JToolBox `config/`).
14. `lang.reflect.Reflect` (Obsidian, renomeado) — API fluente sobre reflection, mas precisa de cache de lookup.

---

## 9. Funcionalidades que não deveriam ser levadas para a nova biblioteca

1. **`ioc/` inteiro** (JToolBox) — clone de Spring, escopo incompatível com uma lib de utilitários/abstrações.
2. **`http/server`** (JToolBox) — servidor web completo; se algo for reaproveitado, deve ser só o `ClientRequest`/`ClientResponse` (cliente HTTP) e talvez `RoutePattern` como utilitário isolado, não o servidor inteiro.
3. **`jdbc/dao/JdbcTemplate`** (JToolBox) — clone de Spring JDBC; no máximo o builder `Jdbc`/`URLBuilder` de conexão.
4. **`test/` inteiro** (JToolBox) — reimplementação de JUnit, redundante com JUnit real já usado pela própria lib.
5. **`config/examples/UsageExamples.java`** — código demo.
6. **Dependências ASM** — sem uso, não devem ser herdadas sem um caso de uso concreto.
7. **`obsidian-tests`** — módulo vazio, não replicar a estrutura sem conteúdo.
8. **Pacote `lang.reflect`** como nome — a ideia de `obsidian-reflection` é boa, o nome do pacote não deve ser herdado.
9. **Módulo `obsidian-configuration` como está** — a mistura de domínios não deve ser replicada; cada domínio (dotenv, file, json) deve virar módulo próprio.
10. **`ioc/environment/Environment.java`** (JToolBox) — terceiro sistema de config redundante.

---

## 10. O que poderia ser melhorado em relação às bibliotecas originais

- **Escopo disciplinado**: nenhuma das duas bibliotecas resistiu à tentação de crescer para além de "utilitários e abstrações" — JToolBox reimplementando frameworks inteiros, Obsidian acumulando três tipos de `Result`/`Maybe`/`Try` sem fronteira clara. A nova biblioteca precisa de critérios explícitos de "o que pertence" desde o início.
- **Testes como requisito, não afterthought**: as áreas mais arriscadas (estruturas de dados, concorrência) são exatamente as sem teste em ambas as libs.
- **Um único sistema por domínio**: eliminar a duplicação de "config" (3x no JToolBox) e de "outcome types" (3x no Obsidian) desde a concepção.
- **Nomenclatura de pacote que não colide com o JDK** e convenção de sufixo documentada (nada de "X" sem explicação).
- **Build limpo e verificado**: garantir que o projeto sempre compila e tem CI real, ao contrário do estado atual do JToolBox.
- **Modularização Gradle pela coesão real**, não por conveniência de agrupamento (o erro de `obsidian-configuration`).
- **Dependências mínimas e todas usadas**: eliminar catálogos de dependência com entradas mortas (ambas as libs têm esse problema).
