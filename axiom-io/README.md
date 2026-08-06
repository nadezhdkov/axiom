# axiom-io

**Operações de arquivo: `FileHandle` (API fluente), `Directory`, atributos, hashing
(MD5/SHA-256), compressão GZIP e busca em texto — porte de `io.obsidian.file.*`, o único
subsistema de configuração com testes reais em ambas as bibliotecas auditadas.**

<p align="left">
  <img src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=flat-square" alt="Status"/>
  <img src="https://img.shields.io/badge/depende%20de-nenhum%20m%C3%B3dulo%20axiom-blue?style=flat-square" alt="Dependências"/>
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

`FileHandle` é um núcleo de delegação fluente sobre `Path`: leitura/escrita, atributos,
permissões POSIX, operações de ciclo de vida (criar/copiar/mover/renomear/backup/links),
compressão GZIP e busca em texto (`filter`/`grep`/`replaceAll`/`count`), todos com componentes
delegados inicializados de forma preguiçosa. `Directory` cobre criação recursiva, listagem e
remoção (`deleteRecursively`/`clean`). Hashing usa Strategy (`HashAlgorithm` sealed:
`Md5Hash`/`Sha256Hash`) mais um atalho por nome de algoritmo. Hierarquia de exceções sealed
(`FileOperationException` → `FileReadException`/`FileWriteException`/`FileNotFoundException`/
`FileCompressionException`/`FileHashException`), cada uma carregando o `Path` afetado.

## O que não resolve (ainda)

- **`FileWatcher`** (wrapper de `WatchService`, do JToolBox) não foi incorporado nesta fatia —
  `axiom.md` o lista como candidato apenas "se não houver equivalente no Obsidian"; avaliar
  separadamente.
- **Testes de `FileCompressor` e `FilePermissions` isolados**: cobertos apenas indiretamente
  (compressão via `FileHandleTest`); mesma lacuna já existia no `obsidian-configuration`
  original — não fechada nesta etapa.

## Instalação

```kotlin
dependencies {
    implementation("io.axiom:axiom-io:<versão>")
}
```

**Requisitos**: JDK 21+. Zero dependências de terceiros e zero dependência de outro módulo
Axiom — só usa `java.nio.file.*`/`java.security.*`/`java.util.zip.*`.

---

## Exemplo Rápido

```java
import io.axiom.io.*;
import io.axiom.io.hash.Sha256Hash;

FileHandle handle = FileHandle.at("data.txt")
        .createIfNotExists()
        .write("hello")
        .append(" world");

String content = handle.readAllText();
String hash = handle.hash(new Sha256Hash());
FileMetadata metadata = handle.metadata();
```

> Exemplo completo e compilado por CI em
> [`examples/src/main/java/io/axiom/io/examples/IoExamples.java`](examples/src/main/java/io/axiom/io/examples/IoExamples.java).

---

## API Principal

| Tipo / Método | Descrição |
|---|---|
| `FileHandle.at(path)` | Núcleo de delegação fluente |
| `Directory.at(path)` | Criação/listagem/remoção recursiva de diretórios |
| `File` | Fachada estática para chamadas avulsas |
| `FileMetadata.of(path)` | Snapshot atômico de atributos (uma única leitura de `BasicFileAttributes`) |
| `HashAlgorithm`/`Md5Hash`/`Sha256Hash`/`FileHasher` | Hashing via Strategy |
| `FileOperationException` (sealed) | Base de todas as exceções, sempre com o `Path` afetado |

---

## Notas de Design

- **`FileMetadata` é atômica**: uma única chamada a `Files.readAttributes(path, BasicFileAttributes.class)`
  evita tanto condição de corrida TOCTOU quanto I/O redundante ao consultar múltiplos atributos
  em sequência — padrão herdado do `io.obsidian.file.attribute.FileMetadata` original.
- **Hierarquia de exceções especializada por operação**, com o `Path` alvo sempre acessível via
  `getTargetPath()` — reconhecida na auditoria como um bom padrão de ambas as bibliotecas,
  preservada aqui.
- **Sem dependência de nenhum outro módulo Axiom**: `axiom-io` não usa `Try`/`Result`/`Maybe` de
  `axiom-core` internamente, então não declara essa dependência — disciplina de "nenhuma
  dependência sem uso confirmado" aplicada mesmo entre módulos do próprio projeto.

---

## Testes

```bash
./gradlew :axiom-io:test
```

Cobertura: cadeias fluentes ponta a ponta em `FileHandle` (criar→escrever→ler,
escrever→anexar→ler linhas, escrever linhas→filtrar, comprimir→descomprimir round-trip),
snapshot atômico de metadados, hashing via Strategy vs. por nome de algoritmo (mesmo resultado),
`Directory` (criação idempotente e profunda, `isEmpty`, listagem, `deleteRecursively` seguro em
diretório ausente, `clean` mantendo o diretório), hierarquia de exceções (path preservado,
mensagem com algoritmo, captura pelo tipo base sealed).

---

## Changelog

Mudanças específicas deste módulo em [`CHANGELOG.md`](../CHANGELOG.md#axiom-io).
