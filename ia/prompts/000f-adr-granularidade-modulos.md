# 000f — ADR 0006 (granularidade dos módulos)

**Sessão:** 2026-09-01 (reconstruída)
**Objetivo:** decidir a organização de pacotes antes do código, depois de uma pergunta sobre a viabilidade de Clean Architecture / Arquitetura Hexagonal nesta entrega.

> **Nota de reconstrução.** Este registro foi reconstruído em 2026-09-01 a partir do histórico de uma conversa com o assistente Claude, onde esta sessão de trabalho foi originalmente conduzida — antes de o registro formal em `ia/prompts/` começar a ser usado, a partir da sessão de implementação de código (`001`). Para ficar antes do `001` sem renumerar registros já versionados, os arquivos reconstruídos usam a numeração `000a`–`000f`. O conteúdo abaixo — decisões, alternativas discutidas, correções — reflete fielmente o que aconteceu; apenas o formato de prompt individual e os timestamps exatos foram reconstituídos a partir da conversa, não registrados em tempo real.

---

## Decisões e pontos-chave

1. **Organização por funcionalidade.** Um pacote `documento/` reúne a entidade, o repositório, o serviço, o controller, os DTOs e o tratamento de erro daquela funcionalidade. A separação de responsabilidade acontece por classe e por estereótipo do Spring (`@RestController`, `@Service`, `@Repository`), não por pacotes de camada nem por portas explícitas. A entidade JPA é a própria entidade de domínio — não há classe de domínio separada.
2. **Descartada — Clean Architecture / Hexagonal (portas e adaptadores):** cada entidade dobraria (uma pura de domínio, uma de persistência) com mapeamento entre as duas; toda fronteira exigiria uma interface e uma implementação; o número de arquivos por funcionalidade cresceria três a quatro vezes. O ganho — domínio testável isolado do framework, troca fácil de infraestrutura — só se paga com muitas equipes, muitos módulos, ou troca real de infraestrutura prevista. Aqui há um projeto, um desenvolvedor, três dias, e o único ponto de troca que o enunciado declara (o fornecedor de IA) já está isolado pela fronteira do Adaptador de extração na §5.
3. **Descartada — monolito sem separação alguma:** o problema que ela evitaria — regra de negócio misturada com HTTP e com persistência — já está resolvido pelo módulo Domínio da §5 e pela separação Controller/Service/Repository, mesmo sem pacotes de camada.
4. **Consequências aceitas:** a regra de negócio carrega anotações de JPA (a entidade é a de persistência); testar o domínio sem subir JPA fica limitado a POJO + repositórios mockados; trocar o banco ou o mecanismo de persistência exigiria mexer nas classes de domínio, porque não há porta que as proteja. **Gatilho de revisão:** se o número de tipos de documento ou de desenvolvedores crescer a ponto de várias pessoas mexerem nas mesmas classes, reabrir a comparação com portas e adaptadores, começando pela fronteira de persistência.
5. **Nota de sequência.** A discussão (a pergunta sobre Clean Architecture) e a decisão aconteceram nesta sessão, antes do código. O arquivo `docs/adr/0006-granularidade-dos-modulos.md` em si foi redigido e commitado no início da sessão de implementação (`001`, Prompt 2, commit `ff5cd03`), quando a linha 0006 ainda estava marcada "(a criar)" na tabela da §8 — ver o registro `001` para o texto do prompt que formalizou o ADR.

## Resultado (efetivado na sessão `001`)

`docs/adr/0006-granularidade-dos-modulos.md` (Status: aceita), no formato da skill `adr`, com as duas alternativas descartadas e o gatilho de revisão. Tabela da §8 de `docs/01-especificacao.md` (sem o "(a criar)") e `docs/adr/README.md` atualizadas.
