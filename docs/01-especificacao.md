# Especificação — DOC Intelligence

> Escrita antes do código. Se a implementação divergir, este documento permanece como está e a divergência é registrada em [`divergencias.md`](divergencias.md).

**Data da primeira versão:** 2026-08-31
**Trilha:** A · back-end

---

## 1. Problema

Todos os dias o escritório recebe documentos de clientes por WhatsApp, e-mail e balcão — identidades, comprovantes de residência, contracheques, carteiras de trabalho, laudos, procurações, contratos, e fotografias tortas desses mesmos papéis. Hoje uma pessoa abre cada arquivo, descobre o que é, renomeia num padrão interno e digita os dados numa planilha. São quatro minutos por documento, e o volume cresce junto com a base de clientes.

O trabalho não é complexo — é repetitivo, e repetitivo em escala é o tipo de problema que software resolve bem. O DOC Intelligence substitui a leitura e a digitação por um serviço: recebe o arquivo, descobre o tipo, extrai os campos que interessam, propõe um nome padronizado, e só entrega como pronto o que tiver confiança suficiente. O que a máquina não tiver certeza, vai para uma fila de conferência humana — a pessoa deixa de digitar do zero e passa a corrigir o que já veio pronto.

## 2. Produto-alvo versus escopo desta entrega

O produto-alvo tem cinco comportamentos: receber um documento; classificar o tipo e extrair os campos daquele tipo, propondo um nome padronizado; permitir consultar um resultado e listar os já processados; segurar para conferência humana quando a confiança for baixa; e ser consumido por sistemas internos do escritório, não por navegador aberto.

**Esta entrega não é o produto-alvo.** É o projeto do sistema mais uma fatia vertical.

### A fatia implementada

Um caminho completo de ponta a ponta, estreito, para um único tipo de documento (identidade):

1. A API recebe o arquivo via `POST /documentos`, valida o essencial (formato, tamanho) e responde imediatamente com um identificador e o status `recebido`.
2. O documento entra numa fila de processamento. Um worker consome a fila, chama o adaptador de extração — que nesta entrega é um dublê determinístico, não o fornecedor real — e recebe de volta um conjunto de campos com um nível de confiança.
3. Se a confiança está no limiar ou acima (`≥ 0,85`, provisório — ver ADR 0005), o documento vai para `pronto`, com nome padronizado proposto e campos extraídos. Se está abaixo (`< 0,85`), vai para `aguardando_conferência`.
4. `GET /documentos/{id}` devolve o estado atual e os campos, prontos ou não. `GET /documentos` lista com filtro por status.
5. Para o documento em conferência, `POST /documentos/{id}/reivindicar` marca que alguém está corrigindo (com expiração), e `PATCH /documentos/{id}` grava a correção e move para `concluído`. Quando o documento não é corrigível — ilegível, tipo errado, dados irrecuperáveis — `POST /documentos/{id}/rejeitar` move para `rejeitado`, também um resultado possível dentro do escopo desta entrega.

**Campos do tipo identidade.** Só os campos obrigatórios entram no cálculo da confiança do documento (seção 3); os demais são extraídos e propostos, mas não seguram o documento na conferência se vierem com confiança baixa. Ver ADR 0005.

| Campo | Obrigatório | Entra no cálculo de confiança do documento |
|---|---|---|
| Nome completo | Sim | Sim |
| CPF | Sim | Sim |
| Data de nascimento | Sim | Sim |
| Órgão emissor | Não | Não |
| Data de emissão | Não | Não |

Esse caminho atravessa validação de entrada, fila assíncrona, adaptador substituível, persistência de estado, e a fila de conferência com controle de concorrência — que são exatamente os pontos onde o enunciado espera que o candidato demonstre raciocínio, e não apenas o caminho feliz de uma chamada síncrona a uma API de IA.

### Fora desta entrega, por escolha

