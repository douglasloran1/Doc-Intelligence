# DOC Intelligence

Serviço de inteligência documental: recebe imagem ou PDF, classifica o tipo, extrai os campos daquele tipo, propõe um nome padronizado e encaminha para conferência humana quando a confiança é baixa. Consumido por sistemas internos, não por navegador aberto.

Entrega para o processo de seleção — trilha **[A · back-end / B · front-end — declarar aqui]**.

## Onde está cada coisa

| O que | Onde |
|---|---|
| Especificação, escrita antes do código | [`docs/01-especificacao.md`](docs/01-especificacao.md) |
| Restrições do ambiente e como cada uma foi tratada | [`docs/restricoes.md`](docs/restricoes.md) |
| Decisões de arquitetura, com alternativas descartadas | [`docs/adr/`](docs/adr/) |
| Onde a implementação divergiu da especificação | [`docs/divergencias.md`](docs/divergencias.md) |
| Instrução do agente | [`CLAUDE.md`](CLAUDE.md) |
| Skills e subagentes configurados | [`.claude/`](.claude/) |
| Prompts, na íntegra e em ordem | [`ia/prompts/`](ia/prompts/) |
| Onde o agente errou e o que foi feito | [`ia/onde-errou.md`](ia/onde-errou.md) |
| Carta de fechamento | [`carta/`](carta/) |

## Como subir o projeto

_(preencher: pré-requisitos, instalação, variáveis de ambiente, comando para rodar, comando para testar — de forma que outra pessoa consiga subir sem perguntar nada)_

```bash
# exemplo
```

## O que está pronto e o que não está

**Implementado:** a fatia vertical — _(descrever o caminho de ponta a ponta que funciona)_.

**Não implementado, por escolha:** _(listar. O que não foi feito está escrito como não feito, aqui e em `docs/restricoes.md`.)_

## Testes

_(um parágrafo: o que foi escolhido testar e por quê. O critério de escolha importa mais que a quantidade.)_

## Sobre a configuração de IA

A pasta `.claude/` contém quatro skills e dois subagentes escritos para este projeto. O princípio que os organiza: o raciocínio é do autor, o agente formaliza, critica e verifica. O crítico faz perguntas em vez de propor soluções; a skill de ADR pergunta pelas alternativas em vez de inventá-las; o auditor confere sem corrigir.

O mecanismo é genérico e a instância é deste projeto — a ligação entre as duas camadas é `docs/restricoes.md`, lido pelas skills e pelos subagentes sem que eles precisem ser editados.

## Dados

Nenhum dado real de cliente, de pessoa física ou do escritório existe neste repositório. Os documentos usados em teste são fictícios e foram gerados para isso.
