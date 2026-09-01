# Restrições do ambiente — DOC Intelligence

As sete restrições declaradas no enunciado. Nenhuma pede funcionalidade; todas quebram o sistema na primeira semana de uso real se forem ignoradas.

Cada uma está **tratada** ou **registrada como risco conhecido**, com justificativa. Preencher as linhas de tratamento à medida que as decisões forem tomadas — e manter a honestidade: risco assumido conscientemente vale tanto quanto problema resolvido.

---

### (a) O modelo é de terceiro, lento, pago e falível

**Implica:** cada chamada leva de 5 a 40 segundos, é cobrada por documento, e às vezes devolve erro ou não responde. Processar dentro do ciclo da requisição significa segurar conexão por até 40 segundos e perder trabalho a cada falha.
**Tratamento:** processamento assíncrono fora do ciclo da requisição, com o documento passando por estados persistidos (spec §6). Timeout de **45 segundos** por chamada ao adaptador. Falha temporária (timeout, 5xx do fornecedor) aciona retry com backoff exponencial de base 5s — esperas de **5s, 10s e 20s** — até o limite de **3 tentativas**; esgotado, o documento vai para `falha_definitiva` e fica visível na listagem para intervenção manual. Os três valores ficam em configuração. A idempotência por hash (restrição c) evita reprocessar e repagar o mesmo arquivo reenviado. Números concretos na spec §6.
**Risco residual:** não há teto de custo por janela de tempo. A idempotência barra o reenvio idêntico, mas um bug de retry ou um envio em massa legítimo ainda gera custo proporcional ao volume, sem limite superior. Aceito nesta entrega; um teto de chamadas por janela fica como evolução (achado 8 do `critico-de-especificacao`).

---

### (b) A entrada não tem validação nenhuma do outro lado

**Implica:** quem envia é o atendimento, do próprio celular, com a foto original da câmera e o nome que a pessoa deu ao arquivo — "WhatsApp Image ... .jpeg", "scan0001.pdf". Chega arquivo enorme, torto, girado, corrompido, e PDF que na verdade é imagem.
**Tratamento:** validação na fronteira da API, antes de o documento entrar na fila. Formatos aceitos: **jpg, jpeg, png, pdf**. Tamanho máximo: **15 MB**. Arquivo fora desses critérios é rejeitado com `422` e não gera item na fila nem chamada paga ao fornecedor. Ver spec §4.
**Risco residual:** a validação cobre formato declarado e tamanho, não a qualidade do conteúdo. Foto torta, girada, escura ou desfocada dentro dos limites é aceita e segue para extração; nesta entrega a baixa qualidade se manifesta como baixa confiança e cai na fila de conferência (spec §6, ADR 0005). Não há normalização de imagem (rotação, deskew, recompressão) nem verificação de que o "PDF" não é uma imagem renomeada — registrado como evolução.

---

### (c) O mesmo documento chega mais de uma vez

**Implica:** o cliente reenvia por insegurança, o atendimento reenvia por precaução. Sem detecção, cada reenvio é uma chamada paga a mais e um item duplicado na fila de conferência.
**Tratamento:** idempotência por hash do arquivo, descrita na spec §4 (`POST /documentos`). Um documento com hash já visto devolve o registro existente em vez de criar um novo ou reprocessar — sem chamada paga a mais, sem item duplicado na fila de conferência.
**Risco residual:** duas fotos diferentes do mesmo papel físico têm hash diferente e não são detectadas como duplicata. Registrado na spec §9 ("O que este projeto conscientemente não resolve" — deduplicação semântica) como fora do escopo desta entrega.

---

### (d) O conteúdo é dado pessoal, e parte dele é sensível