- **Classificação automática de tipo de documento.** A entrada assume identidade como tipo único. Classificar entre sete tipos multiplicaria a superfície de teste sem acrescentar nada ao que está sendo avaliado — a arquitetura de fila, adaptador e conferência é a mesma independentemente de quantos tipos existem. Ver ADR correspondente.
- **Autenticação e autorização reais.** A API assume um único chamador de confiança. Um sistema interno real precisaria de identidade de serviço; aqui isso é registrado como risco aceito.
- **Interface gráfica.** Consumo via chamada HTTP direta ou script; não há tela.
- **Deploy e infraestrutura produtiva.** Roda localmente via instrução no README; não há orquestração, nem ambiente gerenciado.
- **Cobertura de teste ampla.** Testes concentrados nos pontos que a especificação aponta como críticos (ver seção de testes no README) — máquina de estados do documento e idempotência de entrada duplicada.

### Por que este recorte

O que está sendo avaliado, segundo a própria banca, é como o candidato recorta o problema e como conduz decisão sob restrição de tempo — não quantos dos cinco comportamentos ficam de pé. Um tipo de documento processado de ponta a ponta, com fila, adaptador e conferência funcionando de verdade, demonstra mais sobre arquitetura do que cinco tipos processados de forma rasa e sem tratamento de falha. A extensão para mais tipos é, por desenho, uma mudança de dado (um novo mapeamento de campos) e não uma mudança de arquitetura — o que é precisamente o ponto que a fronteira do adaptador defende.

## 3. Modelo do domínio

Um documento nasce no recebimento e atravessa estados até um estado terminal. Não há transição que pule etapa, e toda transição fica registrada com timestamp para permitir reconstruir o histórico de qualquer item.

```
recebido
   │  (worker consome da fila)
   ▼
em_processamento
   │
   ├── falha_temporária ──► (retry com backoff, volta para em_processamento)
   │                         até o limite de tentativas
   │
   ├── falha_definitiva ──► [estado terminal — erro registrado, sem retry automático]
   │
   ├── confiança ≥ 0,85 ──► pronto  [estado terminal]
   │
   └── confiança < 0,85 ──► aguardando_conferência
                                   │  (reivindicado por um humano)
                                   ▼
                              em_conferência
                                   │
                                   ├── (correção registrada) ──► concluído  [estado terminal]
                                   │
                                   ├── (não corrigível, com motivo obrigatório) ──► rejeitado  [estado terminal]
                                   │
                                   └── (reivindicação expira sem conclusão) ──► volta para aguardando_conferência
```

Os estados terminais são `pronto`, `concluído`, `falha_definitiva` e `rejeitado` — um documento em qualquer um deles não muda mais de estado. Nenhuma leitura altera estado: `GET /documentos/{id}` e `GET /documentos` são consulta pura (seção 4).

Cada transição de estado é um evento gravado, não uma sobrescrita — o registro de quando e por que um documento mudou de estado é o que permite responder, depois, "por que este item ainda está pendente" sem precisar adivinhar.

**Campos do documento:** identificador, hash do arquivo original, tipo declarado, estado atual, histórico de transições, campos extraídos (estrutura dependente do tipo, cada campo com uma confiança própria entre 0,0 e 1,0 fornecida pelo adaptador), confiança do documento, nome padronizado proposto, versão do adaptador/prompt que gerou a extração, quem reivindicou a conferência e quando, correção aplicada (se houver).

**Confiança do documento.** É o **mínimo entre as confianças dos campos obrigatórios do tipo** — para identidade, nome completo, CPF e data de nascimento (a lista completa de campos e sua obrigatoriedade está na seção 2). Os campos não-obrigatórios não entram no cálculo, mesmo quando vêm com confiança baixa. O documento vai para `pronto` quando esse mínimo é `≥ 0,85` e para `aguardando_conferência` quando é menor. O limiar de 0,85 é provisório — não confirmado com o cliente — e vive numa única constante de configuração. Regra de agregação, valor do limiar e alternativas descartadas estão no ADR 0005.

