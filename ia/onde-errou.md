# Onde o agente errou

Registrado no momento em que o erro aparece, não reconstruído depois.

Formato de cada entrada: o que houve, como percebi, o que fiz.

## Decisão de implementação fechou um achado do crítico sem sinalizar — 2026-09-01

**O que houve:** ao implementar o `POST /documentos`, o agente decidiu que o formato do arquivo vem do sufixo do nome no multipart. Essa decisão resolve o achado N6 do `critico-de-especificacao`, que estava listado explicitamente em "Em aberto" no bloco Registro de crítica da especificação. O agente apresentou a decisão como um detalhe de implementação — um item numa lista de sete desvios para revisão —, sem apontar que ela fechava um achado rastreado que exigia frase na especificação (§4) e movimentação no Registro de crítica.
**Como percebi:** o usuário, ao revisar os sete desvios, identificou que o quarto correspondia ao N6 e pediu que a resolução ficasse registrada, "não solta".
**O que fiz:** adicionei a frase à §4, movi o N6 para uma subseção "Resolvido na implementação" no Registro de crítica, e registrei este erro. Mudança de método: quando uma decisão de implementação cai sobre um ponto que a especificação deixou em aberto de propósito (achado do crítico, item marcado como a decidir), tratar como alteração de especificação — sinalizar explicitamente e propor a atualização do documento —, não como nota de rodapé da entrega de código.
