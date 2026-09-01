# DOC Intelligence

Serviço de inteligência documental: recebe imagem ou PDF, classifica o tipo, extrai os campos daquele tipo, propõe um nome padronizado e encaminha para conferência humana quando a confiança é baixa. Consumido por sistemas internos, não por navegador aberto.

Entrega para o processo de seleção — trilha **A · back-end**.

Este README cobre só como rodar. A descrição completa do sistema — problema, recorte, contrato, decisões — está em [`docs/01-especificacao.md`](docs/01-especificacao.md), escrita antes do código.

## Onde está cada coisa

| O que | Onde |
|---|---|
| Especificação, escrita antes do código | [`docs/01-especificacao.md`](docs/01-especificacao.md) |
| Restrições do ambiente e como cada uma foi tratada | [`docs/restricoes.md`](docs/restricoes.md) |
| Decisões de arquitetura, com alternativas descartadas | [`docs/adr/`](docs/adr/) |
| Onde a implementação divergiu da especificação | [`docs/divergencias.md`](docs/divergencias.md) |
| Instrução do agente | [`CLAUDE.md`](CLAUDE.md) |
| Skills e subagentes configurados | [`.claude/`](.claude/) |
| Prompts, na íntegra e em ordem | [`ia/prompts/`](ia/prompts/) |
| Onde o agente errou e o que foi feito | [`ia/onde-errou.md`](ia/onde-errou.md) |
| Carta de fechamento | [`carta/`](carta/) |

## Pré-requisitos

- **Java 21** (o projeto usa records e text blocks; não compila em versões anteriores)
- **Maven 3.9+**
- **PostgreSQL 16** — via Docker, abaixo. Qualquer PostgreSQL 12+ serve (`jsonb` e `FOR UPDATE SKIP LOCKED`); 16 é o que foi usado.

## Subir o banco

**Opção A — Docker Compose (recomendada).** Sobe um PostgreSQL 16 com as credenciais já batendo com os defaults da aplicação:

```bash
docker compose up -d
```

**Opção B — `docker run`**, sem o arquivo compose:

```bash
docker run -d --name doc-intelligence-db \
  -e POSTGRES_DB=doc_intelligence \
  -e POSTGRES_USER=doc_intelligence \
  -e POSTGRES_PASSWORD=doc_intelligence \
  -p 5432:5432 postgres:16
```

**Sem Docker.** Crie um banco `doc_intelligence` e um usuário `doc_intelligence` (senha `doc_intelligence`), ou aponte as variáveis abaixo para o que você tiver.

O schema é criado na primeira execução pelo Hibernate (`ddl-auto: update`). Migração versionada (Flyway) ficou para depois — ver [`docs/adr/0002-persistencia.md`](docs/adr/0002-persistencia.md).

## Configuração

A aplicação lê estas variáveis de ambiente; os defaults (em `src/main/resources/application.yml`) já servem para desenvolvimento local com o Compose, então **não é preciso setar nada** para rodar.

| Variável | Default | Para quê |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/doc_intelligence` | conexão JDBC com o Postgres |
| `DB_USERNAME` | `doc_intelligence` | usuário do banco |
| `DB_PASSWORD` | `doc_intelligence` | senha do banco |
| `WORKER_ID` | `worker-local` | identifica o worker nas reivindicações de job (relevante só com mais de uma instância) |

Outros parâmetros (intervalo de polling, duração dos leases, timeout do adaptador) estão em `application.yml` com comentários; sobrescreva na linha de comando se precisar, por exemplo `--worker.polling-ms=500`.

## Rodar

```bash
mvn spring-boot:run
```

Sobe em `http://localhost:8080`. O worker de processamento começa sozinho e consome a fila a cada 2 segundos.

## Testes

```bash
mvn test
```

**Coberto:** testes de unidade, com repositório mockado — validação de entrada e idempotência (`DocumentoServiceTest`), contrato HTTP e códigos de erro (`DocumentoControllerTest`), o dublê de extração por tamanho de arquivo (`DubleExtracaoTest`), o cálculo de confiança e a decisão de estado (`RegrasDocumentoTest`), o processamento de um job com retry até `falha_definitiva` (`ProcessadorDocumentoTest`), e a fila de conferência — reivindicação, disputa 409, correção, rejeição, expiração (`ConferenciaServiceTest`).

**Não coberto, adiado conscientemente:** teste de integração contra um PostgreSQL real (Testcontainers). Fica sem exercício a query `FOR UPDATE SKIP LOCKED` da fila, o reaproveitamento de job com lease expirado, e a JPQL de listagem com filtro nulo — hoje só validados via mock. É o ponto mais frágil da entrega por não ter rodado contra um banco de verdade. O motivo do adiamento (e por que não H2) está em [`docs/adr/0002-persistencia.md`](docs/adr/0002-persistencia.md); o registro do risco, em [`ia/prompts/002-fila-de-processamento-e-worker.md`](ia/prompts/002-fila-de-processamento-e-worker.md).

## Fluxo, de ponta a ponta

