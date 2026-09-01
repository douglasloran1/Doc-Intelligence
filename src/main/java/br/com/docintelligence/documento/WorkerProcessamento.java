package br.com.docintelligence.documento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Worker de processamento (ADR 0003; §6). A cada ciclo, reivindica um job
 * elegível e o processa. O polling roda um ciclo por vez (o scheduler padrão do
 * Spring é de thread única); várias instâncias em paralelo se coordenam pelo
 * {@code SKIP LOCKED} da fila.
 *
 * <p>O ciclo é intencionalmente fino — só orquestra. As duas transações
 * (reivindicar, processar) estão em {@link FilaProcessamento} e
 * {@link ProcessadorDocumento}, para os proxies transacionais valerem.
 */
@Component
public class WorkerProcessamento {

    private static final Logger log = LoggerFactory.getLogger(WorkerProcessamento.class);

    private final FilaProcessamento fila;
    private final ProcessadorDocumento processador;

    public WorkerProcessamento(FilaProcessamento fila, ProcessadorDocumento processador) {
        this.fila = fila;
        this.processador = processador;
    }

    @Scheduled(fixedDelayString = "${worker.polling-ms:2000}")
    public void ciclo() {
        try {
            fila.reivindicarProximo().ifPresent(this::processar);
        } catch (RuntimeException e) {
            log.error("Falha no ciclo do worker", e);
        }
    }

    private void processar(UUID jobId) {
        log.debug("Processando job {}", jobId);
        processador.executar(jobId);
    }
}
