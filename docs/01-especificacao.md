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
3. Se a confiança está acima do limiar (0,85, provisório — ver ADR 0005), o documento vai para `pronto`, com nome padronizado proposto e campos extraídos. Se está abaixo, vai para `aguardando_conferência`.
4. `GET /documentos/{id}` devolve o estado atual e os campos, prontos ou não. `GET /documentos` lista com filtro por status.
5. Para o documento em conferência, `POST /documentos/{id}/reivindicar` marca que alguém está corrigindo (com expiração), e `PATCH /documentos/{id}` grava a correção e move para `concluído`.

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
                                   └── (não corrigível, com motivo obrigatório) ──► rejeitado  [estado terminal]
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

**Erros previstos, não apenas o caminho feliz:** arquivo com formato fora de `jpg`/`jpeg`/`png`/`pdf`, acima de 15 MB, ou corrompido (`422`), recusado antes de entrar na fila; documento em estado que não permite a operação, por exemplo tentar corrigir um documento ainda `em_processamento` (`409`); reivindicação de item já reivindicado (`409`); rejeição sem `motivo` (`422`); fornecedor de extração indisponível — isto não é erro do chamador, e o documento permanece `em_processamento` para nova tentativa, sem expor falha de infraestrutura interna na resposta da API.

## 5. Módulos e fronteiras

| Módulo | Responsabilidade | O que ele NÃO sabe |
|---|---|---|
| **API** | Validação de entrada, tradução HTTP ↔ domínio, contrato externo | Não sabe como a extração é feita, nem como a fila funciona por dentro |
| **Domínio** | Valida as transições de estado (nenhuma etapa é pulada), mapeia campos por tipo de documento, calcula a confiança do documento (regra do ADR 0005) e gera o nome padronizado do arquivo | Não sabe de HTTP, de fila nem de persistência; é chamado pela API e pelos workers, mas não conhece a mecânica deles |
| **Fila de processamento** | Ordena o trabalho, controla concorrência de workers, timeout, retry | Não sabe o que é um documento de identidade nem como extrair campos |
| **Adaptador de extração** | Fala com o fornecedor de IA (ou com o dublê, nesta entrega); traduz prompt e resposta para um formato interno estável | Não sabe de fila, de HTTP, nem de persistência |
| **Persistência** | Grava e lê estado do documento e histórico de transições | Não sabe de regra de negócio — apenas guarda o que mandam guardar |
| **Fila de conferência** | Reivindicação, expiração, aplicação de correção | Não sabe como o documento chegou ao estado de baixa confiança, apenas que chegou |

**A pergunta que esta seção precisa responder, e responde:** quando o fornecedor de IA trocar de versão, ou os prompts mudarem — o que o enunciado declara como certo, não como hipótese — apenas o **Adaptador de extração** muda. Nenhum outro módulo conhece o nome do fornecedor, o formato do prompt, ou a versão do modelo. Essa fronteira é a peça central do desenho e existe especificamente por causa do fato do ambiente (f).

## 6. Processamento

**Por que assíncrono.** A chamada ao fornecedor de IA leva de 5 a 40 segundos e às vezes falha ou não responde. Processar dentro do ciclo da requisição HTTP significaria segurar a conexão de quem enviou por até 40 segundos e perder o trabalho inteiro a cada falha do fornecedor. `POST /documentos` responde imediatamente com o identificador; o processamento acontece fora do ciclo da requisição, num worker que consome a fila.

**Falha e retry.** Cada chamada ao adaptador tem timeout de 45 segundos. Falha classificada como temporária (timeout, erro 5xx do fornecedor) aciona retry com backoff exponencial de base 5 segundos — esperas de 5s, 10s e 20s entre tentativas — até o limite de 3 tentativas. Esgotado o limite, o documento vai para `falha_definitiva` e fica visível na listagem para intervenção manual — não desaparece silenciosamente. Os três valores (timeout, número de tentativas, base do backoff) ficam em configuração.

