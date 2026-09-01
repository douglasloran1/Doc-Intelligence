# 0005 — Cálculo do nível de confiança do documento e comportamento do dublê de extração

- **Data:** 2026-09-01
- **Status:** proposta

## Contexto

A especificação (seção 2, passo 3; seção 3) fala em "nível de confiança" do documento e num
"limiar" que decide entre `pronto` e `aguardando_conferência`, mas nunca diz qual é o valor
do limiar, em que escala ele está, nem como se chega a um número único de confiança para o
documento quando a extração devolve vários campos. O `critico-de-especificacao` marcou isso
como achado bloqueante: sem a regra, o passo 3 não é implementável e duas pessoas escrevendo
o código produzem sistemas diferentes.

Fatos que restringem a decisão:

- O adaptador de extração devolve um conjunto de campos. Cada campo vem com uma confiança
  própria no intervalo 0.0–1.0.
- Nesta entrega o adaptador é um dublê determinístico, não o fornecedor real de IA
  (`CLAUDE.md`). O dublê precisa produzir confiança que leve documentos tanto para `pronto`
  quanto para `aguardando_conferência`, senão metade da máquina de estados não é exercida de
  ponta a ponta.
- O tipo em escopo é identidade. Dele o escritório usa, na prática, nome completo, CPF e data
  de nascimento; órgão emissor e data de emissão são acessórios.
- Restrição de ambiente (a): o modelo de terceiro é lento, pago e falível — a confiança é o
  que decide o que vira trabalho humano de conferência e o que passa direto, então a regra
  precisa errar para o lado seguro no que importa.
- Restrição de ambiente (b): a entrada não tem validação do outro lado — chega foto torta,
  girada, comprimida demais. A qualidade da imagem varia muito e não há sinal limpo dela.

## Alternativas consideradas

### Eixo 1 — como agregar a confiança dos campos num número para o documento

#### A. Média aritmética entre todos os campos extraídos

A confiança do documento seria a soma das confianças dividida pelo número de campos.

**Descartada porque:** a média dilui um campo crítico ruim. Um CPF com confiança 0.30 fica
mascarado por quatro campos acessórios a 0.95 e o documento pontua ~0.82 — perto do limiar,
podendo passar direto para `pronto` com o CPF errado. A conta que interessa é o pior campo
que importa, não a tendência central.

#### B. Mínimo entre todos os campos, sem distinção de obrigatoriedade

A confiança do documento seria a menor confiança entre todos os campos, obrigatórios ou não.

**Descartada porque:** manda para conferência humana documentos cujo núcleo (nome, CPF,
nascimento) veio íntegro só porque um campo acessório — órgão emissor, data de emissão —
veio ruim. Esses campos não bloqueiam a proposta de nome padronizado nem o uso principal do
documento. Travar a esteira por causa deles enche a fila de conferência sem retorno
proporcional, o oposto do que a restrição (a) pede.

#### C. Mínimo entre os campos obrigatórios apenas — **escolhida**

Ver "Decisão".

### Eixo 2 — como o dublê de extração desta entrega decide a confiança dos campos

#### D. Derivar do nome do arquivo

O dublê olharia o nome do arquivo enviado e escolheria a confiança a partir dele.

