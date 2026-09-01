# Prompts

Um arquivo por sessão de trabalho, numerado, na ordem em que aconteceram. Na íntegra, como foram escritos — sem correção, sem resumo, sem reescrita posterior.

Os becos sem saída ficam. São a parte mais informativa do registro.

Ver a skill `registro-de-ia` em `.claude/skills/registro-de-ia/` para o formato e as regras.

## Numeração

- **`000-template.md`** — modelo, não é uma sessão.
- **`000a`–`000f`** — as seis sessões de especificação e arquitetura que aconteceram **antes** de o registro formal em `ia/prompts/` começar a ser usado. Foram **reconstruídas em 2026-09-01** a partir do histórico de uma conversa com o assistente Claude (fora do Claude Code), onde foram originalmente conduzidas. Cada um desses arquivos abre com uma **Nota de reconstrução** e, no lugar dos prompts na íntegra (que não foram registrados em tempo real), traz o objetivo e a lista de decisões e pontos-chave da sessão. A numeração decimal (`000a`, `000b`, …) foi escolhida para inseri-las antes do `001` sem renumerar os registros já versionados.
- **`001` em diante** — sessões registradas em tempo real, com os prompts na íntegra, a partir da implementação de código.

### As seis sessões reconstruídas

| # | Sessão |
|---|---|
| `000a` | Especificação inicial (nove seções) |
| `000b` | Primeira rodada do `critico-de-especificacao` + triagem dos 15 achados |
| `000c` | Correções da primeira rodada — achados 1 e 2, base do 4 (confiança, nome padronizado, `restricoes.md` (a)/(b)) |
| `000d` | Segunda rodada do crítico + correções N1–N5, N7; `restricoes.md` (c)/(e)/(f)/(g) |
| `000e` | ADRs 0002 (persistência) e 0003 (fila de processamento) |
| `000f` | ADR 0006 (granularidade dos módulos) — decisão nesta sessão, arquivo redigido em `001` |