**Implica:** identidades, contracheques, laudos e procurações saem do domínio do escritório em direção a um fornecedor terceiro. Aplica-se LGPD, e há sigilo profissional envolvido.
**Tratamento (parcial):** minimização no nome do arquivo. O nome padronizado proposto é `identidade_{id-do-documento}.{extensão-original}`, usando apenas o identificador interno gerado pelo sistema. Nenhum dado pessoal — CPF, nome, data de nascimento — entra no nome do arquivo; esses campos ficam só no registro do documento no banco. Decisão consciente: o nome do arquivo circula entre sistemas internos e aparece em logs, e não deve carregar dado pessoal. Ver spec §3.
**Risco residual:** retenção e expurgo após prazo, criptografia em repouso, log de acesso ao conteúdo, minimização do que de fato vai ao fornecedor, e destino do arquivo original após o processamento continuam sem tratamento — risco conhecido e aceito nesta entrega (spec §9). Só a minimização no nome do arquivo foi resolvida nesta rodada.

---

### (e) 150 documentos por dia; em pico, mais de 800 entre 9h e 11h

**Implica:** o pico é cerca de 40 vezes a média horária, concentrado em duas horas. Disparar tudo o que chega estoura o limite de taxa do fornecedor e a memória do processo.
**Tratamento:** spec §6 ("Concorrência no volume de pico"). O número de workers em paralelo é limitado explicitamente e mantido abaixo do limite de taxa do fornecedor, para nunca disparar mais chamadas simultâneas do que ele aceita. O que chega acima da capacidade do momento fica na fila, aguardando — não é recusado —, e o cliente já recebeu a confirmação de recebimento no `POST /documentos` antes disso.
**Risco residual:** não há número concreto para o limite de workers nem para o tamanho máximo da fila. Ficam como configuração a ajustar em produção, sem valor testado nesta entrega.

---

### (f) O modelo trocará de versão, e os prompts mudarão mais de uma vez no primeiro ano

**Implica:** é mudança certa, não hipótese. Se o nome do fornecedor e o texto do prompt estiverem espalhados pelo código, cada troca é uma refatoração.
**Tratamento:** spec §5. A fronteira do módulo **Adaptador de extração** isola todo o conhecimento do fornecedor — nenhum outro módulo conhece o nome do fornecedor, o formato do prompt ou a versão do modelo, e uma troca de versão muda só esse módulo. O registro do documento guarda a versão do adaptador/prompt que gerou cada extração (spec §3).
**Risco residual:** nesta entrega o adaptador é um dublê determinístico, então a fronteira nunca foi exercitada com um fornecedor real — o desenho existe, mas não foi testado sob uma troca de versão de verdade.

---

### (g) Duas pessoas do atendimento podem abrir a fila de conferência ao mesmo tempo

**Implica:** sem mecanismo de posse, duas pessoas corrigem o mesmo documento e uma sobrescreve a outra — ou pior, ambas acham que corrigiram.
**Tratamento:** spec §4 e §6. Reivindicação com expiração, o mesmo padrão usado na fila de processamento. `POST /documentos/{id}/reivindicar` falha com `409` se já houver reivindicação ativa de outra pessoa; `PATCH /documentos/{id}` e `POST /documentos/{id}/rejeitar` exigem reivindicação ativa do mesmo operador. Passado o tempo de expiração sem conclusão, o item volta a ficar disponível.
**Risco residual:** a posse repousa sobre um identificador de operador auto-declarado na requisição, não autenticado nem verificado — achado 14 do relatório do `critico-de-especificacao`, em aberto. Coerente com a premissa de chamador único de confiança desta entrega (spec §2), mas registrado como risco.

---

## Nota de método

Tratar todas as sete numa entrega de três dias provavelmente significa tratamentos superficiais. Duas ou três resolvidas de verdade, com o resto registrado honestamente como risco — com o motivo do adiamento e o caminho de resolução — é resultado melhor e mais crível.

Restrição tratada por decisão arquitetural deve ter ADR correspondente em `docs/adr/`. A entrada aqui fica curta e aponta para o ADR.