**Reinício com trabalho em andamento.** Um worker que morre no meio do processamento não pode deixar o documento preso em `em_processamento` para sempre. A reivindicação do worker sobre o item de trabalho tem expiração; passado esse tempo sem conclusão, o item volta a ficar disponível para outro worker pegar. O mesmo padrão de "reivindicação com expiração" usado na fila de conferência humana se repete aqui, na fila de processamento — é o mesmo problema em duas escalas de tempo diferentes.

**Concorrência no volume de pico.** O pico declarado é de mais de 800 documentos concentrado entre 9h e 11h — cerca de 40 vezes a média horária do resto do dia. O número de workers processando em paralelo é limitado explicitamente, e o limite é menor que o limite de taxa do fornecedor de IA, para que o sistema nunca dispare mais chamadas simultâneas do que o fornecedor aceita. Documentos que chegam acima da capacidade de processamento do momento não são recusados — ficam na fila, aguardando, e o cliente que enviou já recebeu confirmação de recebimento no passo 1.

**O dublê de extração.** Nesta entrega o adaptador é um dublê determinístico no lugar do fornecedor real. Ele fixa a confiança dos campos obrigatórios pelo tamanho em bytes do arquivo recebido — abaixo de 500 KB, 0,60; a partir de 500 KB, 0,95 — e a dos campos não-obrigatórios em 0,90. Como a confiança do documento é o mínimo dos obrigatórios (seção 3), arquivo abaixo de 500 KB cai em `aguardando_conferência` e a partir de 500 KB vai para `pronto`. É o que permite percorrer os dois ramos da máquina de estados de ponta a ponta sem o fornecedor real: dois arquivos de tamanhos diferentes exercitam os dois caminhos. A relação entre tamanho e qualidade é aproximação assumida para esta entrega — arquivo menor comprime mais e tende a ter menos detalhe real — não uma medida de qualidade de OCR. Motivo da escolha e alternativas descartadas no ADR 0005.

## 7. Restrições do ambiente

As sete restrições declaradas estão em [`restricoes.md`](restricoes.md), cada uma marcada como tratada ou como risco conhecido aceito, com justificativa. Este documento não as repete — aponta para lá.

Resumo do que esta especificação já cobre diretamente: a latência e a falha do fornecedor (seção 6, processamento assíncrono com retry); a troca de versão do modelo e dos prompts (seção 5, fronteira do adaptador); a duplicação de envio (seção 4, idempotência por hash na criação); e a conferência concorrente entre duas pessoas do atendimento (seção 4, reivindicação com expiração idêntica ao padrão usado na fila de processamento).

## 8. Decisões registradas

| ADR | Assunto |
|---|---|
| [0001](adr/0001-avaliacao-de-harness-de-agentes.md) | Configuração do ambiente de trabalho com agentes |
| 0002 _(a criar)_ | Persistência: banco relacional versus documento, para o par estado/campos extraídos |
| 0003 _(a criar)_ | Mecanismo de fila: fila de mensagens dedicada versus tabela de jobs no próprio banco |
| 0004 _(a criar)_ | Estratégia de idempotência: hash de conteúdo versus hash de metadados do arquivo |
| [0005](adr/0005-calculo-do-nivel-de-confianca.md) | Cálculo do nível de confiança do documento e comportamento do dublê de extração |
| 0006 _(a criar)_ | Granularidade: monolito modular versus serviços separados para esta entrega |

## 9. O que este projeto conscientemente não resolve

**Classificação automática de tipo de documento.** Fora do recorte da fatia vertical (seção 2). Resolveria adicionando uma etapa de classificação antes da extração, com o próprio adaptador de IA decidindo o tipo antes de aplicar o mapeamento de campos correspondente.

**Deduplicação semântica.** O hash de conteúdo (fato c) detecta o mesmo arquivo enviado duas vezes, mas não duas fotos diferentes do mesmo papel físico. Ficou de fora porque exigiria comparar campos extraídos após o processamento, e não na entrada — uma camada adicional de correspondência que o prazo não comporta. Registrado como risco em `restricoes.md`.

**Autenticação de serviço a serviço.** A API assume um único chamador de confiança nesta entrega. Um ambiente real de produção precisaria de identidade de serviço entre os sistemas internos que consomem o DOC Intelligence.

