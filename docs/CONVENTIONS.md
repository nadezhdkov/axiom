# Convenções e checklist de PR

Complementa [`architecture.md`](./architecture.md) (§4, §8, §9) com uma lista
de verificação objetiva a aplicar em cada PR. Deliverable da Etapa 0 do
roadmap (`architecture.md §14`).

## Nomenclatura

- [ ] Pacote raiz do módulo é `io.axiom.<modulo>[.subpacote]`, minúsculo, sem
      abreviação obscura.
- [ ] Nenhum pacote colide com `java.*`/`javax.*` (checar antes de nomear —
      erro cometido pelo `lang.reflect` do Obsidian).
- [ ] Nenhum sufixo/prefixo decorativo sem significado documentado uma única
      vez no README do módulo (erro do `JsonX`/`AssertX` inconsistente do
      JToolBox).
- [ ] Um nome, um conceito: nenhuma classe com o mesmo nome simples resolve o
      mesmo problema em pacotes diferentes (erro do `TypeConverter`
      duplicado em `util/` e `config/core/` do JToolBox).
- [ ] API pública em `io.axiom.<modulo>` (ou `.api`, se necessário);
      implementação interna sempre em `internal.*`, não exportado no
      `module-info.java`.

## Dependências

- [ ] Toda dependência nova em `gradle/libs.versions.toml` tem pelo menos um
      `import` real correspondente em `src/main` do módulo que a declara —
      verificado automaticamente por `scripts/audit-deps.sh` (rodado em CI).
- [ ] Nenhum motor de terceiros (Gson, SnakeYAML, etc.) é exposto no tipo de
      retorno de um método público — sempre escondido atrás do modelo de
      dados próprio do módulo, em um subpacote `internal.*`.
- [ ] Módulo de capacidade (collections, concurrent, json, ...) só depende de
      outro módulo de capacidade se essa dependência estiver explicitamente
      declarada no DAG de `architecture.md §3`.

## Hierarquia de exceções (tensão em aberto, registrada na Etapa 6)

`AxiomException` (`axiom-core`) documenta a expectativa de que módulos criem sua exceção raiz
como subclasse dela. Na prática, hoje isso só é verdade para `axiom-placeholder`
(`PlaceholderException`) e `axiom-dotenv` (`DotenvException`) — `axiom-json`/`axiom-yaml`
(`JsonException`/`YamlException`, `sealed`), `axiom-io` (`FileOperationException`, `sealed`),
`axiom-reflect` (`ReflectException`), `axiom-concurrent` (`PromiseException`) e `axiom-console`
(`ParseFailureException`/`ValidationException`) estendem `RuntimeException` diretamente.

Isso não é um descuido de um módulo isolado — é uma tensão real entre dois princípios já
registrados neste mesmo documento: "um root de exceção por módulo, subclasse de
`AxiomException`" vs. "nenhuma dependência sem uso confirmado" (`axiom-reflect`/`axiom-io`
deliberadamente não dependem de `axiom-core`; ver `axiom-collections`/`axiom-io` como precedente
já registrado no `CHANGELOG.md`). Adicionar `axiom-core` só para herdar `AxiomException`
reintroduziria acoplamento nesses módulos hoje independentes. **Decisão não tomada
unilateralmente aqui** — precisa de uma escolha explícita do mantenedor:
(a) aceitar que `AxiomException` é opcional, não universal, e ajustar o javadoc de
`AxiomException` para não prometer isso; ou (b) tratar `axiom-core` como dependência sempre
aceitável (é, afinal, a raiz do DAG) e migrar as exceções raiz de cada módulo para estendê-la.

## Testes

- [ ] Caminho de sucesso + caminho de falha + composição encadeada cobertos
      antes de considerar um tipo "pronto".
- [ ] Toda correção de bug herdada de JToolBox/Obsidian (ex.: `Try.retry`)
      tem um teste de regressão específico, não só cobertura geral.
- [ ] `./gradlew build` (compila + testa + compila `examples/`) precisa estar
      verde antes de merge — reforçado pelo `.github/workflows/ci.yml`.

## Documentação

- [ ] Todo módulo tem `package-info.java` documentando propósito e fronteiras
      de uso.
- [ ] Todo módulo tem `README.md` próprio seguindo o template abaixo.
- [ ] Todo módulo tem um exemplo em `examples/src/main/java/...` compilado
      por CI, sincronizado com o snippet do README.

### Template — README por módulo

Copiar para `<modulo>/README.md` e preencher. Regra de ouro: um README por
módulo Gradle, nunca fragmentos soltos dentro de `src/main/java` (erro
identificado no JToolBox). Para um exemplo real já preenchido com esse
template, ver [`../axiom-core/README.md`](../axiom-core/README.md).

```markdown
# axiom-{{modulo}}

**{{uma linha descrevendo o propósito do módulo}}**

<p align="left">
  <img src="https://img.shields.io/badge/status-{{planejado|em%20desenvolvimento|estável}}-{{lightgrey|yellow|brightgreen}}?style=flat-square" alt="Status"/>
  <img src="https://img.shields.io/badge/depende%20de-{{axiom--core}}-blue?style=flat-square" alt="Dependências"/>
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

{{ex.: Try<T> unifica captura de exceção + composição funcional para APIs
Java que lançam Throwable, evitando try/catch aninhado.}}

## O que não resolve

{{deixar explícito o limite — evita o problema de escopo confuso
identificado na auditoria original.}}

---

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-{{modulo}}:<versão>")
}
```

**Requisitos**: JDK 21+{{, + outras dependências deste módulo específico}}

---

## Exemplo Rápido

```java
{{trecho mínimo, copiável, que roda sozinho}}
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/{{modulo}}/examples/`](examples/src/main/java/io/axiom/{{modulo}}/examples/).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `{{Tipo.metodo(...)}}` | {{descrição curta}} |

---

## Quando usar (e quando não usar)

{{Especialmente relevante para módulos com sobreposição conceitual, ex.:
Try vs Result vs Maybe em axiom-core. Documentar a fronteira aqui evita o
problema que a auditoria achou no Obsidian — três tipos de "outcome"
convivendo sem fronteira documentada.}}

---

## Notas de Design

{{Complexidade algorítmica relevante, decisões de concorrência, limitações
conhecidas e documentadas honestamente (ex.: "não é uma RRB-tree completa,
inserção no meio é O(n)"). Detalhe técnico completo fica no Javadoc do
pacote (package-info.java) — aqui é o resumo para quem está decidindo se
usa o módulo.}}

---

## Testes

```bash
./gradlew :axiom-{{modulo}}:test
```

{{Se aplicável: tipo de teste específico do módulo.}}

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-{{modulo}}).
```
