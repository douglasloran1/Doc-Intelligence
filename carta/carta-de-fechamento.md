# Carta de fechamento — teste técnico

Agradeço pela oportunidade de realizar este teste técnico. A seguir, respondo às quatro perguntas da carta de fechamento, na ordem em que foram feitas.

**1. O que ficou de fora e por quê?**

O produto-alvo tem cinco comportamentos; eu entreguei uma fatia vertical para um único tipo de documento, identidade, mais o projeto do sistema inteiro. Deixei de fora, por escolha: a classificação automática entre os sete tipos de documento, autenticação real entre serviços, interface gráfica, deploy e infraestrutura de produção, migração de schema versionada (uso ddl-auto do Hibernate), retenção e expurgo de dado sensível após o processamento, e um teto explícito de custo por janela de tempo. Também deixei sem teste de integração contra um banco real a parte mais delicada do sistema: a fila de jobs com FOR UPDATE SKIP LOCKED, que hoje só é validada por teste de unidade com repositório simulado.

O critério que usei para separar o que fazer do que registrar como risco foi simples: priorizei o que demonstra arquitetura e decisão sob restrição de tempo sobre o que apenas amplia a superfície do produto. Estender a extração para mais tipos de documento, por exemplo, seria uma mudança de dado — um novo mapeamento de campos — não uma mudança de desenho; processar dois tipos em vez de um não provaria nada além do que a fatia já prova com um tipo só. Já a fronteira do adaptador de extração, a máquina de estados completa com seus quatro estados terminais, e a fila com controle de concorrência — isso sim ficou de pé, porque é onde a decisão de arquitetura realmente aparece. Nada do que ficou de fora está silenciado: cada item está registrado em docs/restricoes.md ou na seção 9 da especificação, com o motivo e, quando cabia, o caminho de resolução.

**2. O que quebra primeiro se o volume for multiplicado por dez?**

A fila de processamento. Ela vive numa tabela do próprio PostgreSQL (job_processamento), consultada por polling a cada dois segundos, com reivindicação de item via FOR UPDATE SKIP LOCKED. No volume atual isso funciona bem e evita trazer uma peça de infraestrutura extra para um prazo de três dias. Multiplicado por dez — de 800 para 8 mil documentos concentrados em duas horas — três coisas cedem juntas: contenção na mesma linha da tabela quando vários workers disputam o próximo job, a tabela crescendo sem nenhuma rotina de expurgo dos itens concluídos, e a latência do polling deixando de ser irrelevante.

Esse ponto pesa mais porque é justamente o trecho que não testei contra um banco real. Os testes de unidade cobrem a lógica de decisão — quando um job deve ser reivindicado, quando uma tentativa esgota e vira falha definitiva — mas simulam o repositório; a query de concorrência em si nunca rodou sob carga concorrente de verdade. O ADR que registra essa escolha já aponta o gatilho de revisão: sob contenção visível ou latência inaceitável, a fronteira do módulo de fila existe exatamente para permitir trocar por um broker dedicado sem tocar no resto do sistema.

**3. Qual decisão eu menos defenderia hoje?**

Não escrever o teste de integração contra Postgres real para a fila. Sabia, desde que desenhei o mecanismo, que FOR UPDATE SKIP LOCKED e a expiração de reivindicação por lease são exatamente o tipo de lógica que só se prova sob concorrência real — um teste de unidade com mock confirma que a decisão está certa no papel, não que a query se comporta como esperado quando dois workers disputam a mesma linha ao mesmo tempo. Documentei o risco de forma explícita, em mais de um lugar, o que evita que ele passe despercebido. Mas documentar um risco não é o mesmo que eliminá-lo, e essa era a parte do sistema que mais merecia esse teste, não a que menos merecia.

**4. Quanto tempo isso levou?**

No total, nove horas: quatro na especificação e nas decisões de arquitetura, duas na implementação da fatia vertical, e três na revisão — os dois ciclos de crítica contra a especificação, a leitura e o aceite dos ADRs, o teste manual do fluxo completo e o fechamento da entrega. A maior parte do tempo ficou na especificação e na revisão, não no código: rodei dois ciclos completos de revisão crítica antes de escrever a primeira linha de implementação — o primeiro levantou quinze pontos; o segundo, já depois das correções, confirmou o que tinha sido resolvido e encontrou mais alguns pontos de inconsistência entre partes do documento — o tipo de coisa que só aparece quando alguém lê tudo de novo com atenção fria. A fatia vertical em código veio depois, em blocos pequenos e revisados um a um: recebimento e validação, fila e processamento assíncrono, consulta e conferência humana.

Atenciosamente,

Douglas Loran Oliveira Freitas
(84) 98783-1922
douglaslorran453@gmail.com
