# 002 — fila de processamento e worker

**Sessão:** 2026-09-01
**Objetivo:** implementar a fila de jobs (ADR 0003) e o worker que consome, chama o dublê de extração (ADR 0005), aplica as regras de domínio e move o documento até `pronto` / `aguardando_conferencia` / `falha_definitiva`.

---

## Prompt 1

Próxima parte da fatia vertical: fila de processamento e worker, seguindo o ADR 0003 e a especificação §6.

Preciso que você:

1. Crie a tabela/entidade de job de processamento, conforme ADR 0003: referência ao documento, estado do job (pendente, em_execucao, concluido, falhou), quem reivindicou (worker) e quando expira a reivindicação (lease), contador de tentativas.

2. Quando um documento é criado em POST /documentos (RECEBIDO), crie também o job correspondente, pendente.

3. Implemente o worker: um método agendado (@Scheduled, conforme conversamos) que busca jobs pendentes ou com lease expirado usando FOR UPDATE SKIP LOCKED (conforme ADR 0003), reivindica um, e processa:
   a. Move o documento para em_processamento (com transição registrada).
   b. Chama o Adaptador de extração — que nesta entrega é o dublê determinístico do ADR 0005 (regra de tamanho de arquivo: <500KB → confiança 0,60 nos obrigatórios; ≥500KB → 0,95; não-obrigatórios sempre 0,90).
   c. Passa o resultado para o módulo Domínio, que calcula a confiança do documento (mínimo dos campos obrigatórios, conforme ADR 0005) e decide o próximo estado: pronto (≥0,85) ou aguardando_conferência (<0,85).
   d. Grava a transição, atualiza o job para concluído.
   e. Para o tipo "identidade", o dublê deve devolver valores fictícios (não reais) para os campos: nome completo, CPF, data de nascimento (obrigatórios), órgão emissor, data de emissão (não-obrigatórios) — pode ser texto fixo tipo "PENDENTE DE EXTRAÇÃO REAL" ou similar, já que é um dublê.

4. Trate falha na chamada ao adaptador (simule uma falha ocasional no dublê, se quiser, ou deixe como ponto de extensão): timeout de 45s, retry com backoff 5s/10s/20s, até 3 tentativas (conforme restricoes.md (a) e §6), depois falha_definitiva.

5. Gere o nome padronizado do documento (identidade_{id}.{extensao}, conforme especificação §3) quando o documento chega em pronto ou aguardando_conferência — a proposta de nome já pode existir antes da conferência humana revisar.

6. Escreva testes: dublê determinístico por tamanho (dois arquivos de tamanhos diferentes levam a estados diferentes), transição correta de estados, e o job sendo marcado como concluído.

Mesma regra de sempre: me mostre a estrutura e o código antes de qualquer commit, sem commitar ainda.

**Retorno em uma linha:** entidade `JobProcessamento` + `EstadoJob`; job PENDENTE criado na mesma transação do documento em `DocumentoService`; `AdaptadorExtracao` (interface) + `DubleExtracao` (regra de tamanho, valores "PENDENTE DE EXTRAÇÃO REAL"); `RegrasDocumento` como módulo Domínio (mínimo dos obrigatórios, limiar 0,85, nome padronizado); `FilaProcessamento` (reivindica com `FOR UPDATE SKIP LOCKED`, lease) e `ProcessadorDocumento` (processa, retry backoff 5s/10s/20s até 3, depois `FALHA_DEFINITIVA`) em transações separadas, orquestradas por `WorkerProcessamento` (`@Scheduled`); `Documento` ganhou `tamanhoBytes` e `extensaoOriginal`; `@EnableScheduling`; bloco `worker` no `application.yml`; 5 testes novos.
**Verifiquei:** rodei `mvn test` — 23/23 verdes; `ProcessadorDocumentoTest` prova que 600 KB → `PRONTO`/confiança 0,95 e 100 KB → `AGUARDANDO_CONFERENCIA`/0,60, com o job em `CONCLUIDO` e as transições na ordem certa; o caminho de falha (adaptador stub que lança `FalhaExtracaoException`) leva a `tentativas` 1→2→3, backoff crescente, e `FALHA_DEFINITIVA` + job `FALHOU` na 3ª, com `motivo` propagado; `DubleExtracaoTest` cobre a fronteira exata de 500 KB (511.999 → 0,60; 512.000 → 0,95); conferi que a query nativa usa `FOR UPDATE SKIP LOCKED` como o ADR 0003 pede e que ela roda dentro de `@Transactional` (`FilaProcessamento.reivindicarProximo`).
**Ação:** aceito com os pontos sinalizados ao usuário (7 itens). Item 7 (lacuna de teste sem Postgres real) virou risco explícito — ver Fechamento.

---

## Prompt 2

Aprovado. Antes do commit: registre no ia/onde-errou.md ou como observação no commit — não como erro, mas como risco explícito — que a lacuna de teste sem Postgres real (item 7) cresceu nesta parte: agora ela cobre o mecanismo de concorrência da fila (FOR UPDATE SKIP LOCKED), que é o ponto mais arriscado do sistema para não ter sido exercitado contra um banco real. Isso deve aparecer também como candidato forte para a resposta da pergunta "o que quebra primeiro se o volume for multiplicado por dez" na carta de fechamento.

Pode preparar o registro de IA (002) e propor o commit.

**Retorno em uma linha:** criei este arquivo (`002`); registrei o risco no Fechamento abaixo e no corpo do commit; adicionei uma nota de trabalho sob a pergunta 2 de `carta/carta-de-fechamento.md`. Não pus em `ia/onde-errou.md` — aquele arquivo é para erro do agente, e isto é um trade-off de teste declarado, não um erro.
**Verifiquei:** reli o item 7 da apresentação para transcrever o risco sem exagerar nem suavizar; confirmei que `carta/` está fora do commit proposto (fica para quando o usuário redigir a carta).
**Ação:** aceito.

---

## Fechamento da sessão

**Entregue:** fila de jobs + worker de ponta a ponta contra o dublê; `Documento` com `tamanhoBytes` e `extensaoOriginal`; 23 testes verdes (11 novos entre unidade e slice).

**Não feito nesta sessão, escrito como não feito:**

- **Teste da fila contra Postgres real — risco explícito, cresceu nesta parte.** Os testes desta fatia são de unidade com repositório mockado. A fatia anterior já deixava sem cobertura o acesso a banco; esta parte adiciona à zona não testada o **mecanismo de concorrência da fila**: a query nativa `FOR UPDATE SKIP LOCKED`, a reivindicação com lease e o reclaim de job com lease expirado. É o ponto mais arriscado do sistema para não ter sido exercitado contra um banco real — a correção do SQL e o comportamento sob dois workers simultâneos só aparecem com Postgres de verdade (Testcontainers; H2 não serve, ADR 0002). Forte candidato à resposta da pergunta 2 da carta de fechamento ("o que quebra primeiro se o volume for multiplicado por dez").
- Timeout de 45s configurado mas não ligado (o dublê é síncrono; passa a valer com o adaptador real).
- Bytes do arquivo original não são guardados (`EntradaExtracao.conteudo` nulo) — achado 7 / restante da restrição (d).
- Migração versionada de schema (Flyway); README com instrução de subir o banco.

**Pendente:** commit do código + `002` (mostrado ao usuário antes de commitar).
