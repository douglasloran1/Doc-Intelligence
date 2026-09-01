# 000c — correções da primeira rodada (achados 1, 2 e base do 4)

**Sessão:** 2026-08-31 (reconstruída)
**Objetivo:** resolver o achado 1 (confiança sem regra de cálculo) e o achado 2 (nome padronizado sem definição), e começar a preencher `docs/restricoes.md` (achado 4).

> **Nota de reconstrução.** Este registro foi reconstruído em 2026-09-01 a partir do histórico de uma conversa com o assistente Claude, onde esta sessão de trabalho foi originalmente conduzida — antes de o registro formal em `ia/prompts/` começar a ser usado, a partir da sessão de implementação de código (`001`). Para ficar antes do `001` sem renumerar registros já versionados, os arquivos reconstruídos usam a numeração `000a`–`000f`. O conteúdo abaixo — decisões, alternativas discutidas, correções — reflete fielmente o que aconteceu; apenas o formato de prompt individual e os timestamps exatos foram reconstituídos a partir da conversa, não registrados em tempo real.

---

## Decisões e pontos-chave

1. **Confiança do documento = mínimo entre as confianças dos campos obrigatórios do tipo.** Para `identidade`: nome completo, CPF e data de nascimento. Órgão emissor e data de emissão são extraídos e propostos, mas não entram no cálculo e não seguram o documento na conferência.
2. **Escala e limiar.** Confiança por campo entre 0,0 e 1,0, fornecida pelo adaptador. Limiar do documento em **0,85, provisório** (não confirmado com o cliente), morando numa única constante de configuração. `≥ 0,85` → `pronto`; `< 0,85` → `aguardando_conferencia`.
3. **Comportamento do dublê determinístico.** Fixa a confiança dos campos obrigatórios pelo tamanho em bytes do arquivo recebido — abaixo de 500 KB, **0,60**; a partir de 500 KB, **0,95** — e a dos não-obrigatórios em **0,90**. Como a confiança do documento é o mínimo dos obrigatórios, arquivo `< 500 KB` cai em `aguardando_conferencia` e `≥ 500 KB` vai para `pronto`: dois arquivos de tamanhos diferentes exercitam os dois ramos da máquina de estados sem o fornecedor real. A relação tamanho↔qualidade é aproximação assumida para esta entrega, não medida de qualidade de OCR.
4. **Alternativas discutidas e descartadas para o dublê** (registradas no ADR 0005): variar a confiança pelo nome do arquivo; pelo hash do conteúdo; por um parâmetro explícito de teste na requisição; por alternância entre respostas; por configuração fixa única; e **OCR real via Tesseract** — descartada por custo de tempo dentro do prazo de três dias, já que o que está sendo avaliado é a arquitetura de fronteira do adaptador, não a qualidade de extração.
5. **Nome padronizado = `identidade_{id-do-documento}.{extensão-original}`**, onde `id-do-documento` é o identificador interno gerado no recebimento. **Nenhum dado pessoal** — CPF, nome, data de nascimento — entra no nome do arquivo; esses campos ficam só no registro do documento no banco. Discussão explícita: o nome do arquivo circula entre sistemas internos e aparece em log; é decisão de minimização, restrição de ambiente (d).
6. **`restricoes.md` (a) e (b) preenchidas com números concretos:** timeout de **45 s** por chamada ao adaptador, **3 tentativas**, backoff exponencial de base 5 s (esperas de **5 s, 10 s, 20 s**); formatos aceitos **jpg, jpeg, png, pdf**, tamanho máximo **15 MB**, rejeição com `422` antes de entrar na fila.

## Resultado

`docs/adr/0005-calculo-do-nivel-de-confianca.md` criado (Status: proposta). §§ 2, 3 e 6 de `docs/01-especificacao.md` alinhadas à regra de confiança e ao dublê. `docs/restricoes.md` (a) e (b) com Tratamento preenchido. O achado 3 (dublê não exercita os ramos) fica encaminhado: a regra de tamanho leva por `pronto` e por `aguardando_conferencia → em_conferencia → (concluido | rejeitado)`; os ramos `falha_temporaria`/`falha_definitiva` seguem escritos como **não** exercitados pelo dublê.
