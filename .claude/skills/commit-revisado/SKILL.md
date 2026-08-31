---
name: commit-revisado
description: Conduz o versionamento no Git com aprovação humana obrigatória antes de cada commit e de cada push. Use sempre que houver trabalho pronto para versionar, quando o usuário disser "commita", "sobe isso", "manda pro GitHub", "salva no repositório", e proativamente ao fim de cada etapa de trabalho. Use também para decidir o que entra e o que fica de fora do repositório, para escrever mensagens de commit, e antes de qualquer operação que altere o histórico.
---

# Commit revisado

Todo trabalho pertinente vai para o repositório. Nada vai sem o usuário ver antes.

O histórico de commits é evidência: mostra em que ordem o trabalho aconteceu, o que foi decidido antes de programar, e se a pessoa realmente conduziu o processo. Um histórico bom é conteúdo entregue, não subproduto.

## A regra que não se quebra

**Nunca execute `git commit`, `git push`, `git merge`, `git rebase`, `git reset` ou qualquer coisa que reescreva histórico sem aprovação explícita do usuário no turno atual.**

`git status`, `git diff`, `git log` e `git add` de arquivos específicos são livres — são leitura e preparação. A escrita no histórico precisa de autorização.

"Commita tudo" não é dispensa da revisão. É autorização para *preparar* e mostrar. Mostre e espere.

Aprovação vale para o que foi mostrado. Se algo mudou depois da aprovação, mostre de novo.

## Fluxo

### 1. Verificação prévia (antes de mostrar qualquer coisa)

Rode estas checagens e resolva o que aparecer **antes** de apresentar a proposta. Se algo grave aparecer, apresente o achado em vez da proposta.

**Dado pessoal real** — a checagem de maior prioridade. Procure em tudo que entraria no commit: CPF, RG, CNPJ, nomes completos de pessoas reais, endereços, telefones, e-mails pessoais, fotos ou scans de documentos. Inclui fixtures, testes, exemplos de prompt e logs. Se achar, **pare e avise** — não proponha o commit.

**Segredo** — chave de API, token, senha, string de conexão, arquivo `.env`, credencial em arquivo de configuração. Mesmo tratamento: pare e avise.

**Lixo** — `node_modules`, `__pycache__`, `.venv`, artefatos de build, arquivo temporário, saída gerada que deveria ser reproduzível. Não entra; sugira `.gitignore` se ainda não estiver coberto.

**Arquivo grande ou binário** — acima de alguns megabytes, ou binário que não é fonte. Confirme com o usuário se realmente deve entrar.

### 2. Proposta

Mostre, nesta ordem:

```
## Commit proposto

**Mensagem:**
docs: registra as sete restrições do ambiente

**Arquivos** (3 adicionados, 1 modificado):
+ docs/restricoes.md           78 linhas
+ docs/adr/0002-fila.md        41 linhas
+ ia/prompts/003-fila.md       55 linhas
~ CLAUDE.md                    +4 −1

**O que muda:** uma frase sobre o efeito real da mudança.

**Deixado de fora:** rascunho.md (trabalho em andamento), .env (segredo)

**Verificações:** sem dado pessoal, sem segredo, sem arquivo gerado.
```

Para mudanças pequenas ou textuais, mostre o diff inteiro. Para mudanças grandes, mostre o resumo por arquivo e ofereça o diff completo de qualquer um que o usuário queira ver. Nunca esconda um arquivo da lista por ser "pequeno" ou "óbvio".

Sempre liste o que ficou de fora e por quê. É a parte que o usuário não consegue conferir sozinho.

### 3. Aprovação

Espere resposta explícita. "Pode ir", "aprovado", "manda" contam. Silêncio, mudança de assunto ou uma pergunta não contam.

Se o usuário aprovar parcialmente ("commita só os docs"), refaça a proposta com o subconjunto e mostre de novo — o commit resultante é diferente do que foi apresentado.

Se pedir mudança na mensagem ou no recorte, ajuste e reapresente. Não commite a versão ajustada assumindo que a aprovação anterior cobre.

### 4. Execução

`git add` dos arquivos específicos — nunca `git add -A` ou `git add .`, que é como lixo e segredo entram. Commit com a mensagem aprovada. Depois mostre o hash e o `git log --oneline -3` para o usuário ver onde ficou.

### 5. Push é um segundo portão

`git push` nunca vai junto no embalo do commit. Pergunte separadamente. Commit local é reversível; push, na prática, não.

Ao propor push, diga quantos commits vão e para qual branch.

## Mensagens

Prefixo por tipo (`docs:`, `feat:`, `fix:`, `refactor:`, `test:`, `chore:`), imperativo, minúscula, sem ponto final. Assunto em até 72 caracteres.

Corpo só quando o porquê não é óbvio pelo assunto. Quando houver, explique a razão, não o conteúdo — o diff já mostra o conteúdo.

```
feat: processa documento fora do ciclo da requisição

A chamada ao fornecedor leva de 5 a 40s. Manter isso síncrono
seguraria a conexão e perderia o trabalho a cada falha.
Ver docs/adr/0002-fila.md.
```

Referencie o ADR quando o commit implementa uma decisão registrada. Isso costura o histórico à documentação e é exatamente o tipo de rastro que se procura ao ler um repositório.

## Granularidade

Um commit por mudança lógica. Se a mensagem precisa de "e", provavelmente são dois commits.

Não acumule o dia inteiro num commit só. O histórico é a narrativa do trabalho: commits ao longo do dia contam que houve processo; um commit gigante às 23h conta outra coisa.

Não commite trabalho quebrado sem dizer. Se for necessário salvar um ponto intermediário, diga na mensagem que está em andamento.

## Ordem que conta a história

Quando o projeto valoriza o método — e quase todos valorizam mais do que dizem — a sequência dos commits é argumento por si só:

1. estrutura mínima e instrução do agente
2. especificação
3. restrições do ambiente
4. ADRs
5. implementação
6. testes
7. registro de IA e divergências

Um `git log` que mostra especificação e ADR antes do primeiro arquivo de código prova, sem precisar afirmar, que o projeto veio antes da programação. Não force a ordem se o trabalho não aconteceu assim — histórico honesto vale mais que histórico bonito. Mas quando puder escolher a ordem de commitar, escolha essa.

## Histórico não se reescreve

Nada de `--amend` em commit já enviado, nada de squash do histórico, nada de force push. Erro no histórico se corrige com um commit novo.

A única exceção é remoção de segredo ou dado pessoal que vazou — e aí o usuário decide, sabendo que o histórico será reescrito e por quê.

## Quando propor sem ser pedido

Ao fim de uma etapa de trabalho: um documento fechado, um módulo funcionando, uma decisão registrada. Também quando o volume de mudanças não commitadas começar a ficar grande o bastante para virar um commit confuso.

Proponha uma vez. Se o usuário não quiser agora, siga o trabalho e não insista.
