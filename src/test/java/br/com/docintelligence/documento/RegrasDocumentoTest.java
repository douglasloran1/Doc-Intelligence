package br.com.docintelligence.documento;

import br.com.docintelligence.documento.AdaptadorExtracao.CampoExtraido;
import br.com.docintelligence.documento.AdaptadorExtracao.ResultadoExtracao;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasDocumentoTest {

    private final RegrasDocumento regras = new RegrasDocumento();

    private static ResultadoExtracao resultado(double confObrigatorios, double confNaoObrigatorios) {
        Map<String, CampoExtraido> campos = new LinkedHashMap<>();
        campos.put("nome_completo", new CampoExtraido("x", confObrigatorios));
        campos.put("cpf", new CampoExtraido("x", confObrigatorios));
        campos.put("data_nascimento", new CampoExtraido("x", confObrigatorios));
        campos.put("orgao_emissor", new CampoExtraido("x", confNaoObrigatorios));
        campos.put("data_emissao", new CampoExtraido("x", confNaoObrigatorios));
        return new ResultadoExtracao(campos, "duble-identidade-1");
    }

    @Test
    void confiancaDoDocumentoEhOMinimoDosObrigatorios() {
        Map<String, CampoExtraido> campos = new LinkedHashMap<>();
        campos.put("nome_completo", new CampoExtraido("x", 0.95));
        campos.put("cpf", new CampoExtraido("x", 0.72));
        campos.put("data_nascimento", new CampoExtraido("x", 0.88));
        campos.put("orgao_emissor", new CampoExtraido("x", 0.90));
        ResultadoExtracao r = new ResultadoExtracao(campos, "v");

        assertThat(regras.confiancaDoDocumento(r, "identidade")).isEqualTo(0.72);
    }

    @Test
    void naoObrigatoriosNaoEntramNoCalculoMesmoComConfiancaBaixa() {
        ResultadoExtracao r = resultado(0.95, 0.10);

        assertThat(regras.confiancaDoDocumento(r, "identidade")).isEqualTo(0.95);
    }

    @Test
    void noLimiarOuAcimaVaiParaPronto() {
        assertThat(regras.proximoEstado(0.85)).isEqualTo(EstadoDocumento.PRONTO);
        assertThat(regras.proximoEstado(0.95)).isEqualTo(EstadoDocumento.PRONTO);
    }

    @Test
    void abaixoDoLimiarVaiParaConferencia() {
        assertThat(regras.proximoEstado(0.84)).isEqualTo(EstadoDocumento.AGUARDANDO_CONFERENCIA);
        assertThat(regras.proximoEstado(0.60)).isEqualTo(EstadoDocumento.AGUARDANDO_CONFERENCIA);
    }

    @Test
    void nomePadronizadoUsaTipoIdEExtensaoSemDadoPessoal() {
        Documento doc = new Documento();
        UUID id = UUID.randomUUID();
        doc.setId(id);
        doc.setTipo("identidade");
        doc.setExtensaoOriginal("jpg");

        assertThat(regras.nomePadronizado(doc)).isEqualTo("identidade_" + id + ".jpg");
    }

    @Test
    void camposParaPersistenciaGuardamValorEConfiancaPorCampo() {
        Map<String, Object> mapa = regras.camposParaPersistencia(resultado(0.95, 0.90));

        assertThat(mapa).containsOnlyKeys(
                "nome_completo", "cpf", "data_nascimento", "orgao_emissor", "data_emissao");
        assertThat(mapa.get("cpf")).isEqualTo(Map.of("valor", "x", "confianca", 0.95));
    }
}
