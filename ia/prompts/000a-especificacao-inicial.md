# 000a — especificação inicial

**Sessão:** 2026-08-31 (reconstruída)
**Objetivo:** escrever a primeira versão da especificação da DOC Intelligence, antes de qualquer código — nove seções: problema, recorte da fatia vertical (trilha A · back-end), modelo de domínio, contrato, módulos e fronteiras, processamento, restrições do ambiente, decisões registradas, e o que o projeto conscientemente não resolve.

> **Nota de reconstrução.** Este registro foi reconstruído em 2026-09-01 a partir do histórico de uma conversa com o assistente Claude, onde esta sessão de trabalho foi originalmente conduzida — antes de o registro formal em `ia/prompts/` começar a ser usado, a partir da sessão de implementação de código (`001`). Para ficar antes do `001` sem renumerar registros já versionados, os arquivos reconstruídos usam a numeração `000a`–`000f`. O conteúdo abaixo — decisões, alternativas discutidas, correções — reflete fielmente o que aconteceu; apenas o formato de prompt individual e os timestamps exatos foram reconstituídos a partir da conversa, não registrados em tempo real.

---

## Decisões e pontos-chave

1. **Fatia vertical para um único tipo de documento (`identidade`), de ponta a ponta:** recebimento → fila → adaptador (dublê) → `pronto`/conferência → conclusão/rejeição. Classificação automática entre os sete tipos fica fora — multiplicaria a superfície de teste sem mudar a arquitetura de fila, adaptador e conferência.
2. **Máquina de estados com nove estados:** `recebido`, `em_processamento`, `falha_temporaria`, `falha_definitiva`, `pronto`, `aguardando_conferencia`, `em_conferencia`, `concluido`, `rejeitado`. Terminais: `pronto`, `concluido`, `falha_definitiva`, `rejeitado`. Nenhuma transição pula etapa; cada transição é um evento gravado com timestamp, não uma sobrescrita.
3. **Seis módulos com fronteira de responsabilidade:** API, Domínio, Fila de processamento, Adaptador de extração, Persistência, Fila de conferência. Todo conhecimento do fornecedor de IA (nome, formato do prompt, versão do modelo) fica isolado no Adaptador de extração — é a fronteira que o enunciado exige por causa do fato de ambiente (f), "o modelo trocará de versão e os prompts mudarão".
4. **Contrato HTTP/JSON com seis operações** (`POST /documentos`, `GET /documentos/{id}`, `GET /documentos`, `POST /{id}/reivindicar`, `PATCH /{id}`, `POST /{id}/rejeitar`). Toda resposta de erro carrega um código de motivo, não apenas o status HTTP — porque "o fornecedor não respondeu" e "o arquivo é inválido" pedem tratamento diferente de quem consome.
5. **Processamento assíncrono fora do ciclo da requisição:** a chamada ao fornecedor leva de 5 a 40 s e às vezes falha; processar dentro do ciclo seguraria a conexão e perderia o trabalho a cada falha. `POST /documentos` responde na hora com o identificador; o worker consome a fila depois.
6. **Restrições do ambiente como seção própria (7)**, apontando para `docs/restricoes.md` — a ser preenchido, uma restrição por vez, com Tratamento ou Risco residual.
7. **Escopo consciente do que não se resolve (9):** classificação automática de tipo, deduplicação semântica, autenticação serviço a serviço, observabilidade/métricas, reprocessamento de `falha_definitiva`, retenção/expurgo de dado sensível.

## Resultado

`docs/01-especificacao.md` v1 (data da primeira versão: 2026-08-31). Nenhum código ainda.