**Nome padronizado do arquivo.** O nome proposto segue o padrão `identidade_{id-do-documento}.{extensão-original}`, onde `id-do-documento` é o identificador interno que o sistema gera no recebimento e `extensão-original` é a extensão do arquivo enviado (`jpg`, `jpeg`, `png` ou `pdf`) — o sistema não converte o formato. Nenhum dado pessoal — CPF, nome, data de nascimento — entra no nome do arquivo; esses campos ficam apenas no registro do documento no banco. É decisão consciente de minimização (restrição de ambiente d, em [`restricoes.md`](restricoes.md)): o nome do arquivo circula entre sistemas internos e aparece em logs, e não deve carregar dado pessoal.

## 4. Contrato

API HTTP, JSON. Toda resposta de erro inclui um código de motivo, não apenas um status HTTP — porque "o fornecedor não respondeu" e "o arquivo é inválido" pedem tratamento diferente de quem consome.

| Operação | Descrição | Idempotente |
|---|---|---|
| `POST /documentos` | Recebe o arquivo (multipart). Formatos aceitos: `jpg`, `jpeg`, `png`, `pdf`; tamanho máximo 15 MB. Arquivo fora desses critérios é rejeitado com `422` antes de entrar na fila — sem item enfileirado, sem chamada ao fornecedor. Responde com `id` e `status: recebido`. Calcula o hash do arquivo; se já existir um documento com o mesmo hash, responde com o documento existente em vez de criar um novo. | Sim, por hash |
| `GET /documentos/{id}` | Estado atual, campos extraídos, histórico de transições. | — (leitura) |
| `GET /documentos?status=&tipo=` | Lista paginada, filtrável por estado e tipo. | — (leitura) |
| `POST /documentos/{id}/reivindicar` | Marca o documento como em conferência por um identificador de operador, com expiração. Falha com `409` se já estiver reivindicado por outra pessoa e a reivindicação não tiver expirado. | Não |
| `PATCH /documentos/{id}` | Grava a correção de campos feita por um humano; exige reivindicação ativa e do mesmo operador. Move para `concluído`. | Não |
| `POST /documentos/{id}/rejeitar` | Marca o documento como `rejeitado` (estado terminal) quando não é corrigível — arquivo ilegível, tipo errado, dados irrecuperáveis. Exige reivindicação ativa e do mesmo operador (mesma regra do `PATCH`) e um campo `motivo` no corpo (texto curto, obrigatório). Falha com `409` se o documento não estiver `em_conferência` ou a reivindicação não for do operador; `422` se faltar o `motivo`. | Não |

**Erros previstos, não apenas o caminho feliz:** arquivo com formato fora de `jpg`/`jpeg`/`png`/`pdf` ou acima de 15 MB (`422`), recusado antes de entrar na fila — um arquivo que passa nessa validação mas está corrompido não é barrado aqui: segue para processamento e vira `falha_temporária`/`falha_definitiva` ou confiança baixa (não há decodificação de conteúdo na fronteira, ver `restricoes.md` (b)); documento em estado que não permite a operação, por exemplo tentar corrigir um documento ainda `em_processamento` (`409`); reivindicação de item já reivindicado (`409`); rejeição sem `motivo` (`422`); fornecedor de extração indisponível — isto não é erro do chamador, e o documento permanece `em_processamento` para nova tentativa, sem expor falha de infraestrutura interna na resposta da API.

## 5. Módulos e fronteiras

