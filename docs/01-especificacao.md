# Especificação — DOC Intelligence

> Escrita antes do código. Se a implementação divergir, este documento permanece como está e a divergência é registrada em [`divergencias.md`](divergencias.md).

**Data da primeira versão:** 2026-08-31
**Trilha:** A · back-end 

---

## 1. Problema

Hoje uma pessoa abre cada arquivo recebido, descobre o que é, renomeia num padrão interno e digita os dados numa planilha. São quatro minutos por documento, e o volume cresce.

_(Reescreva com suas palavras o que você entendeu do problema. Uma pessoa que não leu o enunciado precisa entender daqui.)_

## 2. Produto-alvo versus escopo desta entrega

O produto-alvo tem cinco comportamentos: receber documento; classificar e extrair campos e propor nome; consultar e listar processados; segurar para conferência humana quando a confiança for baixa; ser consumido por sistemas internos.

**Esta entrega não é o produto-alvo.** É o projeto do sistema mais uma fatia vertical.

### O recorte

**A fatia implementada é:** _(descreva o caminho de ponta a ponta, do início ao fim)_

**Está fora, por escolha:** _(liste, com o motivo de cada um)_

**Por que este recorte e não outro:** _(o raciocínio. Esta é a parte que mais conta.)_

## 3. Modelo do domínio

_(As entidades e seus estados. Um documento nasce em que estado? Por quais estados passa? Quando é terminal? O que acontece quando falha no meio?)_

```
[diagrama de estados, mesmo em texto]
```

## 4. Contrato

_(Trilha A: a API exposta. Trilha B: o contrato que você definiu e serve por mock — ele faz parte da entrega.)_

Para cada operação: o que recebe, o que devolve, o que acontece em erro, e o que é idempotente.

## 5. Módulos e fronteiras

_(Quais são os módulos, o que cada um pode saber sobre os outros, e onde estão as fronteiras que protegem contra mudança.)_

Pergunta a responder explicitamente: **quando o fornecedor de IA trocar de versão, quantos arquivos mudam?**

## 6. Processamento

_(Como um documento atravessa o sistema. Síncrono ou não. O que acontece na falha, no timeout, no reinício do serviço com trabalho em andamento.)_

## 7. Restrições do ambiente

As sete restrições estão em [`restricoes.md`](restricoes.md), cada uma tratada ou registrada como risco conhecido. Este documento não as repete; aponta para lá.

## 8. Decisões registradas

_(Lista com link para cada ADR em `adr/`.)_

## 9. O que este projeto conscientemente não resolve

_(Escrito como não feito, não escondido. Cada item com o motivo e, quando aplicável, o caminho de resolução.)_

---

## Registro de crítica

Data em que o `critico-de-especificacao` foi rodado sobre esta especificação, e o que mudou por causa dos achados. Se algum achado foi conscientemente não endereçado, registre qual e por quê.
