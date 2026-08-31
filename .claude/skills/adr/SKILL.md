---
name: adr
description: Registra decisões de arquitetura em formato ADR, com alternativas consideradas e descartadas. Use sempre que uma decisão técnica for tomada ou discutida — escolha de banco, linguagem, framework, estratégia de processamento, granularidade de módulo, fronteira com serviço externo, formato de contrato — e também quando o usuário disser "decidi X", "vou usar Y", "acho melhor Z", mesmo sem pedir um ADR explicitamente. Use igualmente quando uma decisão for revista ou revertida.
---

# ADR — registro de decisão de arquitetura

Um ADR documenta uma decisão e o raciocínio que levou a ela. O valor não está na decisão: está nas alternativas descartadas e no motivo do descarte. Quem lê quer saber o que foi considerado e rejeitado, porque é isso que mostra que houve escolha, e não inércia.

## Antes de escrever

Se o usuário deu a decisão mas não as alternativas, **pergunte**. Um ADR com uma única opção não é um ADR, é um comunicado. Duas perguntas resolvem: o que mais foi cogitado, e o que fez cada um perder.

Se o usuário não cogitou nada, ofereça duas ou três alternativas plausíveis para ele avaliar — mas deixe claro que a escolha e o motivo são dele. Não decida em nome dele: quem não formulou o motivo não consegue defender a decisão depois.

## Formato

Arquivo em `docs/adr/NNNN-titulo-curto.md`, numeração sequencial de quatro dígitos, título em substantivo (o assunto, não a conclusão).

```markdown
# NNNN — [assunto da decisão]

- **Data:** AAAA-MM-DD
- **Status:** proposta | aceita | substituída por NNNN

## Contexto

O problema concreto, com os números e restrições que importam.
Cite a restrição de ambiente relacionada quando houver.

## Alternativas consideradas

### A. [nome]
Como funcionaria. O que ganha. O que custa.
**Descartada porque:** ...

### B. [nome]
...
**Descartada porque:** ...

## Decisão

O que foi escolhido, em uma ou duas frases.

## Consequências

O que passa a ser verdade por causa disso — inclusive o que fica pior.
O que precisaria mudar para esta decisão deixar de valer.
```

## Regras de conteúdo

**Pelo menos duas alternativas, sempre.** "Não considerei alternativa" é uma resposta aceitável apenas se estiver escrito assim, com o motivo (prazo, familiaridade, restrição externa). Escrever isso é melhor que inventar comparação que não houve.

**Números em vez de adjetivos.** Não "o volume é alto"; sim o número. Não "a API é lenta"; sim a faixa de latência medida ou informada. Se o número não existe, diga que não existe — isso também é informação.

**Consequência negativa obrigatória.** Toda decisão custa alguma coisa. Um ADR só com vantagens não foi pensado. Se não houver custo aparente, o problema é que a alternativa não foi levada a sério.

**Reversibilidade.** Diga o que precisaria acontecer para revisitar a decisão. Isso transforma o ADR em instrumento, não em memorial.

**Sem reescrita retroativa.** ADR aceito não é editado quando a implementação diverge. Cria-se um novo ADR com status "substitui NNNN", ou registra-se a divergência num documento próprio de divergências.

## Cobertura mínima

Ao fim da fase de projeto, verifique se existe ADR para cada um destes. O que não tiver, provavelmente foi decidido no automático:

- linguagem, framework e persistência — e por que não as outras opções
- síncrono versus assíncrono nos pontos de entrada
- fronteira com cada serviço externo, e o custo de trocá-lo
- estratégia de idempotência e reprocessamento
- onde vivem os dados sensíveis e o que sai do domínio
- granularidade dos módulos
- o recorte do escopo: o que entrou e o que ficou de fora

Se o projeto tem um arquivo de restrições de ambiente (ver a skill `restricoes-do-ambiente`), toda restrição tratada por decisão arquitetural deve ter ADR correspondente.

## Exemplo curto

**Entrada do usuário:** "Vou de Postgres. Pensei em Mongo mas os campos variam por registro e as consultas principais são relacionais."

**Saída esperada:** ADR com contexto citando a variação de campos e a natureza das consultas; alternativa A Mongo descartada por perda de integridade referencial; alternativa B Postgres com JSONB escolhida; consequência registrando que JSONB não valida esquema e que isso empurra a validação para a aplicação; gatilho de revisão se a variação de formato crescer além de um limite.