| Módulo | Responsabilidade | O que ele NÃO sabe |
|---|---|---|
| **API** | Validação de entrada, tradução HTTP ↔ domínio, contrato externo | Não sabe como a extração é feita, nem como a fila funciona por dentro |
| **Domínio** | Valida as transições de estado (nenhuma etapa é pulada), mapeia campos por tipo de documento, calcula a confiança do documento (regra do ADR 0005) e gera o nome padronizado do arquivo | Não sabe de HTTP, de fila nem de persistência; é chamado pela API e pelos workers, mas não conhece a mecânica deles |
| **Fila de processamento** | Ordena o trabalho, controla concorrência de workers, timeout, retry; classifica o erro do adaptador como temporário ou definitivo (pelo tipo de erro e pela contagem de tentativas) e aciona o Domínio para registrar a transição final quando o limite de tentativas se esgota | Não sabe o que é um documento de identidade nem como extrair campos |
| **Adaptador de extração** | Fala com o fornecedor de IA (ou com o dublê, nesta entrega); traduz prompt e resposta para um formato interno estável | Não sabe de fila, de HTTP, nem de persistência |
| **Persistência** | Grava e lê estado do documento e histórico de transições | Não sabe de regra de negócio — apenas guarda o que mandam guardar |
| **Fila de conferência** | Reivindicação, expiração, aplicação de correção | Não sabe como o documento chegou ao estado de baixa confiança, apenas que chegou |

**A pergunta que esta seção precisa responder, e responde:** quando o fornecedor de IA trocar de versão, ou os prompts mudarem — o que o enunciado declara como certo, não como hipótese — apenas o **Adaptador de extração** muda. Nenhum outro módulo conhece o nome do fornecedor, o formato do prompt, ou a versão do modelo. Essa fronteira é a peça central do desenho e existe especificamente por causa do fato do ambiente (f).

## 6. Processamento

**Por que assíncrono.** A chamada ao fornecedor de IA leva de 5 a 40 segundos e às vezes falha ou não responde. Processar dentro do ciclo da requisição HTTP significaria segurar a conexão de quem enviou por até 40 segundos e perder o trabalho inteiro a cada falha do fornecedor. `POST /documentos` responde imediatamente com o identificador; o processamento acontece fora do ciclo da requisição, num worker que consome a fila.

**Falha e retry.** Cada chamada ao adaptador tem timeout de 45 segundos. Falha classificada como temporária (timeout, erro 5xx do fornecedor) aciona retry com backoff exponencial de base 5 segundos — esperas de 5s, 10s e 20s entre tentativas — até o limite de 3 tentativas. Esgotado o limite, o documento vai para `falha_definitiva` e fica visível na listagem para intervenção manual — não desaparece silenciosamente. Os três valores (timeout, número de tentativas, base do backoff) ficam em configuração. Documentos em `falha_definitiva` são visíveis via `GET /documentos?status=falha_definitiva`; não há operação dedicada de reprocessamento nesta entrega — a intervenção é manual e fora da API (seção 9).

**Reinício com trabalho em andamento.** Um worker que morre no meio do processamento não pode deixar o documento preso em `em_processamento` para sempre. A reivindicação do worker sobre o item de trabalho tem expiração; passado esse tempo sem conclusão, o item volta a ficar disponível para outro worker pegar. O mesmo padrão de "reivindicação com expiração" usado na fila de conferência humana se repete aqui, na fila de processamento — é o mesmo problema em duas escalas de tempo diferentes.

**Concorrência no volume de pico.** O pico declarado é de mais de 800 documentos concentrado entre 9h e 11h — cerca de 40 vezes a média horária do resto do dia. O número de workers processando em paralelo é limitado explicitamente, e o limite é menor que o limite de taxa do fornecedor de IA, para que o sistema nunca dispare mais chamadas simultâneas do que o fornecedor aceita. Documentos que chegam acima da capacidade de processamento do momento não são recusados — ficam na fila, aguardando, e o cliente que enviou já recebeu confirmação de recebimento no passo 1.