**Descartada porque:** o nome do arquivo é lixo conhecido (restrição b: "WhatsApp Image
....jpeg", "scan0001.pdf"), é controlado por quem envia, e não chega de forma estável nos
testes. Acoplaria o comportamento do sistema a uma string que o cliente define.

#### E. Derivar do hash do conteúdo

Mapear faixas do hash do arquivo para faixas de confiança.

**Descartada porque:** o hash é uniforme por construção; qualquer corte sobre ele é
arbitrário e ilegível — ninguém olha um hash e prevê o resultado. Escrever um teste que
quer de propósito o ramo de baixa confiança viraria tentativa e erro.

#### F. Parâmetro explícito de teste (campo na requisição ou header)

A requisição carregaria um valor dizendo qual confiança o dublê deve devolver.

**Descartada porque:** abre no contrato da API (seção 4) um caminho que só serve a teste e
que precisaria ser barrado em produção. O dublê deve responder ao mesmo input que o
fornecedor real receberia, sem porta dos fundos.

#### G. Alternância sequencial (um documento pronto, o próximo para conferência)

O dublê alternaria o resultado a cada chamada.

**Descartada porque:** o resultado passa a depender da ordem de processamento e de estado
global compartilhado entre os workers — o mesmo arquivo dá resultado diferente conforme quem
chegou antes. Impossível de reproduzir num teste isolado, e esconde problema de concorrência
em vez de expor.

#### H. Configuração fixa por ambiente (tudo pronto, ou tudo para conferência, conforme variável)

Uma variável de ambiente decidiria o ramo para toda a execução.

**Descartada porque:** exercitar os dois ramos na mesma execução — que é o ponto da fatia
vertical — exigiria subir o sistema duas vezes com configuração diferente. Não demonstra a
bifurcação num caminho único de ponta a ponta.

#### I. Derivar do tamanho em bytes do arquivo enviado — **escolhida**

Ver "Decisão".

## Decisão

1. Cada campo extraído carrega confiança própria no intervalo 0.0–1.0, fornecida pelo
   adaptador.
2. Para o tipo identidade, os campos obrigatórios são **nome completo, CPF e data de
   nascimento**; os não-obrigatórios são **órgão emissor e data de emissão**.
3. A confiança do documento é o **mínimo das confianças dos campos obrigatórios**. Os campos
   não-obrigatórios não entram nessa conta, mesmo quando vêm com confiança baixa.
4. O limiar que separa `pronto` (mínimo ≥ limiar) de `aguardando_conferência` (mínimo <
   limiar) é **0,85**. Valor provisório: não foi confirmado com o cliente e precisa de
   calibração com dados reais rotulados. Fica registrado como pendência (ver Consequências).
5. Nesta entrega, o dublê de extração fixa a confiança dos campos obrigatórios pelo tamanho
   em bytes do arquivo enviado, como aproximação simplificada de qualidade de imagem —
   arquivo menor comprime mais, o que tende a indicar menos detalhe real:
   - arquivo com **menos de 500 KB** → confiança dos obrigatórios = **0,60** (abaixo do
     limiar; vai para conferência);
   - arquivo com **500 KB ou mais** → confiança dos obrigatórios = **0,95** (acima do
     limiar; fica pronto);
   - campos não-obrigatórios recebem sempre **0,90**, independente do tamanho, para não
     complicar a lógica do dublê à toa.

   Essa relação entre tamanho e qualidade é aproximação para os fins desta entrega, não uma
   métrica real de qualidade de OCR.

## Consequências

- A proposta de nome padronizado e o uso principal do documento passam a depender só de três
  campos. Se o cliente disser que órgão emissor ou data de emissão são críticos para algum
  uso, a lista de obrigatórios cresce e a regra de agregação muda — este ADR é revisitado.
- 0,85 é um chute informado. Enquanto não houver uma rodada com documentos reais rotulados
  para medir a taxa de falso `pronto` (documento errado entregue como certo) contra o volume
  gerado na fila de conferência, o número não tem base empírica. Mitigação: o limiar vive
  numa única constante de configuração, não espalhado pelo código, para que o ajuste seja de
  uma linha.
- O dublê nunca produz confiança entre 0,60 e 0,95. O comportamento perto do limiar (0,80,
  0,86) não tem como ser testado com o dublê; esse teste fica para quando o adaptador real
  entrar.
- Um campo não-obrigatório com confiança 0,90 fixa nunca dispara conferência sozinho. Um
  documento pode ir para `pronto` com a data de emissão visivelmente errada. Aceito nesta
  entrega.
- O modelo do domínio passa a exigir duas coisas que antes estavam implícitas: a confiança
  registrada por campo (não só o número agregado) e a marcação de quais campos são
  obrigatórios para cada tipo. A especificação (seções 2 e 3) reflete isso.

**Gatilho de revisão:** quando o cliente confirmar ou ajustar o limiar; quando entrar um
segundo tipo de documento (cada tipo traz sua própria lista de campos obrigatórios); e
quando o dublê for substituído pelo adaptador real — nesse momento a regra de tamanho
desaparece, mas a agregação pelo mínimo dos obrigatórios e o limiar permanecem.
