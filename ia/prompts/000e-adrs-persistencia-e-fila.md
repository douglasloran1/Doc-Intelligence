# 000e — ADRs 0002 (persistência) e 0003 (fila de processamento)

**Sessão:** 2026-09-01 (reconstruída)
**Objetivo:** decidir e registrar a persistência e o mecanismo da fila de processamento antes de escrever código.

> **Nota de reconstrução.** Este registro foi reconstruído em 2026-09-01 a partir do histórico de uma conversa com o assistente Claude, onde esta sessão de trabalho foi originalmente conduzida — antes de o registro formal em `ia/prompts/` começar a ser usado, a partir da sessão de implementação de código (`001`). Para ficar antes do `001` sem renumerar registros já versionados, os arquivos reconstruídos usam a numeração `000a`–`000f`. O conteúdo abaixo — decisões, alternativas discutidas, correções — reflete fielmente o que aconteceu; apenas o formato de prompt individual e os timestamps exatos foram reconstituídos a partir da conversa, não registrados em tempo real.

---

## Decisões e pontos-chave

### ADR 0002 — Persistência

1. **PostgreSQL via Spring Data JPA**, com os campos extraídos numa coluna `jsonb` (estrutura dependente do tipo de documento, cada campo com confiança própria).
2. **Descartada — banco de documento (MongoDB):** o domínio é pequeno e majoritariamente relacional (estado do documento + histórico append-only de transições, com consulta e listagem filtrada); o ganho de esquema flexível não se paga, e os campos extraídos variáveis já cabem no `jsonb` do Postgres.
3. **Descartada — H2 em teste:** de propósito, para não validar contra um banco diferente do de produção. Consequência aceita e registrada: os testes desta entrega usam repositório mockado, e o teste de integração contra um PostgreSQL real (Testcontainers) fica adiado — é o ponto mais frágil da entrega, por não exercitar `FOR UPDATE SKIP LOCKED`, o reaproveitamento de job com lease expirado e a JPQL de listagem com filtro nulo.
4. Migração versionada (Flyway) adiada; na fatia vertical o schema é criado por `ddl-auto: update`.

### ADR 0003 — Fila de processamento

1. **Tabela de jobs no próprio PostgreSQL**, consumida com `SELECT ... FOR UPDATE SKIP LOCKED`, com reivindicação por worker e expiração (lease).
2. **Descartada — fila de mensagens dedicada (RabbitMQ, Kafka):** mais uma peça de infraestrutura para subir, operar e testar dentro de três dias, sem ganho no volume declarado (150 documentos/dia, pico de mais de 800 entre 9h e 11h). O Postgres já está no projeto por causa do ADR 0002.
3. **Descartada — fila em memória:** perde o trabalho em andamento no reinício do processo, exatamente o que a restrição (a) proíbe ("perder o trabalho inteiro a cada falha do fornecedor").
4. O mesmo padrão de "reivindicação com expiração" vale para o worker da fila de processamento e para a fila de conferência humana — o mesmo problema em duas escalas de tempo (segundos para a máquina, minutos para a pessoa).

## Resultado

`docs/adr/0002-persistencia.md` e `docs/adr/0003-fila-de-processamento.md` (Status: aceita). Tabela da §8 de `docs/01-especificacao.md` e `docs/adr/README.md` atualizadas com as duas linhas.