**O dublê de extração.** Nesta entrega o adaptador é um dublê determinístico no lugar do fornecedor real. Ele fixa a confiança dos campos obrigatórios pelo tamanho em bytes do arquivo recebido — abaixo de 500 KB, 0,60; a partir de 500 KB, 0,95 — e a dos campos não-obrigatórios em 0,90. Como a confiança do documento é o mínimo dos obrigatórios (seção 3), arquivo abaixo de 500 KB cai em `aguardando_conferência` e a partir de 500 KB vai para `pronto`. É o que permite percorrer os dois ramos da máquina de estados de ponta a ponta sem o fornecedor real: dois arquivos de tamanhos diferentes exercitam os dois caminhos. A relação entre tamanho e qualidade é aproximação assumida para esta entrega — arquivo menor comprime mais e tende a ter menos detalhe real — não uma medida de qualidade de OCR. Motivo da escolha e alternativas descartadas no ADR 0005.

## 7. Restrições do ambiente

As sete restrições declaradas estão em [`restricoes.md`](restricoes.md), cada uma marcada como tratada ou como risco conhecido aceito, com justificativa. Este documento não as repete — aponta para lá.

Resumo da cobertura, restrição por restrição: **(a)** latência e falha do fornecedor — processamento assíncrono com timeout, retry e backoff (seção 6); **(b)** entrada não confiável — validação de formato e tamanho na fronteira, com `422` antes da fila (seção 4); **(c)** duplicação de envio — idempotência por hash na criação (seção 4); **(d)** dado pessoal — minimização no nome do arquivo (seção 3), com o restante como risco aceito (seção 9); **(e)** volume de pico — número de workers limitado e abaixo do rate limit do fornecedor, excesso enfileirado e não recusado (seção 6); **(f)** troca de versão do modelo e dos prompts — fronteira do adaptador de extração (seção 5); **(g)** conferência concorrente — reivindicação com expiração e `409` na disputa (seção 4). Os riscos residuais de cada uma estão em [`restricoes.md`](restricoes.md).

## 8. Decisões registradas

| ADR | Assunto |
|---|---|
| [0001](adr/0001-avaliacao-de-harness-de-agentes.md) | Configuração do ambiente de trabalho com agentes |
| [0002](adr/0002-persistencia.md) | Persistência: banco relacional versus documento, para o par estado/campos extraídos |
| [0003](adr/0003-fila-de-processamento.md) | Mecanismo de fila: fila de mensagens dedicada versus tabela de jobs no próprio banco |
| 0004 _(a criar)_ | Estratégia de idempotência: hash de conteúdo versus hash de metadados do arquivo |
| [0005](adr/0005-calculo-do-nivel-de-confianca.md) | Cálculo do nível de confiança do documento e comportamento do dublê de extração |
| 0006 _(a criar)_ | Granularidade: monolito modular versus serviços separados para esta entrega |

## 9. O que este projeto conscientemente não resolve

**Classificação automática de tipo de documento.** Fora do recorte da fatia vertical (seção 2). Resolveria adicionando uma etapa de classificação antes da extração, com o próprio adaptador de IA decidindo o tipo antes de aplicar o mapeamento de campos correspondente.

**Deduplicação semântica.** O hash de conteúdo (fato c) detecta o mesmo arquivo enviado duas vezes, mas não duas fotos diferentes do mesmo papel físico. Ficou de fora porque exigiria comparar campos extraídos após o processamento, e não na entrada — uma camada adicional de correspondência que o prazo não comporta. Registrado como risco em `restricoes.md`.

**Autenticação de serviço a serviço.** A API assume um único chamador de confiança nesta entrega. Um ambiente real de produção precisaria de identidade de serviço entre os sistemas internos que consomem o DOC Intelligence.

**Observabilidade e métricas operacionais.** Não há painel, alerta ou métrica de SLA nesta entrega. O que existe é o registro de transição de estado, que permite reconstruir o que aconteceu com qualquer documento específico — mas não agregação ao longo do tempo.

