package br.com.docintelligence.documento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro append-only de cada mudanca de estado de um documento — secao 3:
 * "cada transicao e um evento gravado, nao uma sobrescrita". Nunca atualizado
 * nem apagado; e o que permite reconstruir por que um documento esta onde esta.
 */
@Entity
@Table(name = "transicao_estado")
@Getter
@Setter
public class TransicaoEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false, updatable = false)
    private Documento documento;

    /** Estado antes da transicao. Nulo apenas na primeira (criacao direto em RECEBIDO). */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 40, updatable = false)
    private EstadoDocumento estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", nullable = false, length = 40, updatable = false)
    private EstadoDocumento estadoNovo;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private Instant ocorridoEm;

    /** Motivo textual, quando aplicavel — por exemplo o motivo obrigatorio da rejeicao (secao 4). */
    @Column(name = "motivo", updatable = false)
    private String motivo;

    @PrePersist
    void aoCriar() {
        this.ocorridoEm = Instant.now();
    }

    static TransicaoEstado de(Documento documento, EstadoDocumento anterior,
                              EstadoDocumento novo, String motivo) {
        TransicaoEstado t = new TransicaoEstado();
        t.documento = documento;
        t.estadoAnterior = anterior;
        t.estadoNovo = novo;
        t.motivo = motivo;
        return t;
    }
}
