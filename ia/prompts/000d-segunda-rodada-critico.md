# 000d — segunda rodada do critico-de-especificacao + correções (N1–N5, N7)

**Sessão:** 2026-09-01 (reconstruída)
**Objetivo:** rodar a segunda rodada do `critico-de-especificacao` para confirmar se as correções da sessão `000c` resolveram de verdade, e corrigir as inconsistências que as próprias rodadas de correção introduziram entre partes do documento.

> **Nota de reconstrução.** Este registro foi reconstruído em 2026-09-01 a partir do histórico de uma conversa com o assistente Claude, onde esta sessão de trabalho foi originalmente conduzida — antes de o registro formal em `ia/prompts/` começar a ser usado, a partir da sessão de implementação de código (`001`). Para ficar antes do `001` sem renumerar registros já versionados, os arquivos reconstruídos usam a numeração `000a`–`000f`. O conteúdo abaixo — decisões, alternativas discutidas, correções — reflete fielmente o que aconteceu; apenas o formato de prompt individual e os timestamps exatos foram reconstituídos a partir da conversa, não registrados em tempo real.

---

## Decisões e pontos-chave

1. **A rodada 2 confirmou** os achados 1, 2, 3, 5, 6 e 9 como resolvidos, com ressalvas, e levantou **N1–N11** — a maioria são inconsistências entre seções introduzidas pelas correções da rodada 1, mais pontos menores.
2. **N2 (bloqueante):** o diagrama da §3 não tinha a transição `em_conferencia → aguardando_conferencia` quando a reivindicação expira sem conclusão — adicionada. A prosa da §6 já a descrevia; faltava no desenho.
3. **N1:** a §2 (passo 5) passou a mencionar `rejeitado` como resultado possível dentro do escopo desta entrega, coerente com a rota `POST /documentos/{id}/rejeitar` já na §4.
4. **N3:** a §6 passou a explicitar que `falha_definitiva` é visível via `GET /documentos?status=falha_definitiva`, sem operação dedicada de reprocessamento nesta entrega (a intervenção é manual e fora da API, §9).
5. **N4:** a classificação do erro do adaptador como temporário ou definitivo (pelo tipo de erro e pela contagem de tentativas) foi atribuída explicitamente à **Fila de processamento** na §5, que também aciona o Domínio para registrar a transição final quando o limite de tentativas se esgota.
6. **N5:** "corrompido" saiu da lista de motivos de `422` na §4. Arquivo que passa em formato e tamanho mas está corrompido **não** é barrado na fronteira — segue para processamento e vira `falha_temporaria`/`falha_definitiva` ou confiança baixa. Coerente com `restricoes.md` (b): não há decodificação de conteúdo na fronteira.
7. **N7:** a §2 passou a usar `≥ 0,85` / `< 0,85`, a mesma notação do ADR 0005, da §3 e do diagrama (antes usava `>`).
8. **`restricoes.md` (c), (e), (f), (g) preenchidas**, puxando do que já estava decidido: idempotência por hash na criação; número de workers limitado e abaixo do rate limit do fornecedor, excesso enfileirado e não recusado; fronteira do Adaptador de extração isolando o fornecedor; reivindicação com expiração e `409` na disputa de conferência.
9. **Deixado em aberto como risco aceito e registrado** (não como lacuna silenciosa): achado 7 / restante de (d) — retenção/expurgo, criptografia em repouso, log de acesso, minimização do que vai ao fornecedor, destino do arquivo original; achado 8 — teto de custo por janela; achado 10 / N8 — números sem valor testado (expiração de reivindicações, workers, rate limit, paginação, profundidade da fila) e os números de volume de (e) que não reconciliam entre si; achados menores 12, 13, 14, 15; N10 (adaptador não devolver um campo obrigatório); N11 (documento em `pronto` com campo obrigatório errado, sem operação de retorno).

## Resultado

Bloco "Registro de crítica" de `docs/01-especificacao.md` atualizado (rodada 2: 2026-09-01), com as subseções "Confirmados na rodada 2", "Resolvidos na rodada 2" e "Em aberto". `docs/restricoes.md` completa — as sete restrições com Tratamento e Risco residual. A especificação passa a estar pronta para virar código.
