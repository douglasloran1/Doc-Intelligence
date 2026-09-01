package br.com.docintelligence.documento;

import java.util.Map;

/**
 * Fronteira com o fornecedor de extração (especificação §5). É o único ponto que
 * o enunciado declara que vai trocar de versão (restrição de ambiente f); nenhum
 * outro código conhece o fornecedor, o prompt ou o modelo.
 *
 * <p>Nesta entrega a implementação é {@link DubleExtracao}, um dublê
 * determinístico (ADR 0005).
 */
public interface AdaptadorExtracao {

    ResultadoExtracao extrair(EntradaExtracao entrada);

    /** Identificador da versão do adaptador/prompt, gravado no documento com a extração. */
    String versao();

    /**
     * O que o adaptador recebe. {@code conteudo} carrega os bytes do arquivo
     * quando houver — nesta entrega é {@code null}, porque o armazenamento do
     * arquivo original ainda não foi resolvido (achado 7 / restante da restrição
     * d). O dublê usa apenas {@code tamanhoBytes}.
     */
    record EntradaExtracao(String tipo, long tamanhoBytes, byte[] conteudo) {
    }

    /** O que o adaptador devolve: um campo por chave, cada um com sua confiança (0,0–1,0). */
    record ResultadoExtracao(Map<String, CampoExtraido> campos, String versao) {
    }

    record CampoExtraido(String valor, double confianca) {
    }
}
