# DOC Intelligence

Serviço de inteligência documental: recebe imagem ou PDF, classifica o tipo, extrai os campos daquele tipo, propõe um nome padronizado e encaminha para conferência humana quando a confiança é baixa. Consumido por sistemas internos do escritório, não por navegador anônimo na internet aberta.

Entrega em três dias corridos. O que está sendo avaliado é o projeto do sistema — arquitetura, decisões e especificação — mais uma fatia vertical implementada. Não o produto completo.

## O que não é para construir

Os cinco comportamentos do produto-alvo, interface polida, deploy, autenticação real, alta cobertura de testes. Nada disso é entregável. Entre implementar mais uma funcionalidade e documentar melhor uma decisão, documentar vence: cinco funcionalidades pela metade valem menos que uma fatia estreita e honesta.

A fatia vertical é um caminho completo de ponta a ponta, ainda que estreito. O modelo de IA pode ser um dublê que devolve sempre a mesma resposta.

## Restrições próprias deste projeto

As sete restrições do ambiente estão em `docs/restricoes.md`. Elas não pedem funcionalidade nenhuma — e é por isso que estão lá. Todo desenho é revisado contra as sete.

Além delas: nenhum dado real de cliente, de pessoa física ou do escritório, em lugar nenhum. Documentos de teste são fictícios e gerados para isso.

## Registro obrigatório

Este projeto exige o registro do uso de IA como parte da entrega: instrução do agente, skills e subagentes versionados, prompts na íntegra e em ordem em `ia/prompts/`, e o parágrafo sobre falhas do agente em `ia/onde-errou.md`. Ver a skill `registro-de-ia`.

---

## Doutrina base

*(o que segue vale para qualquer projeto — copiado de `CLAUDE.base.md`)*

**Especificação antes de código.** Nenhum módulo novo começa sem que a especificação diga o que ele faz e por que existe. Divergência entre implementação e especificação é registrada em `docs/divergencias.md`, com a especificação original preservada.

**Toda decisão vira ADR**, com alternativas consideradas e descartadas. Ver a skill `adr`.

**As restrições do ambiente são revisadas a cada etapa.** Cada uma tratada, ou registrada como risco com justificativa. Silêncio não é resposta.

**O que não foi feito é escrito como não feito.** Nunca esconder lacuna, nunca simular funcionalidade ausente.

**Nada entra sem verificação registrada.** Saída de agente é rascunho até ser conferida.

**Divisão de trabalho:** o raciocínio é meu; o agente formaliza, critica e verifica. Não peço ao agente para decidir — peço opções, lacunas, e boa redação do que já foi decidido. Antes de aceitar um desenho, rodo `critico-de-especificacao`. Antes de fechar uma etapa, rodo `auditor-de-restricoes`.

**Estilo:** português, prosa direta, sem adjetivo de venda. Commits no imperativo, escopo pequeno.