**Reprocessamento de falhas.** Não há operação de API para reprocessar, reenfileirar ou fechar um documento em `falha_definitiva` nesta entrega. Ele fica visível na listagem por status (`GET /documentos?status=falha_definitiva`); agir sobre ele é intervenção manual fora da API. Uma fila de reprocessamento com política de descarte é a evolução natural.

**Retenção e expurgo de dados sensíveis.** O fato (d) — dado pessoal, parte sensível — é registrado em `restricoes.md`. Esta especificação não implementa uma política automática de expurgo após prazo; fica como risco aceito e conhecido, não como lacuna silenciosa.

---

## Registro de crítica

**Rodada 1 do `critico-de-especificacao`:** 2026-08-31 — 15 achados. **Rodada 2:** 2026-09-01 — confirmou os achados corrigidos e levantou N1–N11 (inconsistências introduzidas pelas próprias rodadas de correção, mais pontos menores). **Registro atualizado em:** 2026-09-01.

As correções foram feitas em rodadas; o que segue é o estado atual.

### Os 15 achados

**Bloqueantes**

1. "Nível de confiança" sem regra de cálculo, sem escala e sem valor de limiar.
2. Campos do tipo identidade e formato do nome padronizado nunca definidos.
3. O dublê determinístico não exercita os ramos de erro e de conferência da máquina de estados.
4. A seção 7 apontava para `restricoes.md` como evidência de cobertura, mas o arquivo estava com todos os tratamentos em branco.

**Importantes**

5. Incoerência entre o diagrama da seção 3 (um `GET` levaria o documento a `consultado`) e a seção 4 (`GET` é leitura pura); e `pronto` é ou não estado terminal.
6. Nenhum módulo da seção 5 é dono da máquina de estados nem da lógica de domínio (nome padronizado, mapeamento de campos por tipo).
7. Restrição (d) coberta pela metade: a spec não diz onde os bytes do arquivo original ficam, se são cifrados, com que log de acesso, nem o destino após o processamento.
8. Restrição (a): nenhuma menção a teto de custo por janela de tempo.
9. Sem rota de rejeição na conferência (arquivo ilegível, tipo errado, "não consigo corrigir") e sem operação definida sobre `falha_definitiva`.
10. Números ausentes em bloco: formatos e tamanho de arquivo, timeout, número de tentativas, backoff, expiração das reivindicações, número de workers, paginação.
11. A seção 2 não demarca quais mecanismos da seção 6 são código nesta entrega e quais são apenas desenho.

**Menores**

12. Idempotência por hash sem janela de tempo nem exceção por estado terminal.
13. Quem declara o `tipo` na entrada não está dito; e o filtro por `tipo` numa fatia de um tipo só.
14. A posse da conferência (restrição g) repousa sobre um identificador de operador auto-declarado e não verificado.
15. O operador barrado com `409` não vê quem reivindicou nem quando a reivindicação expira.

### Confirmados na rodada 2, com as ressalvas resolvidas

- **Achado 1** — cálculo de confiança no [ADR 0005](adr/0005-calculo-do-nivel-de-confianca.md), refletido nas seções 2, 3 e 6 (confiança por campo 0,0–1,0; confiança do documento = mínimo dos obrigatórios; limiar 0,85 provisório numa constante única). A ressalva de notação `>` vs `≥` na seção 2 foi corrigida (N7).
- **Achado 2** — campos do tipo identidade tabelados na seção 2; nome `identidade_{id-do-documento}.{extensão-original}` na seção 3, sem dado pessoal, consistente com `restricoes.md` (d). A origem da extensão fica como ponto menor em aberto (N6).
- **Achado 3** — a regra de tamanho de arquivo do ADR 0005 leva o dublê por `pronto` e por `aguardando_conferência → em_conferência → (concluído | rejeitado)`. A rodada 2 confirmou que a spec **não** superdeclara: os ramos `falha_temporária`/`falha_definitiva` estão escritos como não exercitados pelo dublê. A lacuna de operação sobre `falha_definitiva` foi fechada (N3).
- **Achado 4** — as sete restrições de `restricoes.md` têm Tratamento e Risco residual. A rodada 2 apontou overclaim em (b) — corrigido (N5) — e números que não fecham em (e), que ficam no achado 10 / N8.
- **Achado 5** — `pronto`, `concluído`, `falha_definitiva` e `rejeitado` são terminais; nenhuma leitura muda estado. Diagrama, seção 3 e seção 4 concordam. Sem ressalva.
- **Achado 6** — módulo **Domínio** na seção 5. A rodada 2 apontou que ninguém era dono da classificação de falha — atribuída à Fila de processamento (N4).
- **Achado 9** — estado `rejeitado` e `POST /documentos/{id}/rejeitar` na seção 4. A rodada 2 apontou que a seção 2 não mencionava a rota — corrigido (N1).

