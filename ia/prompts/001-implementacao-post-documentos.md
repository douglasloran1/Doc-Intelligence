# 001 — implementação da fatia vertical: POST /documentos

**Sessão:** 2026-09-01
**Objetivo:** estrutura do projeto Spring Boot e o endpoint `POST /documentos` (recebimento, validação, idempotência), sem a fila/worker. Fechar o ADR 0006 antes de commitar o código.

> Primeira sessão registrada em `ia/prompts/`. As sessões anteriores desta mesma conversa — revisão da especificação pelo `critico-de-especificacao` (duas rodadas), ADRs 0002, 0003, 0005 e 0006, preenchimento de `restricoes.md`, correções dos achados — não têm arquivo próprio aqui ainda. Decisão pendente: registrá-las retroativamente (marcadas como reconstruídas) ou tratá-las como raciocínio do usuário formalizado pelo agente (CLAUDE.md, "Divisão de trabalho").

---

## Prompt 1

Vamos começar a implementação da fatia vertical, seguindo docs/01-especificacao.md e os ADRs 0002, 0003 e 0005. Primeira parte, por escopo reduzido para revisão: estrutura do projeto e o endpoint POST /documentos.

Preciso que você:

1. Crie a estrutura de um projeto Spring Boot em src/, com Maven (pom.xml). Dependências: Spring Web, Spring Data JPA, driver PostgreSQL, Lombok (se ajudar a reduzir boilerplate), Spring Boot Test.

2. Crie a entidade/tabela Documento, com os campos definidos na especificação §3: identificador (UUID), hash do arquivo original, tipo declarado, estado atual (enum com os valores do diagrama: recebido, em_processamento, falha_temporaria, falha_definitiva, pronto, aguardando_conferencia, em_conferencia, concluido, rejeitado), campos extraídos (usar uma coluna JSONB, conforme consequência registrada no ADR 0002), confiança do documento, nome padronizado proposto, versão do adaptador que gerou a extração, dados de reivindicação (quem e quando, para a conferência).

3. Crie também uma entidade separada para o histórico de transições (append-only, conforme §3: "cada transição é um evento gravado, não uma sobrescrita") — documento_id, estado anterior, estado novo, timestamp, motivo (quando aplicável, como na rejeição).

4. Implemente POST /documentos:
   - Recebe multipart (arquivo).
   - Valida formato (jpg, jpeg, png, pdf) e tamanho (máximo 15 MB) — 422 se fora disso, conforme §4 e restricoes.md (b).
   - Calcula hash SHA-256 do conteúdo do arquivo.
   - Se já existe um documento com esse hash, devolve o documento existente (idempotência, §4 e restricoes.md (c)) em vez de criar um novo.
   - Caso contrário, cria o documento em estado "recebido", grava a primeira transição no histórico, e devolve o id e o status.
   - Nesta primeira parte, não crie ainda o mecanismo de fila/worker — só a criação do documento em "recebido" já é suficiente para este passo.

5. Configure application.properties (ou .yml) para conexão com Postgres local, com valores de exemplo que serão documentados no README depois.

6. Escreva testes para: validação rejeitando formato/tamanho inválido, e idempotência (mesmo hash não cria segundo registro).

Me mostre a estrutura de arquivos criada e o conteúdo antes de qualquer commit — não commite ainda, s importa mostrar primeiro.

**Retorno em uma linha:** projeto Maven na raiz (Spring Boot 3.5.16, Java 21), pacote `documento/` (entidade `Documento` com `campos_extraidos` jsonb, enum `EstadoDocumento`, `TransicaoEstado` append-only com `@ManyToOne` → coluna `documento_id`), `DocumentoService` (validação + SHA-256 + dedup por hash + criação em RECEBIDO com a primeira transição), `DocumentoController` (`POST /documentos`, 201 novo / 200 reenvio), `TratadorDeErros` (422 com `{codigo, mensagem}`), `application.yml`, e 8 testes.
**Verifiquei:** rodei `mvn test` — compila e passa 8/8 (5 em `DocumentoServiceTest`, 3 em `DocumentoControllerTest`); o teste de idempotência prova que `documentoRepository.save` e `transicaoRepository.save` não são chamados quando o hash já existe; o slice `@WebMvcTest` devolve `422` com corpo `{codigo, mensagem}` e `201`/`estado: recebido` no caminho feliz; confirmei que Spring Boot 3.3.x está fora de suporte (aviso do language server) antes de subir para 3.5.16; confirmei que o ADR 0002 descarta H2, então não adicionei H2 aos testes.
**Ação:** aceito com desvios listados ao usuário: Spring Boot 3.5.16 no lugar de 3.3.x; testes com mock em vez de Testcontainers nesta parte (integração com Postgres real fica para a próxima, para não contrariar o ADR 0002); `ddl-auto: update` sem Flyway; `201`/`200`; corpo de erro `{codigo, mensagem}` (a §4 exige código de motivo); origem do formato pelo sufixo do nome do arquivo. Seis desvios aprovados como estão; o sétimo (origem do formato) virou o ajuste da §4 do Prompt 3.

### Pergunta de esclarecimento (agente → usuário)

Antes de implementar, o agente perguntou onde ficaria a raiz do projeto Maven — na raiz do repositório (layout idiomático), aninhado em `src/`, ou `pom.xml` em `src/` com testes na pasta `tests/`.
**Resposta do usuário:** raiz do repositório, layout Maven padrão (`pom.xml` na raiz, `src/main/java`, `src/test/java`). A pasta `tests/` fica sem uso, com `.gitkeep`.

---

## Prompt 2