```
recebido ──(worker)──► em_processamento ──┬── confiança ≥ 0,85 ──► pronto                  [terminal]
                                          │
                                          ├── confiança < 0,85 ──► aguardando_conferência
                                          │
                                          ├── falha temporária ──► (retry: 5s, 10s, 20s)
                                          │
                                          └── 3 falhas ──────────► falha_definitiva         [terminal]

aguardando_conferência ──(POST /reivindicar)──► em_conferência ──┬── PATCH ────────────► concluído   [terminal]
                                                                 │
                                                                 ├── POST /rejeitar ───► rejeitado   [terminal]
                                                                 │
                                                                 └── lease expira ─────► aguardando_conferência
```

### Exemplos via `curl`

Gere dois arquivos de exemplo — não precisam ser imagens de verdade, o dublê desta entrega só olha o tamanho (não decodifica o conteúdo):

```bash
# bash
head -c 100000 /dev/zero > exemplo-pequeno.jpg   # < 500 KB → vai para conferência
head -c 600000 /dev/zero > exemplo-grande.jpg    # ≥ 500 KB → vai direto para pronto
```

```powershell
# PowerShell
[IO.File]::WriteAllBytes("exemplo-pequeno.jpg", [byte[]]::new(100000))
[IO.File]::WriteAllBytes("exemplo-grande.jpg",  [byte[]]::new(600000))
```

**1. Recebe o documento.** Responde na hora com `id` e `status: recebido`.

```bash
curl -s -X POST http://localhost:8080/documentos -F "arquivo=@exemplo-pequeno.jpg"
```

**2. Aguarda o processamento.** O worker pega o job em até ~2s e move o documento para `pronto` ou `aguardando_conferencia`.

**3. Consulta um documento** — estado, campos extraídos, confiança, nome padronizado e o histórico de transições. `404` se não existir.

```bash
curl -s http://localhost:8080/documentos/<id>
```

**4. Lista os documentos**, com filtro opcional por estado e tipo, paginado (página padrão de 20):

```bash
curl -s "http://localhost:8080/documentos?status=aguardando_conferencia&pagina=0&tamanho=20"
```

**5. Reivindica para conferência.** O identificador do operador vai no corpo. `409` se já houver reivindicação ativa de outro operador; só documentos em `aguardando_conferencia` podem ser reivindicados.

```bash
curl -s -X POST http://localhost:8080/documentos/<id>/reivindicar \
  -H "Content-Type: application/json" \
  -d '{"operador": "op-1"}'
```

**6a. Corrige e conclui.** Exige a reivindicação ativa do mesmo operador; move para `concluido`.

```bash
curl -s -X PATCH http://localhost:8080/documentos/<id> \
  -H "Content-Type: application/json" \
  -d '{"operador": "op-1", "campos": {"cpf": "000.000.000-00", "nome_completo": "FULANO DE TAL"}}'
```

**6b. Ou rejeita.** Exige o mesmo operador e um `motivo` (`422` se faltar); move para `rejeitado`.

```bash
curl -s -X POST http://localhost:8080/documentos/<id>/rejeitar \
  -H "Content-Type: application/json" \
  -d '{"operador": "op-1", "motivo": "arquivo ilegivel"}'
```

## O que está pronto e o que não está

O produto-alvo tem cinco comportamentos (seção 2 da especificação). Esta entrega **não é o produto-alvo** — é o projeto do sistema mais uma fatia vertical.

**Implementado** — um caminho completo de ponta a ponta, estreito, para um único tipo de documento (`identidade`): recebimento com validação e idempotência por hash → fila de jobs no PostgreSQL → worker que consome com lease e chama o adaptador → extração (um **dublê determinístico** no lugar do fornecedor real, ADR 0005) → cálculo de confiança e decisão entre `pronto` e conferência → consulta e listagem → fila de conferência humana com reivindicação, correção, rejeição e expiração.

**Fora desta entrega, por escolha** (detalhe na seção 2 da especificação e em [`docs/restricoes.md`](docs/restricoes.md)): classificação automática entre os sete tipos de documento; o fornecedor de IA real; autenticação de serviço; interface gráfica; deploy e infraestrutura; migração versionada de schema; retenção e expurgo de dado sensível; teto de custo por janela. O que não foi feito está escrito como não feito — na especificação (seção 9 e o bloco "Registro de crítica"), em `docs/restricoes.md`, e na seção de testes acima.

## Sobre a configuração de IA

A pasta `.claude/` contém quatro skills e dois subagentes escritos para este projeto. O princípio que os organiza: o raciocínio é do autor, o agente formaliza, critica e verifica. O crítico faz perguntas em vez de propor soluções; a skill de ADR pergunta pelas alternativas em vez de inventá-las; o auditor confere sem corrigir.

O mecanismo é genérico e a instância é deste projeto — a ligação entre as duas camadas é `docs/restricoes.md`, lido pelas skills e pelos subagentes sem que eles precisem ser editados.

## Dados

Nenhum dado real de cliente, de pessoa física ou do escritório existe neste repositório. Os documentos usados em teste são fictícios e foram gerados para isso.
