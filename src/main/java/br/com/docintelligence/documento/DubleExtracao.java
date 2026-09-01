package br.com.docintelligence.documento;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dublê determinístico do adaptador de extração (ADR 0005). Não fala com
 * fornecedor nenhum: devolve valores fixos e fictícios, e deriva a confiança dos
 * campos obrigatórios do tamanho do arquivo.
 *
 * <ul>
 *   <li>arquivo com menos de 500 KB → confiança 0,60 nos obrigatórios (abaixo do limiar → conferência);</li>
 *   <li>arquivo com 500 KB ou mais → confiança 0,95 nos obrigatórios (acima do limiar → pronto);</li>
 *   <li>campos não-obrigatórios → sempre 0,90.</li>
 * </ul>
 *
 * A relação tamanho ↔ qualidade é aproximação assumida para esta entrega, não
 * medida real de OCR (ADR 0005). O dublê nunca falha — o caminho de retry /
 * falha_definitiva é exercitado nos testes com um adaptador que lança
 * {@link FalhaExtracaoException}.
 */
@Component
public class DubleExtracao implements AdaptadorExtracao {

    static final long LIMITE_TAMANHO_BYTES = 500L * 1024;
    static final double CONFIANCA_OBRIGATORIOS_BAIXA = 0.60;
    static final double CONFIANCA_OBRIGATORIOS_ALTA = 0.95;
    static final double CONFIANCA_NAO_OBRIGATORIOS = 0.90;
    static final String VALOR_PLACEHOLDER = "PENDENTE DE EXTRAÇÃO REAL";
    static final String VERSAO = "duble-identidade-1";

    @Override
    public ResultadoExtracao extrair(EntradaExtracao entrada) {
        if (!"identidade".equals(entrada.tipo())) {
            throw new IllegalArgumentException(
                    "O dublê só conhece o tipo 'identidade'; recebeu: " + entrada.tipo());
        }

        double confiancaObrigatorios = entrada.tamanhoBytes() < LIMITE_TAMANHO_BYTES
                ? CONFIANCA_OBRIGATORIOS_BAIXA
                : CONFIANCA_OBRIGATORIOS_ALTA;

        Map<String, CampoExtraido> campos = new LinkedHashMap<>();
        campos.put("nome_completo", new CampoExtraido(VALOR_PLACEHOLDER, confiancaObrigatorios));
        campos.put("cpf", new CampoExtraido(VALOR_PLACEHOLDER, confiancaObrigatorios));
        campos.put("data_nascimento", new CampoExtraido(VALOR_PLACEHOLDER, confiancaObrigatorios));
        campos.put("orgao_emissor", new CampoExtraido(VALOR_PLACEHOLDER, CONFIANCA_NAO_OBRIGATORIOS));
        campos.put("data_emissao", new CampoExtraido(VALOR_PLACEHOLDER, CONFIANCA_NAO_OBRIGATORIOS));

        return new ResultadoExtracao(campos, VERSAO);
    }

    @Override
    public String versao() {
        return VERSAO;
    }
}
