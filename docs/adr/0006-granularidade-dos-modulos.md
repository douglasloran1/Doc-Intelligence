# 0006 — Granularidade dos módulos e organização dos pacotes

- **Data:** 2026-09-01
- **Status:** aceita

## Contexto

A especificação §5 define seis módulos lógicos com fronteiras de responsabilidade (API,
Domínio, Fila de processamento, Adaptador de extração, Persistência, Fila de conferência). Este
ADR decide como essas fronteiras aparecem no código: em quantos artefatos, com que separação
física, e com ou sem portas (interfaces) explícitas entre elas.

Restrições que pesam na decisão: prazo de três dias; um único projeto, um único
desenvolvedor, sem múltiplas equipes; a fatia vertical entrega um tipo de documento
(identidade) de ponta a ponta; o back-end é Java + Spring Boot (trilha A), que já traz
estereótipos de separação (`@RestController`, `@Service`, `@Repository`). A troca de fornecedor
de IA é o único ponto de substituição de infraestrutura que o enunciado declara como certo — e
a especificação §5 já isola esse ponto no módulo Adaptador de extração.

## Alternativas consideradas

### A. Clean Architecture / Arquitetura Hexagonal (portas e adaptadores)

Entidade de domínio pura, separada da entidade de persistência; uma interface (porta) para
cada fronteira — repositório, adaptador, fila —, com implementações (adaptadores) num pacote de
infraestrutura; regra de negócio sem nenhuma anotação de framework.

**Descartada porque:** o custo não se paga nesta entrega. Cada entidade dobraria — uma pura de
domínio e uma de persistência —, com mapeamento entre as duas; toda fronteira exigiria uma
interface e uma implementação; o número de arquivos por funcionalidade cresceria três a quatro
vezes. O ganho — domínio testável isolado do framework, substituição fácil de infraestrutura —
tem valor quando há muitas equipes, muitos módulos, ou troca real de infraestrutura prevista.
Aqui há um projeto, um desenvolvedor, três dias, e o único ponto de troca declarado (o
fornecedor de IA) já está isolado pela fronteira do Adaptador de extração na especificação §5,
sem precisar de portas em todo o resto.

### B. Monolito sem nenhuma separação (tudo numa classe ou num pacote único)

Uma classe por endpoint fazendo validação, regra de negócio, acesso a banco e montagem de
resposta; ou um pacote plano com tudo junto.

**Descartada porque:** o problema que essa alternativa evitaria — regra de negócio misturada
com HTTP e com persistência — já está resolvido pelo módulo Domínio da especificação §5. A
separação em `Controller` / `Service` / `Repository` (estereótipos do Spring) mantém a regra de
negócio fora do HTTP e fora do SQL mesmo sem camadas em pacotes distintos. Descartar a
separação por classe seria abrir mão de uma fronteira barata e já paga.

## Decisão

Organização **por funcionalidade**: um pacote `documento/` reúne a entidade, o repositório, o
serviço, o controller, os DTOs e o tratamento de erro daquela funcionalidade. A separação de
responsabilidades acontece por classe e por estereótipo do Spring (`@RestController`,
`@Service`, `@Repository`), não por pacotes de camada nem por portas explícitas. A entidade JPA
é a própria entidade do domínio — não há uma classe de domínio separada.

O módulo Domínio da especificação §5 é realizado como classes de serviço nesse mesmo pacote,
não como uma camada isolada de framework.

## Consequências

- Menos arquivos por funcionalidade e navegação mais direta: tudo que toca "documento" está num
  lugar só. Bom para o tamanho e o prazo desta entrega.
- A regra de negócio carrega anotações de JPA junto (a entidade é a de persistência). Testar o
  domínio sem subir JPA fica limitado ao que dá para exercitar com a entidade como POJO e os
  repositórios mockados — foi o caminho usado nos testes desta fatia.
- Trocar o banco, ou o mecanismo de persistência, exigiria mexer nas classes de domínio, porque
  não há uma porta que as proteja disso. Aceito: a decisão de persistência (ADR 0002) e a de
  fila (ADR 0003) já escolheram PostgreSQL de propósito, e a fronteira que o enunciado exige —
  a do fornecedor de IA — continua isolada no Adaptador de extração.
- Se surgirem muitos tipos de documento, o pacote `documento/` pode ficar grande; a evolução
  natural é quebrar por subpacote de tipo, ainda dentro da organização por funcionalidade.

**Gatilho de revisão:** se o número de tipos de documento ou de desenvolvedores no projeto
crescer de forma significativa — a ponto de várias pessoas mexerem nas mesmas classes, ou de a
troca de infraestrutura deixar de ser hipotética —, reabrir a comparação com portas e
adaptadores, começando pela fronteira de persistência.
