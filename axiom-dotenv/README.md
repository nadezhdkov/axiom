# axiom-dotenv

**Parsing de `.env` + injeção declarativa por anotação (`@Env`, `@EnvPrefix`,
`@Default`, `@RequiredEnv`), com interpolação `${OUTRA_CHAVE:default}` via
`axiom-placeholder`.**

<p align="left">
  <img src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=flat-square" alt="Status"/>
  <img src="https://img.shields.io/badge/depende%20de-axiom--core%2C%20axiom--reflect%2C%20axiom--placeholder-blue?style=flat-square" alt="Dependências"/>
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

Consolida em um único módulo o que, na auditoria original, existia como
três sistemas de configuração não coordenados (`dotenv/`, `config/`,
`ioc/environment/Environment`, no JToolBox). `Dotenv.configure().load()`
faz o parsing; `DotenvBinder.bind(objeto, dotenv)` injeta valores em campos
anotados com `@Env`, convertendo para o tipo do campo (`String`, `int`,
`long`, `double`, `boolean`, `Duration`, `Path`, `File`, `enum`, `List`,
`Set`). Valores podem referenciar outras chaves (`URL=jdbc://${HOST}:${PORT}`),
resolvido pelo `axiom-placeholder` — inclusive detecção de referência
circular, herdada do módulo.

## O que não resolve (ainda)

Não há suporte a valores multi-linha entre aspas (cada entrada precisa caber em uma linha).

---

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-dotenv:<versão>")
}
```

**Requisitos**: JDK 21+. Depende de `axiom-core`, `axiom-reflect` e
`axiom-placeholder` — nenhuma dependência de terceiros.

---

## Exemplo Rápido

```java
import io.axiom.dotenv.*;

@EnvPrefix("APP_")
class AppConfig {
    @Env("HOST") @Default("localhost")
    String host;

    @Env("PORT") @Default("8080")
    int port;
}

Dotenv dotenv = Dotenv.configure().directory(".").ignoreIfMissing().load();

AppConfig config = new AppConfig();
DotenvBinder.bind(config, dotenv);

// Profiles: only binds if dotenv.activeProfile() is "dev" or "test"
@Profile({"dev", "test"})
class DevOnlyConfig {
    @Env("DEBUG") @Default("false")
    boolean debug;
}
Dotenv devDotenv = Dotenv.configure().directory(".").profile("dev").load(); // also merges .env.dev
DotenvBinder.bind(new DevOnlyConfig(), devDotenv);

// Reload: only allowed for classes explicitly opted in via @Reloadable
@Reloadable
class LiveConfig {
    @Env("FEATURE_FLAG") @Default("false")
    boolean featureFlag;
}
LiveConfig live = new LiveConfig();
Dotenv reloadable = Dotenv.configure().directory(".").reloadable().load();
DotenvBinder.bind(live, reloadable);
// ...later, after the .env file changed on disk...
reloadable = DotenvBinder.reload(live, reloadable);
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/dotenv/examples/DotenvExamples.java`](examples/src/main/java/io/axiom/dotenv/examples/DotenvExamples.java).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `Dotenv.configure()` | Ponto de entrada do `DotenvBuilder` |
| `DotenvBuilder#directory`/`#filename`/`#ignoreIfMissing`/`#throwIfMissing`/`#strict`/`#withoutInterpolation` | Configuração do parsing |
| `Dotenv#get(key)` / `#get(key, default)` | Acesso a um valor resolvido (env real do sistema tem precedência sobre o arquivo) |
| `@Env("KEY")` | Marca um campo para injeção |
| `@EnvPrefix("PREFIX_")` | Prefixo de classe aplicado a todo `@Env` |
| `@Default("valor")` | Valor padrão se a chave não existir |
| `@RequiredEnv` | Falha explícita se não houver valor nem default |
| `@EnvIgnore` | Ignora um campo mesmo se anotado com `@Env` |
| `DotenvBinder.bind(objeto, dotenv)` | Executa a injeção |
| `DotenvBuilder#profile(String)` | Mescla `<filename>.<profile>` sobre o arquivo base; expõe `Dotenv#activeProfile()` |
| `@Profile({"dev", "test"})` | Restringe `bind` à(s) profile(s) listada(s); sem match, `bind` não toca os campos |
| `DotenvBuilder#reloadable()` | `Dotenv#reload()` volta a ler o(s) arquivo(s) do disco em vez de lançar |
| `@Reloadable` + `DotenvBinder.reload(objeto, dotenv)` | Re-lê e reinjeta; exige a classe explicitamente anotada |

---

## Notas de Design

- **Nenhum estado estático global.** `Dotenv` é uma instância explícita
  criada por `DotenvBuilder#load()` — corrige o padrão `EnvEngine` estático
  mutável do JToolBox (causa raiz de bugs de concorrência identificados na
  auditoria).
- **Injeção usa `axiom-reflect` internamente**, não
  `java.lang.reflect.Field` cru — corrige a integração perdida identificada
  na auditoria original (`DotenvInjector` reimplementava reflection em vez
  de reusar o próprio módulo de reflection da mesma biblioteca).
- **Interpolação usa `axiom-placeholder` internamente**, incluindo detecção
  de referência circular herdada de lá — um valor `A=${B}` / `B=${A}` falha
  com `CircularPlaceholderReferenceException` em vez de estourar a stack.
- Variáveis de ambiente reais (`System.getenv()`) sempre têm precedência
  sobre o arquivo `.env` — mesma convenção observada no JToolBox.
- **`@Profile`/`@Reloadable` sem estado estático**, ao contrário do `config/` original do
  JToolBox: o "engine" recarregável (`ReloadableDotenv`, obtido via `DotenvBuilder#reloadable()`)
  é uma instância explícita que o usuário cria e mantém — não um singleton estático mutável tipo
  `EnvEngine`. `DotenvBinder.reload()` exige `@Reloadable` na classe alvo para nunca reinjetar um
  objeto vivo por acidente; `@Profile` sem `Dotenv#activeProfile()` presente faz `bind` não tocar
  os campos, em vez de assumir um profile default implícito.

---

## Testes

```bash
./gradlew :axiom-dotenv:test
```

Cobertura: casos de borda de parsing (aspas simples/duplas, comentário
inline, `export`, linha malformada em modo estrito), injeção com sucesso/
falha/prefixo/default/required/ignore, propagação da exceção de
referência circular do `axiom-placeholder` através do `DotenvBuilder`,
mesclagem de arquivo por profile (com e sem arquivo de profile presente),
`reload()` lançando em instância não-recarregável vs. refletindo mudança de
arquivo em uma recarregável, e `@Profile`/`@Reloadable` aplicados/pulados
conforme o profile ativo e a presença da anotação na classe alvo.

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-dotenv).
