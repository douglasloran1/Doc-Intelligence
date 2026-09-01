package br.com.docintelligence.documento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Um documento e seu estado corrente. Campos conforme a secao 3 da especificacao.
 * O historico de mudancas de estado nao vive aqui — e append-only em
 * {@link TransicaoEstado}.
 */
@Entity
@Table(name = "documento")
@Getter
@Setter
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** SHA-256 do conteudo do arquivo original, em hex minusculo. Base da idempotencia (secao 4, restricao c). */
    @Column(name = "hash_conteudo", nullable = false, unique = true, length = 64)
    private String hashConteudo;

    /** Tipo declarado do documento. Nesta fatia, sempre {@code "identidade"} (secao 2). */
    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoDocumento estado;

    /**
     * Campos extraidos, estrutura dependente do tipo. Coluna {@code jsonb}
     * (ADR 0002, consequencia registrada). Nulo ate a extracao acontecer.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "campos_extraidos", columnDefinition = "jsonb")
    private Map<String, Object> camposExtraidos;

    /** Minimo das confiancas dos campos obrigatorios do tipo (ADR 0005). Nulo ate a extracao. */
    @Column(name = "confianca")
    private Double confianca;

    /** Nome padronizado proposto para o arquivo (secao 3). Nulo ate a extracao. */
    @Column(name = "nome_padronizado")
    private String nomePadronizado;

    /** Versao do adaptador/prompt que gerou a extracao (restricao de ambiente f). Nulo ate a extracao. */
    @Column(name = "versao_adaptador")
    private String versaoAdaptador;

    /**
     * Identificador do operador que reivindicou a conferencia (secao 4).
     * Auto-declarado, nao autenticado nesta entrega (restricao g / achado 14).
     */
    @Column(name = "reivindicado_por")
    private String reivindicadoPor;

    @Column(name = "reivindicado_em")
    private Instant reivindicadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }
}
