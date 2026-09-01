package br.com.docintelligence.documento;

import br.com.docintelligence.documento.AdaptadorExtracao.EntradaExtracao;
import br.com.docintelligence.documento.AdaptadorExtracao.ResultadoExtracao;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DubleExtracaoTest {

    private final DubleExtracao duble = new DubleExtracao();

    private ResultadoExtracao extrair(long tamanhoBytes) {
        return duble.extrair(new EntradaExtracao("identidade", tamanhoBytes, null));
    }

    @Test
    void abaixoDe500KBaConfiancaDosObrigatoriosEh060() {
        ResultadoExtracao r = extrair(100 * 1024);

        assertThat(r.campos().get("nome_completo").confianca()).isEqualTo(0.60);
        assertThat(r.campos().get("cpf").confianca()).isEqualTo(0.60);
        assertThat(r.campos().get("data_nascimento").confianca()).isEqualTo(0.60);
    }

    @Test
    void de500KBouMaisaConfiancaDosObrigatoriosEh095() {
        ResultadoExtracao r = extrair(500 * 1024);

        assertThat(r.campos().get("nome_completo").confianca()).isEqualTo(0.95);
        assertThat(r.campos().get("cpf").confianca()).isEqualTo(0.95);
        assertThat(r.campos().get("data_nascimento").confianca()).isEqualTo(0.95);
    }

    @Test
    void naoObrigatoriosTemSempre090IndependenteDoTamanho() {
        assertThat(extrair(1).campos().get("orgao_emissor").confianca()).isEqualTo(0.90);
        assertThat(extrair(1).campos().get("data_emissao").confianca()).isEqualTo(0.90);
        assertThat(extrair(10_000_000).campos().get("orgao_emissor").confianca()).isEqualTo(0.90);
        assertThat(extrair(10_000_000).campos().get("data_emissao").confianca()).isEqualTo(0.90);
    }

    @Test
    void aFronteiraDe500KBeExata() {
        assertThat(extrair(500 * 1024 - 1).campos().get("cpf").confianca()).isEqualTo(0.60);
        assertThat(extrair(500 * 1024).campos().get("cpf").confianca()).isEqualTo(0.95);
    }

    @Test
    void todosOsValoresSaoPlaceholderNaoReal() {
        ResultadoExtracao r = extrair(600 * 1024);

        assertThat(r.campos().values())
                .allSatisfy(campo -> assertThat(campo.valor()).isEqualTo("PENDENTE DE EXTRAÇÃO REAL"));
    }

    @Test
    void recusaTipoQueNaoSejaIdentidade() {
        assertThatThrownBy(() -> duble.extrair(new EntradaExtracao("contracheque", 1000, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
