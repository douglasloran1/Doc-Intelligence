package br.com.docintelligence.documento;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope de listagem paginada. Formato próprio em vez do {@code Page} do Spring
 * para não expor a estrutura interna dele na API (secao 4).
 */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {

    static <T> PaginaResponse<T> de(Page<?> page, List<T> conteudo) {
        return new PaginaResponse<>(
                conteudo, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
