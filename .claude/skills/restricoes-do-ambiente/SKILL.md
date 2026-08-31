---
name: restricoes-do-ambiente
description: Levanta, registra e cobra as restrições do ambiente onde um sistema vai rodar — latência e falha de dependências externas, entrada não confiável, duplicação, dado sensível, picos de volume, mudanças previstas em fornecedores, concorrência entre usuários. Use ao iniciar um projeto, ao revisar arquitetura ou especificação, ao fechar uma etapa, e sempre que o usuário perguntar o que esqueceu, o que quebra primeiro, ou o que acontece se o volume crescer. Use também para manter o arquivo docs/restricoes.md do projeto atualizado.
---

# Restrições do ambiente

Requisitos dizem o que o sistema deve fazer. Restrições de ambiente dizem como é o lugar onde ele vai rodar — e não pedem funcionalidade nenhuma. É por isso que passam despercebidas, e é por isso que são o que quebra na primeira semana de uso real.

Cada restrição admite duas respostas legítimas: **tratada** ou **risco conhecido e aceito, com justificativa**. As duas contam. O que não conta é silêncio.

## Onde fica o registro

`docs/restricoes.md` na raiz do projeto. Se o arquivo não existe, o primeiro trabalho é criá-lo levantando as restrições com o usuário. Se existe, leia antes de qualquer revisão de arquitetura — ele é a fonte da verdade sobre o que já foi decidido.

## Levantamento

Para descobrir as restrições de um projeto novo, percorra estas famílias. Nem toda família se aplica a todo sistema; a lista é um provocador, não um formulário.

**Dependências externas.** Quanto tempo cada chamada leva, no melhor e no pior caso? Custa dinheiro por chamada? Com que frequência falha? Tem limite de taxa? Vai mudar de versão, e quando?

**Entrada.** Quem produz os dados que entram? Existe validação do outro lado? O que chega malformado, e com que frequência?

**Duplicação e reentrada.** A mesma coisa chega mais de uma vez? O que custa reprocessar? O usuário pode repetir a ação sem perceber?

**Dados sensíveis.** Que categoria de dado circula? O que sai do domínio, e para onde? Que regra jurídica se aplica — LGPD, sigilo profissional, contrato com cliente? Por quanto tempo o dado fica?

**Volume e forma da carga.** Qual a média e qual o pico? O pico é quantas vezes a média? Ele é concentrado em que janela? O que acontece na saturação — enfileira, rejeita, degrada?

**Mudança prevista.** O que já se sabe que vai mudar no primeiro ano? Modelo, fornecedor, regra de negócio, formato de arquivo, equipe?

**Concorrência humana.** Quantas pessoas mexem na mesma coisa ao mesmo tempo? O que a segunda pessoa vê? Existe algo parecido com "pegar" um item?

**Operação.** Quem opera isso quando quebrar? Que visibilidade essa pessoa tem? O que acontece com trabalho em andamento quando o serviço reinicia?

Ao levantar, prefira o número ao adjetivo. "Volume alto" não serve; "150 por dia, pico de 800 concentrado em duas horas" serve, porque permite calcular.

## Formato do registro

Uma entrada por restrição, três linhas cada.

```markdown
### (c) O mesmo documento chega mais de uma vez

**Implica:** reprocessamento duplicado consome cota paga do fornecedor e polui a fila com itens idênticos.
**Tratamento:** hash SHA-256 do arquivo na entrada; item já visto retorna o resultado existente sem nova chamada.
**Risco residual:** foto tirada duas vezes do mesmo papel tem hash diferente e passa. Deduplicação semântica ficou fora do escopo; resolveria comparando os campos extraídos após a classificação.
```

Quando a restrição for aceita como risco em vez de tratada, a terceira linha explica **por que ficou para depois** e **como seria resolvida**. Sem isso, parece esquecimento em vez de escolha.

## Como usar numa revisão

Percorra as restrições registradas na ordem e classifique cada uma: tratada, aceita como risco, ou silenciada pelo desenho atual. Não invente tratamento onde não há — se o desenho não fala da restrição, diga que não fala.

Ao encontrar uma restrição não endereçada, não proponha imediatamente a solução completa. Primeiro mostre a consequência concreta, de preferência com número. Depois deixe a escolha entre resolver e aceitar o risco para o usuário: é a decisão dele que precisa existir e ser defensável, e uma solução pronta rouba isso.

## Sinal de alerta

Se um desenho trata todas as restrições numa entrega curta, provavelmente os tratamentos são superficiais. Duas ou três resolvidas de verdade, com o resto registrado honestamente como risco, é resultado melhor e mais crível do que uma lista inteira de caixas marcadas.

## Ligação com ADR

Restrição tratada por decisão arquitetural gera ADR. A entrada em `docs/restricoes.md` fica curta e aponta para o ADR, em vez de repetir o raciocínio.
