# 000b — primeira rodada do critico-de-especificacao + triagem

**Sessão:** 2026-08-31 (reconstruída)
**Objetivo:** rodar o subagente `critico-de-especificacao` contra a especificação v1, em contexto limpo, e triar os achados antes de corrigir qualquer coisa.

> **Nota de reconstrução.** Este registro foi reconstruído em 2026-09-01 a partir do histórico de uma conversa com o assistente Claude, onde esta sessão de trabalho foi originalmente conduzida — antes de o registro formal em `ia/prompts/` começar a ser usado, a partir da sessão de implementação de código (`001`). Para ficar antes do `001` sem renumerar registros já versionados, os arquivos reconstruídos usam a numeração `000a`–`000f`. O conteúdo abaixo — decisões, alternativas discutidas, correções — reflete fielmente o que aconteceu; apenas o formato de prompt individual e os timestamps exatos foram reconstituídos a partir da conversa, não registrados em tempo real.

---

## Decisões e pontos-chave

1. **15 achados**, classificados pelo crítico em 4 bloqueantes, 7 importantes, 4 menores.
2. **Bloqueantes:**
   - **1** — "nível de confiança" usado sem regra de cálculo, sem escala e sem valor de limiar.
   - **2** — campos do tipo identidade e formato do nome padronizado nunca definidos.
   - **3** — o dublê determinístico, como descrito, não exercita os ramos de erro e de conferência da máquina de estados.
   - **4** — a §7 apontava para `restricoes.md` como evidência de cobertura, mas o arquivo estava com todos os tratamentos em branco.
3. **Importantes (5–11):** incoerência entre o diagrama da §3 (um `GET` levaria o documento a `consultado`) e a §4 (`GET` é leitura pura), e `pronto` terminal ou não; nenhum módulo da §5 dono da máquina de estados nem da lógica de domínio (nome padronizado, mapeamento de campos por tipo); restrição (d) coberta pela metade (onde ficam os bytes do arquivo, cifra, log de acesso, destino após processamento); restrição (a) sem menção a teto de custo por janela de tempo; sem rota de rejeição na conferência e sem operação definida sobre `falha_definitiva`; números ausentes em bloco (formatos e tamanho de arquivo, timeout, tentativas, backoff, expiração das reivindicações, número de workers, paginação); a §2 não demarca quais mecanismos da §6 são código nesta entrega e quais são só desenho.
4. **Menores (12–15):** idempotência por hash sem janela de tempo nem exceção por estado terminal; quem declara o `tipo` na entrada não está dito, e o filtro `?tipo=` numa fatia de um tipo só; a posse da conferência repousa sobre identificador de operador auto-declarado e não verificado; o operador barrado com `409` não vê quem reivindicou nem quando a reivindicação expira.
5. **Triagem:** resolver os quatro bloqueantes primeiro (sessão `000c`); importantes e menores em seguida, parte deles aceita explicitamente como risco conhecido na §9 e em `restricoes.md`.

## Resultado

Lista dos 15 achados registrada no bloco "Registro de crítica" de `docs/01-especificacao.md` (rodada 1: 2026-08-31). Nenhuma correção nesta sessão — só levantamento e triagem.