### Resolvidos na rodada 2 (inconsistências entre partes do documento)

- **N1** — a seção 2 (passo 5) confirma `rejeitado` como resultado possível no escopo desta entrega.
- **N2** — o diagrama da seção 3 ganhou a transição `em_conferência → aguardando_conferência` quando a reivindicação expira sem conclusão (já descrita em prosa na seção 6).
- **N3** — a seção 6 explicita que `falha_definitiva` é visível via `GET /documentos?status=falha_definitiva`, sem operação de reprocessamento nesta entrega (seção 9).
- **N4** — a Fila de processamento (seção 5) passa a classificar o erro como temporário ou definitivo e a acionar o Domínio para registrar a transição final ao esgotar as tentativas.
- **N5** — "corrompido" saiu da lista de motivos de `422` na seção 4; arquivo corrompido que passa em formato/tamanho segue para processamento, coerente com `restricoes.md` (b).
- **N7** — a seção 2 passou a usar `≥ 0,85` / `< 0,85`, a mesma notação do ADR 0005, da seção 3 e do diagrama.
- **N9** — o `README.md` de `docs/adr/` passou a listar o ADR 0005 na tabela.

### Em aberto

- **Achado 7 / restante de (d)** — retenção/expurgo, criptografia em repouso, log de acesso, minimização do que vai ao fornecedor e destino do arquivo original após o processamento: sem tratamento, risco aceito nesta entrega (seção 9). A rodada 2 observou que "onde o arquivo original é guardado" é decisão de desenho, não só política operacional.
- **Achado 8** — sem teto de custo por janela de tempo; risco residual em `restricoes.md` (a). Com dublê (custo zero) e prazo, aceito.
- **Achado 10 / N8** — sem valor testado para: expiração da reivindicação do operador e do worker, número de workers, rate limit do fornecedor, parâmetros de paginação, profundidade da fila. Os números de volume em `restricoes.md` (e) não reconciliam entre si. Ficam como configuração a ajustar em produção.
- **Achado 11** — a seção 2 ainda não demarca, mecanismo a mecanismo da seção 6, o que é código nesta entrega e o que é só desenho (retry/backoff, pool de workers).
- **Achados menores 12, 13, 14, 15** — idempotência sem exceção por estado terminal; quem declara o `tipo` na entrada e o filtro `?tipo=` numa fatia de um tipo só; identificador de operador auto-declarado; `409` sem indicar quem reivindicou nem quando expira. Não endereçados.
- **N6** — a origem da extensão/formato do arquivo (sufixo do nome, `Content-Type` ou sniffing) não está definida; afeta o `422` e o nome proposto. Menor.
- **N10** — a regra de confiança não diz o que acontece se o adaptador não devolver um campo obrigatório (o dublê sempre devolve os três). Menor.
- **N11** — um documento em `pronto` com um campo obrigatório errado (o dublê manda todo arquivo `≥ 500 KB` para `pronto` a 0,95) não tem operação de retorno. Risco aceito nesta fatia.