**Observabilidade e métricas operacionais.** Não há painel, alerta ou métrica de SLA nesta entrega. O que existe é o registro de transição de estado, que permite reconstruir o que aconteceu com qualquer documento específico — mas não agregação ao longo do tempo.

**Retenção e expurgo de dados sensíveis.** O fato (d) — dado pessoal, parte sensível — é registrado em `restricoes.md`. Esta especificação não implementa uma política automática de expurgo após prazo; fica como risco aceito e conhecido, não como lacuna silenciosa.

---

## Registro de crítica

**Rodada do `critico-de-especificacao`:** 2026-08-31. **Registro atualizado em:** 2026-09-01.

O crítico foi rodado sobre este documento e devolveu 15 achados, organizados por severidade. As correções vêm sendo feitas em rodadas; o que segue é o estado atual.

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

### Resolvido até aqui

- **Achado 1** — cálculo de confiança definido no [ADR 0005](adr/0005-calculo-do-nivel-de-confianca.md) e refletido nas seções 2, 3 e 6: confiança por campo (0,0–1,0), confiança do documento = mínimo dos campos obrigatórios, limiar 0,85 provisório numa constante única.
- **Achado 2** — campos obrigatórios e não-obrigatórios do tipo identidade tabelados na seção 2; nome padronizado do arquivo definido na seção 3 (`identidade_{id-do-documento}.{extensão-original}`, sem dado pessoal), com a decisão de minimização registrada em `restricoes.md` (d).
- **Achado 4 (parcial)** — `restricoes.md` (a) e (b) preenchidos com números concretos: formatos `jpg`/`jpeg`/`png`/`pdf` e teto de 15 MB, com `422` na fronteira (b); timeout de 45s, 3 tentativas e backoff 5s/10s/20s (a). Seções 4 e 6 da spec atualizadas com esses números. A restrição (d) recebeu o tratamento de minimização no nome do arquivo; o restante de (d) segue em aberto.
- **Achado 3 (incidental)** — a regra de tamanho de arquivo do ADR 0005 (abaixo de 500 KB → confiança 0,60 → `aguardando_conferência`; a partir de 500 KB → 0,95 → `pronto`) faz o dublê percorrer os dois ramos principais da máquina de estados de ponta a ponta. Isso resolve o achado 3 para os ramos `pronto` e `aguardando_conferência` — **não** para `falha_temporária` e `falha_definitiva`. **A confirmar numa nova rodada do `critico-de-especificacao`.**
- **Achado 5** — `pronto` passa a ser estado terminal, como `concluído` e `falha_definitiva`. A transição `consultado` saiu do diagrama da seção 3 e o texto explicita que nenhuma leitura muda estado — consistente com a seção 4.
- **Achado 6** — a seção 5 ganhou o módulo **Domínio**, dono da validação de transições, do mapeamento de campos por tipo, do cálculo de confiança (ADR 0005) e da geração do nome padronizado. Chamado pela API e pelos workers; não conhece HTTP, fila nem persistência.
- **Achado 9** — novo estado terminal `rejeitado` no diagrama da seção 3, alcançável de `em_conferência` quando o operador marca o documento como não corrigível com `motivo` obrigatório; operação `POST /documentos/{id}/rejeitar` na seção 4, com a mesma regra de reivindicação do `PATCH`. A ação sobre `falha_definitiva` continua sendo intervenção manual pela listagem, sem operação dedicada — fora do escopo desta rodada.

### Em aberto

- **`restricoes.md`:** restrições **(c)**, **(e)**, **(f)** e **(g)** ainda sem tratamento preenchido; e o restante de **(d)** — retenção/expurgo, criptografia em repouso, log de acesso, minimização do que vai ao fornecedor, destino do arquivo após o processamento (mesmo conteúdo do achado 7).
- **Achado importante 11** — não endereçado nesta rodada.
- **Achados menores 12, 13, 14 e 15** — não endereçados.
- **Achado 3** — aguarda confirmação do crítico para os ramos de falha.
- **Achado 8** (teto de custo) — registrado como risco residual em `restricoes.md` (a); não resolvido.
- **Achado 10** — parcialmente resolvido: formatos, tamanho, timeout, tentativas e backoff agora têm número; expiração das reivindicações, número de workers e paginação continuam sem valor.