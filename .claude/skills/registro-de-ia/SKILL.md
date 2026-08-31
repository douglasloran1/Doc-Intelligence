---
name: registro-de-ia
description: Mantém o registro obrigatório do uso de IA — prompts na íntegra e em ordem, o que foi verificado antes de aceitar cada saída, e onde o agente errou. Use ao final de cada sessão de trabalho, sempre que uma saída do agente for aceita ou rejeitada, sempre que um erro do agente for descoberto, e quando o usuário perguntar como está o registro ou pedir para preparar o parágrafo sobre falhas do agente. Use também antes de qualquer commit que inclua trabalho feito com agente.
---

# Registro de uso de IA

Quando um projeto exige (ou você decide manter) o registro do uso de IA, ele tem quatro partes: arquivos de instrução, skills e subagentes versionados, os prompts na íntegra e em ordem, e um parágrafo sobre onde o agente errou, como isso foi percebido e o que foi feito.

O valor está em ser verdadeiro. Um registro polido depois parece polido depois — repetição de estilo, ausência de becos sem saída, nenhum erro. Registro real tem prompt mal formulado, correção no meio do caminho e ideia abandonada.

## Prompts

Um arquivo por sessão de trabalho em `ia/prompts/`, nomeado `NNN-assunto-curto.md`.

```markdown
# 003 — modelagem da fila de conferência

**Sessão:** 2026-08-27, 14h–16h
**Objetivo:** decidir como duas pessoas trabalham a fila sem colidir.

---

## Prompt 1
[texto exato como foi enviado]

**Retorno em uma linha:** propôs lock pessimista no banco.
**Verifiquei:** se o lock expira quando a aba é fechada. Não expirava.
**Ação:** rejeitado; pedi alternativa com claim expirável.

---

## Prompt 2
[texto exato]

...
```

Regras:

**Copiar o prompt como foi escrito.** Com o erro de digitação, com a pergunta mal formulada, com o "não, esquece, faz assim". Não corrigir, não resumir, não traduzir.

**Ordem cronológica, sem lacuna.** Se uma tentativa foi abandonada, ela entra assim mesmo, marcada como abandonada. Os becos sem saída são a parte mais informativa do registro.

**Uma linha de retorno, não a resposta inteira.** O registro é do que foi pedido e do que foi feito com o resultado, não um dump da conversa.

**Registrar no momento.** No terceiro dia é impossível reconstruir com honestidade. Se a sessão terminou sem registro, registre antes de qualquer outra coisa na sessão seguinte, e anote que foi reconstruída.

## Verificação

Toda saída aceita precisa de uma linha dizendo o que foi conferido. Não "revisei" — o que especificamente foi olhado.

Exemplos do que conta como verificação: rodou o código e viu o resultado; conferiu se a biblioteca citada existe e tem a função na versão usada; testou o caminho de erro, não só o feliz; comparou o número afirmado com o enunciado; confirmou que o desenho proposto realmente trata o fato do ambiente que ele diz tratar.

## Erros do agente

Manter `ia/onde-errou.md`, atualizado no momento em que o erro aparece.

```markdown
## Alucinação de API — 2026-08-27

**O que houve:** sugeriu um método de expiração de lock que não existe na versão que estou usando.
**Como percebi:** rodou e quebrou; conferi na documentação e o método é de outra biblioteca.
**O que fiz:** troquei por claim com coluna de timestamp e verificação na leitura. Passei a pedir referência de documentação para toda chamada de API que eu não conhecia.
```

O padrão importa mais que o incidente isolado. Ao fechar o projeto, olhe os erros acumulados e pergunte: eles se parecem? Se sim, o parágrafo final da entrega descreve o padrão e a mudança de método que ele provocou — isso é muito mais forte que um erro anedótico.

## Ausência de uso também se registra

Se em alguma etapa o agente não foi usado, e isso foi uma escolha, registre a escolha e o motivo. Faz parte da mesma prestação de contas.

## Antes do commit

Verifique: os prompts da sessão estão em `ia/prompts/`? Toda saída aceita tem linha de verificação? Algum erro descoberto ficou fora de `ia/onde-errou.md`? Se algo estiver faltando, complete antes de commitar — o histórico de commits é lido, e um registro que aparece todo de uma vez no último commit conta a história errada.
