# 003 — consulta e fila de conferência

**Sessão:** 2026-09-01
**Objetivo:** fechar o código da fatia vertical — `GET /documentos/{id}`, listagem paginada, e a fila de conferência humana (`reivindicar`, `PATCH`, `rejeitar`) com expiração de reivindicação, seguindo a §4.

---

## Prompt 1

Última parte de código da fatia vertical: consulta e fila de conferência, seguindo a especificação §4.

Preciso que você:

1. GET /documentos/{id} — devolve estado atual, campos extraídos, confiança, nome padronizado, e o histórico de transições. 404 se não existir.

2. GET /documentos?status=&tipo= — listagem paginada, filtrável por estado e tipo (conforme §4). Defina um tamanho de página padrão razoável.

3. POST /documentos/{id}/reivindicar — marca o documento como em_conferencia, associa a um identificador de operador (recebido no corpo ou header, sua escolha, mas documente qual), com expiração (lease-segundos, reaproveite o padrão do worker se fizer sentido, ou defina um valor próprio para conferência humana — sugiro mais longo que o do worker, por exemplo 300s, já que é uma pessoa revisando, não uma máquina). 409 se já houver reivindicação ativa de outro operador; regra: só documentos em aguardando_conferencia podem ser reivindicados.

4. PATCH /documentos/{id} — grava a correção dos campos feita pelo operador; exige reivindicação ativa do mesmo operador (senão 409); move para concluido. Valide que o documento está em em_conferencia.

5. POST /documentos/{id}/rejeitar — move para rejeitado; exige reivindicação ativa do mesmo operador e um campo "motivo" obrigatório no corpo (422 se faltar); mesma validação de estado do PATCH.

6. Implemente também a expiração: quando a reivindicação expira sem PATCH/rejeitar, o documento volta para aguardando_conferencia (isso pode ser verificado sob demanda, quando alguém tenta reivindicar ou consultar, sem precisar de um worker dedicado para isso — mas documente a escolha).

7. Testes: reivindicação bem-sucedida, 409 na disputa de reivindicação, PATCH movendo para concluido, rejeitar exigindo motivo, e a expiração liberando o documento para nova reivindicação.

Mesma regra: me mostre tudo antes de qualquer commit.

**Retorno em uma linha:** `ConferenciaService` (reivindicar/corrigir/rejeitar + `liberarSeExpirado`); `Documento` ganhou `reivindicacaoExpiraEm` e `correcaoAplicada`; `GET /{id}` (detalhe + histórico, 404) e `GET` (listagem paginada, página padrão 20, filtro por estado/tipo com `PaginaResponse` próprio) em `DocumentoService` como leitura pura; exceções 404/409/422 com `{codigo, mensagem}` no `TratadorDeErros`; `conferencia.lease-segundos: 300` no `application.yml`; 12 testes novos; 35 no total, verdes.
**Verifiquei:** rodei `mvn test` — 35/35; `ConferenciaServiceTest` cobre os cinco casos do item 7 mais 404 e disputa por operador diferente no PATCH; conferi que a expiração grava as duas transições (`em_conferencia→aguardando_conferencia` motivo "reivindicacao expirada", depois `aguardando_conferencia→em_conferencia`) — bate com a transição N2 do diagrama; conferi no `DocumentoController` que os `GET` não chamam nada que altere estado (consistente com a §3 / achado 5); reli a §4 para confirmar os códigos HTTP de cada rota.
**Ação:** aceito, com um desvio explícito do item 6 — a expiração é verificada só no `reivindicar`, **não** no `GET`, porque disparar no `GET` contradiria a §3 ("nenhuma leitura altera estado", achado 5 resolvido). Apresentado como ponto 1 dos sete para revisão.

---

## Prompt 2

Aprovado. Concordo especificamente com a decisão do ponto 1 — está certo em priorizar a consistência com o achado 5 (nenhuma leitura muda estado) em vez de seguir minha sugestão original, que contradizia isso. Boa captura.

Pode preparar o registro de IA (003), fazer o check de onde-errou.md — sem necessidade de nova entrada a menos que tenha havido erro real, já que o ponto 1 foi uma decisão correta, não um erro — e propor o commit.

**Retorno em uma linha:** criei este arquivo; revisei `ia/onde-errou.md` e não adicionei entrada — não houve erro real nesta sessão (o desvio do item 6 foi decisão consciente, sinalizada antes da aprovação, e o usuário confirmou como correta).
**Verifiquei:** repassei as decisões da sessão (operador no corpo, lease 300s, renovação pelo mesmo operador, códigos 409/422, página 20, `GET` puro) — todas foram instruídas, oferecidas como escolha, ou sinalizadas para aprovação; nenhuma foi tomada em cima de ponto rastreado em aberto sem avisar (diferente do N6 na sessão 001).
**Ação:** aceito.

---

## Fechamento da sessão

**Entregue:** consulta (`GET /{id}`, listagem) e fila de conferência (`reivindicar`, `PATCH`, `rejeitar`, expiração sob demanda) de ponta a ponta; `Documento` com `reivindicacaoExpiraEm` e `correcaoAplicada`; 35 testes verdes (12 novos). Com isso o caminho da fatia vertical está completo em código: recebimento → fila → worker/dublê → pronto/conferência → conclusão/rejeição.

**Não feito nesta sessão, escrito como não feito:**

- Liberação por expiração **não** roda no `GET` nem por varredura periódica — só quando alguém tenta reivindicar. Documento abandonado no meio da conferência fica em `em_conferencia` com lease vencido até a próxima tentativa de reivindicação. Varredura periódica é a evolução; se a decisão for o `GET` disparar também, vira mudança na §3 + registro em `divergencias.md`.
- `ConferenciaService`, `DocumentoService.listar`/`detalhar` e a JPQL de `listar` (filtro com enum nulo) testados com repositório mockado — a verificação contra Postgres real segue para o teste de integração com Testcontainers, ainda adiado (risco menor que o da fila: aqui não há primitiva de concorrência).
- Sem valor testado para o lease de conferência (300s) — configuração a ajustar (achado 10).

**Pendente:** commit do código + `003` (mostrado ao usuário antes de commitar). Depois desta fatia, o que resta da entrega é não-código: README, revisão do ADR 0001, decisão sobre registro de IA retroativo, e a carta de fechamento.
