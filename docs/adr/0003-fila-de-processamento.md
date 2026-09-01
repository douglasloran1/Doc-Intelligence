# 0003 — Mecanismo da fila de processamento

- **Data:** 2026-09-01
- **Status:** aceita

## Contexto

`POST /documentos` responde na hora com o identificador e o processamento acontece fora do
ciclo da requisição, num worker que consome uma fila (especificação §6). A fila precisa:
sobreviver ao reinício do processo; permitir que um item cuja reivindicação (lease) expirou
volte a ficar disponível para outro worker; e limitar quantos itens são processados em
paralelo, abaixo do limite de taxa do fornecedor (especificação §6; restrições de ambiente
**(a)** latência e falha do fornecedor, **(e)** pico de mais de 800 documentos entre 9h e 11h
contra cerca de 150 por dia no total).

O projeto já decidiu usar PostgreSQL para persistência (ADR 0002). Back-end Java + Spring Boot.
Prazo de três dias; o volume da fatia vertical é de escala de teste.

## Alternativas consideradas

### A. RabbitMQ ou Kafka (broker de mensagens dedicado)

Um serviço de fila separado, com entrega, confirmação e reentrega próprios.

**Descartada porque:** é infraestrutura adicional para configurar, subir e manter (broker,
conexões, filas/tópicos, dead-letter), sem benefício real dado o volume da fatia vertical e o
prazo. Como o projeto já sobe um PostgreSQL, uma tabela de jobs dá uma fila com garantias
transacionais: reivindicar um job e gravar a transição de estado do documento acontecem na
mesma transação, sem o problema de coordenar dois sistemas (mensagem entregue mas transação do
banco falhou, ou o inverso). Kafka em particular é desenhado para throughput e retenção de log
que este problema não tem.

### B. Fila em memória, dentro do processo

Uma `BlockingQueue` ou equivalente, alimentada pelo endpoint e consumida por threads worker no
mesmo processo.

**Descartada porque:** não sobrevive ao reinício do processo — mesmo motivo do banco em memória
no ADR 0002. Um documento já aceito e ainda na fila em memória some se o processo cair,
quebrando a promessa de que nada aceito se perde (especificação §6). Também acopla a capacidade
de processamento ao ciclo de vida de um único processo.

## Decisão

Uma tabela de jobs no próprio PostgreSQL, consultada periodicamente por um worker (polling),
que reivindica itens com lease e expiração usando as garantias transacionais do banco
(`SELECT ... FOR UPDATE SKIP LOCKED` ou equivalente).

## Consequências

- Sob volume alto de produção, uma tabela de jobs no Postgres tem limites de performance que
  uma fila dedicada não teria: o polling gera carga constante mesmo com a fila vazia,
  `SKIP LOCKED` sob muitos workers concorrentes tem contenção, e a tabela cresce e precisa de
  expurgo dos jobs concluídos. É um gatilho de revisão explícito, não um problema ignorado
  (ver abaixo).
- O polling introduz latência entre enfileirar e começar a processar, da ordem do intervalo de
  polling. Aceitável para o alvo deste serviço; ajustável por configuração.
- Nenhuma dependência de infraestrutura além do PostgreSQL que o ADR 0002 já exige — README e
  testes não ganham um serviço a mais.
- O mesmo padrão de "reivindicação com expiração" já usado na fila de conferência humana
  (especificação §4 e §6) se aplica aqui, na fila de processamento — uma peça conceitual, duas
  escalas de tempo.

**Gatilho de revisão:** se o volume sustentado passar de algumas dezenas de documentos por
minuto, se o número de workers concorrentes crescer a ponto de a contenção em `SKIP LOCKED`
aparecer nos tempos de processamento, ou se a latência do polling deixar de ser aceitável,
migrar a fila para um broker dedicado (RabbitMQ é o candidato natural pelo modelo de trabalho).
A fronteira do módulo "Fila de processamento" (especificação §5) existe para que essa troca não
toque os outros módulos.
