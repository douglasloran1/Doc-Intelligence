# 004 — Swagger UI (documentação interativa da API)

**Sessão:** 2026-09-01
**Objetivo:** adicionar documentação interativa da API (Swagger UI via springdoc-openapi) sobre os seis endpoints da fatia vertical, com descrições e códigos de erro puxados da §4, sem tocar em comportamento.

---

## Prompt 1

Adicionar Swagger UI (documentação interativa da API) ao projeto, seguindo a especificação §4.

Preciso que você:

1. Adicione a dependência springdoc-openapi-starter-webmvc-ui ao pom.xml, compatível com Spring Boot 3.5.16 e Java 21 (linha 2.x do springdoc, por exemplo 2.8.5 — confirme a versão compatível mais recente antes de fixar).

2. Configure informações básicas da documentação (título, descrição curta, versão) via um @Bean OpenAPI, com o título "DOC Intelligence" e uma descrição de uma frase apontando para a especificação.

3. Anote os seis endpoints existentes (POST /documentos, GET /documentos/{id}, GET /documentos, POST /{id}/reivindicar, PATCH /{id}, POST /{id}/rejeitar) com @Operation e @ApiResponse (springdoc/swagger annotations), descrevendo em português o que cada operação faz, os códigos de erro possíveis (404/409/422) e seus códigos de motivo, puxando da seção 4 da especificação. Não precisa duplicar a especificação inteira — uma frase por endpoint mais os códigos de erro já basta.

4. Adicione exemplos de corpo de requisição para os endpoints que recebem JSON (reivindicar, corrigir, rejeitar), usando os mesmos valores fictícios do README (CPF 000.000.000-00, operador "op-1").

5. Confirme que a Swagger UI fica disponível em /swagger-ui.html sem necessidade de nenhuma configuração adicional do meu lado.

6. Rode mvn test para confirmar que nada quebrou com a dependência nova.

7. Atualize o README.md: uma seção curta (pode ir logo após "Rodar") dizendo que a documentação interativa está disponível em http://localhost:8080/swagger-ui.html após subir a aplicação, e que ela permite testar os endpoints sem precisar montar comandos curl manualmente.

Me mostre o resultado antes de qualquer commit — e, se possível, suba a aplicação localmente para confirmar que a página realmente carrega antes de propor o commit.

**Retorno em uma linha:** dependência `springdoc-openapi-starter-webmvc-ui:2.9.0` no `pom.xml`; novo `ConfiguracaoOpenApi` com `@Bean OpenAPI` (título/descrição/versão); `@Operation` + `@ApiResponses` nos seis endpoints do `DocumentoController`, com os códigos de motivo da §4; exemplos de corpo (`@ExampleObject`) em reivindicar/corrigir/rejeitar com os valores do README; seção nova no `README.md` após "Rodar"; `mvn test` 35/35 verde; Swagger UI verificada numa instância na 8081 (a instância na 8080 já rodava código anterior) — `/swagger-ui.html` 302→`/swagger-ui/index.html` 200, `/v3/api-docs` 200.

**Verifiquei:**
- Versão do springdoc: consultei o `maven-metadata.xml` oficial — 3.x tem alvo Spring Boot 4; a última 2.x é a 2.9.0 e o POM dela declara `spring-boot-starter-parent` 3.5.16, a mesma deste projeto. Fixei 2.9.0 (mais recente compatível), não a 2.8.5 citada como exemplo.
- `mvn test`: 35/35, `BUILD SUCCESS`. Os seis arquivos de teste existentes passam sem alteração — o `@WebMvcTest` não carrega a autoconfig do springdoc, então o slice não muda.
- Subi a app na 8081 (a 8080 estava ocupada por instância anterior à mudança) e li o `/v3/api-docs` gerado: título "DOC Intelligence", versão `0.0.1-SNAPSHOT`, descrição apontando para `docs/01-especificacao.md §4`; os seis paths presentes; cada `@ApiResponse` com os códigos de motivo conferidos contra a §4 e contra os `throw` do `DocumentoService`/`ConferenciaService` (`arquivo_ausente`/`formato_nao_suportado`/`arquivo_grande`, `documento_nao_encontrado`, `status_invalido`, `ja_reivindicado`/`estado_invalido`, `nao_esta_em_conferencia`/`operador_diferente`, `motivo_obrigatorio`/`operador_obrigatorio`); exemplos de corpo com CPF `000.000.000-00`, operador `op-1`, motivo `arquivo ilegivel` — os mesmos do README.
- `/swagger-ui.html` responde 302 para `/swagger-ui/index.html`, que responde 200 `text/html` — sem nenhuma configuração adicional em `application.yml`.
- Encerrei a instância da 8081 ao fim; a instância da 8080 (do usuário) não foi tocada.

**Ação:** aceito. Um erro do agente durante a execução (nome de pacote da anotação errado — `parameter` em vez de `parameters`), pego pelo `mvn test` na mesma sessão e corrigido antes de mostrar o resultado — registrado em `ia/onde-errou.md`.

---

## Prompt 2

Aprovado. Sobre o ponto da doutrina: não precisa de ADR novo — adicione uma frase em restricoes.md, na restrição (g) ou onde fizer mais sentido, dizendo que a Swagger UI expõe /swagger-ui e /v3/api-docs sem autenticação, e que isso é coerente com o risco já aceito de ausência de autenticação nesta entrega (não é uma categoria de risco nova).

Pode preparar o registro de IA (004) e propor o commit.

**Retorno em uma linha:** frase adicionada ao "Risco residual" da restrição (g) em `docs/restricoes.md` (Swagger UI expõe `/swagger-ui` e `/v3/api-docs` sem autenticação; mesma ausência de autenticação já aceita, não categoria nova; serviço consumido por sistemas internos); este arquivo criado; entrada nova em `ia/onde-errou.md` para o erro de nome de pacote.

**Verifiquei:** a frase entrou na restrição que o próprio enunciado do usuário indicou (g); não inventa tratamento novo — aponta para a premissa de chamador único da §2 já registrada ali; reli para confirmar que não contradiz o "Fora desta entrega, por escolha" da §2 (autenticação real fora do escopo) nem a §9.

**Ação:** aceito.

---

## Fechamento da sessão

**Entregue:** documentação interativa da API via springdoc-openapi 2.9.0 — `@Bean OpenAPI` com metadados, `@Operation`/`@ApiResponse` nos seis endpoints com os códigos de motivo da §4, exemplos de corpo nas três rotas JSON, seção no README, frase de risco em `docs/restricoes.md`. Nenhuma mudança de comportamento: os 35 testes passam sem alteração. Swagger UI confirmada carregando numa instância local.

**Não feito nesta sessão, escrito como não feito:**

- A Swagger UI e o `/v3/api-docs` ficam sem autenticação — coerente com a ausência de autenticação já aceita nesta entrega (spec §2 e §9, `restricoes.md` (g)), não tratada aqui.
- A instância da app na porta 8080 (iniciada antes desta mudança) segue rodando o código anterior, sem o springdoc; para ver a Swagger UI na 8080 é preciso reiniciar a app. Verificação desta sessão foi feita numa segunda instância na 8081.
- Sem ADR para a escolha springdoc vs. OpenAPI manual vs. nenhuma documentação — decisão do usuário registrada no Prompt 2 (não justifica ADR); a frase em `restricoes.md` cobre o efeito sobre as restrições.

**Pendente:** commit (código + docs + `004` + entrada em `onde-errou.md`), mostrado ao usuário antes de commitar.
