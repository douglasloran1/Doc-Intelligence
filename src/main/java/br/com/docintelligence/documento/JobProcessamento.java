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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Item da fila de processamento (ADR 0003). Um por documento, criado junto com
 * ele em {@code POST /documentos}. O worker consome esta tabela por polling, com
 * {@code FOR UPDATE SKIP LOCKED}.
 */
@Entity
@Table(name = "job_processamento")
@Getter
@Setter
public class JobProcessamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false, updatable = false, unique = true)
    private Documento documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoJob estado;

    /** Identificador do worker que reivindicou o job. Nulo quando PENDENTE. */
    @Column(name = "reivindicado_por")
    private String reivindicadoPor;

    /** Fim do lease da reivindicação do worker (§6). Passado esse instante sem conclusão, o job volta a ser elegível. */
    @Column(name = "lease_expira_em")
    private Instant leaseExpiraEm;

    /** Quando o job volta a ser elegível após uma falha temporária (backoff). Nulo = elegível já. */
    @Column(name = "proxima_tentativa_em")
    private Instant proximaTentativaEm;

    /** Número de tentativas de processamento já feitas. Ao atingir o limite, o documento vai para falha_definitiva. */
    @Column(name = "tentativas", nullable = false)
    private int tentativas = 0;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
        if (this.estado == null) {
            this.estado = EstadoJob.PENDENTE;
        }
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    static JobProcessamento pendentePara(Documento documento) {
        JobProcessamento job = new JobProcessamento();
        job.documento = documento;
        job.estado = EstadoJob.PENDENTE;
        return job;
    }
}