Antes de commitar o código já implementado, preciso fechar o ADR 0006 (granularidade dos módulos), que estava "a criar" na tabela da §8.

Decisão: estrutura de pacote por funcionalidade (documento/ com entidade, repositório, serviço, controller juntos), não Clean Architecture / Arquitetura Hexagonal com camadas separadas em pacotes e portas explícitas.

Alternativas a descartar:
- Clean Architecture / Hexagonal (portas e adaptadores, entidade de domínio separada da entidade JPA): descartar por custo — cada entidade dobraria (uma pura de domínio, uma de persistência), exigiria interfaces para toda fronteira, e o ganho (testabilidade de domínio isolado de framework, substituição fácil de infraestrutura) não se paga numa fatia vertical de três dias com um only projeto e sem múltiplas equipes.
- Monolito sem nenhuma separação (tudo numa classe ou pacote único): descartar por já ter sido resolvido pelo módulo Domínio da especificação (§5) — a separação em Service/Controller/Repository já garante que a regra de negócio não fica misturada com HTTP nem com persistência, mesmo sem camadas em pacotes distintos.

Consequência a registrar: se o projeto crescesse para produção real, com múltiplos tipos de documento e mais desenvolvedores, a ausência de portas explícitas tornaria trocar o banco ou testar o domínio isolado de JPA mais caro. Gatilho de revisão: se o número de tipos de documento ou de desenvolvedores no projeto crescer significativamente.

Preciso que você:
- Crie docs/adr/0006-granularidade-dos-modulos.md, Status: aceita, seguindo o formato da skill adr.
- Atualize a tabela da §8 da especificação, tirando "(a criar)" da linha 0006 e linkando o arquivo.
- Atualize a tabela de docs/adr/README.md.
- Me mostre o resultado antes do commit.

**Retorno em uma linha:** `docs/adr/0006-granularidade-dos-modulos.md` criado (Status: aceita) no formato da skill `adr`, com as duas alternativas descartadas e o gatilho de revisão dados pelo usuário; tabelas da §8 e do `README.md` dos ADR atualizadas.
**Verifiquei:** conferi que a decisão do ADR corresponde ao código já escrito no Prompt 1 (pacote `documento/` com tudo junto, separação por estereótipo Spring, entidade JPA = entidade de domínio, testes com repositório mockado); reli as duas tabelas de índice para checar que ficaram consistentes entre si e com os arquivos existentes.
**Ação:** aceito; commitado como `ff5cd03`.

---

## Prompt 3

Revisei os sete pontos. Seis estão aprovados como estão. Um ajuste antes do commit:

Ponto 4 (origem do formato via sufixo do arquivo): concordo com a decisão, mas isso resolve o achado N6 do crítico, que estava em aberto — preciso que isso fique registrado, não solto. Adicione uma frase na seção 4 da especificação (perto de onde já se fala de formato aceito) dizendo que a extensão vem do sufixo do nome do arquivo enviado no multipart, e mova N6 de "Em aberto" para "Resolvido" no bloco Registro de crítica, explicando a decisão.

Agora, o registro de IA para esta sessão de trabalho (implementação do POST /documentos): rode a skill registro-de-ia e crie o arquivo correspondente em ia/prompts/, cobrindo os pedidos que fiz nesta sessão — a instrução completa para a estrutura Spring Boot e o endpoint, a pergunta sobre Clean Architecture, e a instrução do ADR 0006. Registre também em ia/onde-errou.md, se aplicável, algo que valha a pena: por exemplo, se o agente tomou alguma decisão de implementação sem me perguntar quando deveria ter perguntado (isso vale conferir com honestidade, não inventar problema).

Depois disso, proponha o commit do código (pom.xml, src/, .gitignore) junto com o ajuste da spec e o registro de IA. Me mostre tudo antes de qualquer commit.

**Retorno em uma linha:** frase adicionada à §4 (formato vem do sufixo do nome no multipart, sem `Content-Type` nem inspeção de conteúdo); N6 movido de "Em aberto" para nova subseção "Resolvido na implementação" no Registro de crítica; skill `registro-de-ia` rodada; este arquivo e a entrada em `ia/onde-errou.md` criados.
**Verifiquei:** reli a §4 e o bloco Registro de crítica — N6 não aparece mais em "Em aberto", a referência no Achado 2 aponta para a nova subseção; conferi que a frase da §4 descreve o que o código faz de fato (`DocumentoService.extensao(...)` lê o sufixo de `MultipartFile.getOriginalFilename()`); a observação "a pergunta sobre Clean Architecture" citada pelo usuário corresponde ao Prompt 2 (a pergunta de esclarecimento que o agente fez foi sobre o layout Maven, não sobre Clean Architecture — as duas coisas estão registradas acima).
**Ação:** aceito (pendente commit).

---

## Fechamento da sessão

**Entregue:** `pom.xml` (Spring Boot 3.5.16, Java 21); pacote `br.com.docintelligence.documento` completo para o `POST /documentos`; `application.yml` com Postgres local por variável de ambiente; 8 testes verdes; ajuste da §4 + Registro de crítica da especificação; ADR 0006; seção Java/Maven no `.gitignore`.

**Não feito nesta sessão, escrito como não feito:** mecanismo de fila/worker; teste de integração com Postgres real via Testcontainers (só mock nesta parte); migração versionada de schema (Flyway); README com instrução de subir o banco. O `tests/` na raiz do repositório ficou sem uso (os testes seguem a convenção Maven em `src/test/java`).

**Pendente ao fim da sessão:** commit do código + ajuste da spec + este registro (mostrado ao usuário antes de commitar).
