package br.com.docintelligence.documento;

import br.com.docintelligence.documento.AdaptadorExtracao.CampoExtraido;
import br.com.docintelligence.documento.AdaptadorExtracao.ResultadoExtracao;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Módulo Domínio da especificação §5, realizado como classe de serviço (ADR
 * 0006). Não conhece HTTP, fila nem persistência: só regra.
 *
 * <p>Aqui vivem: quais campos são obrigatórios por tipo, o cálculo da confiança
 * do documento (ADR 0005), a decisão do próximo estado pelo limiar, e a geração
 * do nome padronizado (§3).
 */
@Component
public class RegrasDocumento {

    /** Limiar provisório entre pronto e conferência (ADR 0005). Vive num lugar só. */
    static final double LIMIAR_CONFIANCA = 0.85;

    private static final Map<String, Set<String>> OBRIGATORIOS_POR_TIPO = Map.of(
            "identidade", Set.of("nome_completo", "cpf", "data_nascimento"));

    public Set<String> camposObrigatorios(String tipo) {
        Set<String> campos = OBRIGATORIOS_POR_TIPO.get(tipo);
        if (campos == null) {
            throw new IllegalArgumentException("Tipo sem mapeamento de campos: " + tipo);
        }
        return campos;
    }

    /**
     * Confiança do documento = mínimo das confianças dos campos obrigatórios do
     * tipo (ADR 0005). Campos não-obrigatórios não entram, mesmo com confiança
     * baixa.
     *
     * <p>Se algum obrigatório não veio no resultado, o mínimo é tirado sobre os
     * que vieram; se nenhum veio, é erro (achado N10, ainda em aberto na
     * especificação — o dublê sempre devolve os três).
     */
    public double confiancaDoDocumento(ResultadoExtracao resultado, String tipo) {
        return camposObrigatorios(tipo).stream()
                .map(resultado.campos()::get)
                .filter(Objects::nonNull)
                .mapToDouble(CampoExtraido::confianca)
                .min()
                .orElseThrow(() -> new IllegalStateException(
                        "Adaptador não devolveu nenhum campo obrigatório para o tipo " + tipo));
    }

    public EstadoDocumento proximoEstado(double confiancaDoDocumento) {
        return confiancaDoDocumento >= LIMIAR_CONFIANCA
                ? EstadoDocumento.PRONTO
                : EstadoDocumento.AGUARDANDO_CONFERENCIA;
    }

    /** Nome padronizado proposto: {@code identidade_{id}.{extensao-original}} (§3), sem dado pessoal. */
    public String nomePadronizado(Documento documento) {
        return "%s_%s.%s".formatted(
                documento.getTipo(), documento.getId(), documento.getExtensaoOriginal());
    }

    /** Estrutura para a coluna jsonb {@code campos_extraidos}: por campo, o valor e a confiança (§3). */
    public Map<String, Object> camposParaPersistencia(ResultadoExtracao resultado) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        resultado.campos().forEach((chave, campo) ->
                mapa.put(chave, Map.of("valor", campo.valor(), "confianca", campo.confianca())));
        return mapa;
    }
}
