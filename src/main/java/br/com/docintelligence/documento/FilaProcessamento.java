package br.com.docintelligence.documento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Dona da tabela de jobs (ADR 0003). Reivindica um job elegível numa transação
 * curta — marca EM_EXECUCAO e grava o lease — e devolve só o id. O processamento
 * de fato acontece depois, em {@link ProcessadorDocumento}, noutra transação, de
 * modo que um worker que morre no meio deixe o lease expirar e o job volte para
 * a fila.
 */
@Service
public class FilaProcessamento {

    private final JobProcessamentoRepository jobs;
    private final Duration lease;
    private final String workerId;

    public FilaProcessamento(
            JobProcessamentoRepository jobs,
            @Value("${worker.lease-segundos:60}") long leaseSegundos,
            @Value("${worker.id:worker-local}") String workerId) {
        this.jobs = jobs;
        this.lease = Duration.ofSeconds(leaseSegundos);
        this.workerId = workerId;
    }

    @Transactional
    public Optional<UUID> reivindicarProximo() {
        return jobs.reivindicarElegivel(Instant.now()).map(job -> {
            job.setEstado(EstadoJob.EM_EXECUCAO);
            job.setReivindicadoPor(workerId);
            job.setLeaseExpiraEm(Instant.now().plus(lease));
            job.setProximaTentativaEm(null);
            return job.getId();
        });
    }
}
