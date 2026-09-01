# 0002 — Persistência do estado do documento e dos campos extraídos

- **Data:** 2026-09-01
- **Status:** aceita

## Contexto

O sistema guarda, para cada documento: estado atual, histórico de transições (append-only),
campos extraídos (estrutura que varia por tipo de documento), confiança do documento, nome
padronizado, versão do adaptador/prompt que gerou a extração, dados da reivindicação da
conferência e correção aplicada (especificação §3). A fila de processamento também precisa de
armazenamento durável: um worker que morre no meio não pode deixar o documento preso em
`em_processamento` (especificação §6, "Reinício com trabalho em andamento"), o que só funciona
se o estado sobreviver ao reinício do processo.

O back-end é Java + Spring Boot (trilha A). Restrições de ambiente relacionadas: **(a)** o
processamento é assíncrono e falível, então o estado do job tem de ser recuperável; **(e)**
pico de mais de 800 documentos entre 9h e 11h, que o armazenamento precisa absorver sem perder
trabalho; **(d)** o conteúdo é dado pessoal, o que reforça a necessidade de um mecanismo de
acesso e retenção controlável (tratado como risco parcial nesta entrega — ver
[`restricoes.md`](../restricoes.md)).

Prazo de três dias; a fatia vertical entrega um tipo de documento de ponta a ponta.

## Alternativas consideradas

### A. MongoDB (banco de documentos)

Guardaria cada documento como um único registro BSON, com os campos extraídos aninhados e sem
schema fixo.

**Descartada porque:** o domínio tem entidades relacionais claras — documento, transições de
estado, campos extraídos por tipo — e nenhuma necessidade real de schema flexível que
justifique um banco de documentos. O histórico de transições append-only e a fila de
conferência se beneficiam de integridade referencial (chave estrangeira entre transição e
documento, entre reivindicação e documento), que um banco relacional garante e um de documentos
empurra para a aplicação. A variação dos campos extraídos por tipo é estreita e conhecida, não
aberta.

### B. H2 ou outro banco em memória

Banco relacional embutido, no mesmo processo, sem configuração.

**Descartada porque:** não sobrevive a reinício do processo. Isso inviabiliza testar o cenário
central de "worker morre no meio do processamento" descrito na especificação §6 — se o estado
some junto com o processo, não há o que recuperar, e o mecanismo de reivindicação com
expiração fica sem como ser exercitado. Um banco em memória também esconde diferenças de
comportamento (tipos, isolamento transacional, JSON) que só apareceriam em produção.

## Decisão

PostgreSQL, acessado via Spring Data JPA.

## Consequências

- A estrutura de "campos extraídos", que varia por tipo de documento, precisa de uma coluna
  `JSONB` ou de uma tabela separada de campos. É decisão de modelagem que fica para a
  implementação — não trava este ADR, mas é o primeiro ponto a resolver ao escrever o schema.
- Um processo PostgreSQL separado passa a ser dependência de execução: o README precisa de
  instrução para subir o banco (container ou local) e os testes precisam de um Postgres real
  (Testcontainers ou equivalente) em vez de H2 — mais lento que um banco em memória.
- JPA/Hibernate adiciona uma camada de mapeamento com custo de tuning (estratégia de fetch,
  fronteira de transação, lazy loading) que um acesso mais direto (JDBC ou jOOQ) não teria.
  Aceito pela integração pronta com Spring Boot e pela velocidade de escrever o CRUD da fatia.
- Ganha-se integridade referencial e transações ACID, que a fila de jobs (ADR 0003) usa para
  reivindicar trabalho sem corrida entre workers.

**Gatilho de revisão:** se surgir um segundo ou terceiro tipo de documento com campos
radicalmente diferentes e consultas frequentes sobre o conteúdo desses campos, reavaliar
`JSONB` versus tabelas por tipo. No limite, se a maior parte do acesso virar leitura de
estrutura semiestruturada, reabrir a comparação com um banco de documentos.
