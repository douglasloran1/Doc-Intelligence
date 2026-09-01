# 0001 — Configuração do ambiente de trabalho com agentes

- **Data:** AAAA-MM-DD
- **Status:** proposta

> **Rascunho.** O raciocínio abaixo reflete uma avaliação que precisa ser sua. Confira os fatos, ajuste o que discordar, apague o que não fizer sentido — e reescreva com as suas palavras antes de aceitar. Um ADR que você não consegue defender numa conversa não serve para nada.

## Contexto

O trabalho será feito com agentes de IA, e a configuração usada precisa ser versionada no repositório: arquivos de instrução, skills, subagentes, comandos, hooks ou servidores MCP.

Duas escolhas eram possíveis: adotar um conjunto pronto e maduro, ou montar uma configuração mínima escrita para este problema. O prazo é de três dias corridos.

## Alternativas consideradas

### A. `45ck/skill-harness`

Instalador em Go que sincroniza cerca de trinta repositórios de skills e um conjunto de subagentes por papel (analista de requisitos, modelador de sistema, arquiteto, revisor de qualidade, revisor de segurança), mais tooling próprio de documentação e rastreamento de tarefas. Publica experimentos comparando o toolkit contra o agente sem estrutura, cujo achado principal é que a diferença vem de enforcement de escopo e rastreabilidade, e não de qualidade de código.

**Descartada porque:** exige Go, Node e Python para o setup, num prazo em que o tempo precisa ir para especificação e decisões; a documentação do próprio projeto classifica a instalação como sensível do ponto de vista de cadeia de suprimentos, já que escreve em diretórios de agente do usuário, altera permissões e pode instalar dependências via script remoto; e, principalmente, trinta packs de terceiro no repositório respondem a uma pergunta diferente da que foi feita — a configuração precisa ser a que eu montei.

### B. `SynkraAI/aiox-core`

Framework de orquestração bem mais maduro, com doze agentes especializados, fluxo em duas fases (planejamento agêntico produzindo documentos de produto e arquitetura, depois desenvolvimento com contexto embutido em arquivos de história), engine de execução autônoma e camada comercial. Justifica a arquitetura de duas fases dizendo que ela existe para eliminar inconsistência de planejamento e perda de contexto.

**Descartada porque:** o mesmo motivo central da alternativa A, ampliado pela escala. A instalação escreve uma estrutura inteira no projeto — diretórios de configuração para múltiplas IDEs, scaffolding de artefatos, ferramentas auxiliares — e um repositório coberto por essa estrutura comunica "instalei um framework" onde deveria comunicar "configurei meu ambiente". O fluxo de doze agentes também é grande demais para três dias: agentes configurados e nunca acionados aparecem no registro de prompts como configuração morta.

### C. Nenhuma configuração — usar o agente sem instrução

**Descartada porque:** o registro é entregável, e porque o experimento citado na alternativa A aponta que é exatamente a ausência de estrutura que produz escopo inflado. Trabalhar sem instrução seria abrir mão do controle que precisa ser demonstrado.

## Decisão

Configuração mínima própria: um arquivo de instrução, quatro skills e dois subagentes, escritos para este problema.

As skills cobrem procedimento — formato de ADR com alternativas obrigatórias, levantamento e cobrança das restrições do ambiente, registro do uso de IA, e revisão antes de cada commit. Os subagentes cobrem o que exige contexto isolado: um crítico que ataca a especificação antes de existir código, e um auditor que confere o que o repositório afirma contra o que ele faz.

A divisão entre mecanismo genérico e instância deste projeto é deliberada, e a ligação entre as duas camadas é um único arquivo: `docs/restricoes.md`. As skills e subagentes leem esse arquivo e se adaptam ao projeto sem serem editados.

## Consequências

Perco automação, reuso imediato e o trabalho já validado de terceiros. Cada peça da configuração é simples demais para um projeto de longo prazo com muitos repositórios.

Ganho um repositório em que cada linha de configuração é minha e defensável, e um conjunto que continua servindo depois deste projeto — a camada genérica não depende do domínio.

**Gatilho de revisão:** se este método for usado em mais de três projetos, ou por mais de uma pessoa, a falta de instalador e versionamento centralizado passa a doer. Aí a alternativa A volta a fazer sentido, agora como base a ser adaptada, e não como adoção integral.
