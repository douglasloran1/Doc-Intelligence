package br.com.docintelligence.documento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface JobProcessamentoRepository extends JpaRepository<JobProcessamento, UUID> {

    /**
     * Um job elegível: PENDENTE cujo backoff já passou, ou EM_EXECUCAO cujo lease
     * expirou (worker morreu no meio). {@code FOR UPDATE SKIP LOCKED} deixa
     * outros workers pularem a linha travada em vez de esperar — precisa rodar
     * dentro de uma transação (ADR 0003).
     */
    @Query(value = """
            SELECT * FROM job_processamento j
            WHERE (j.estado = 'PENDENTE'
                   AND (j.proxima_tentativa_em IS NULL OR j.proxima_tentativa_em <= :agora))
               OR (j.estado = 'EM_EXECUCAO' AND j.lease_expira_em < :agora)
            ORDER BY j.criado_em
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<JobProcessamento> reivindicarElegivel(@Param("agora") Instant agora);

    @Query("select j from JobProcessamento j join fetch j.documento where j.id = :id")
    Optional<JobProcessamento> buscarComDocumento(@Param("id") UUID id);

    Optional<JobProcessamento> findByDocumentoId(UUID documentoId);
}
