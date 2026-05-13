package com.lucas.financas.scheduler;

import com.lucas.financas.service.TransacaoRecorrenteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class TransacoesRecorrentesScheduler {

    private static final Logger log = LoggerFactory.getLogger(TransacoesRecorrentesScheduler.class);

    private final TransacaoRecorrenteService service;

    public TransacoesRecorrentesScheduler(TransacaoRecorrenteService service) {
        this.service = service;
    }

    // todo dia 1h da manha processa as recorrencias do dia
    @Scheduled(cron = "0 0 1 * * *")
    public void rodar() {
        LocalDate hoje = LocalDate.now();
        log.info("processando recorrencias do dia {}", hoje);
        int n = service.processarPendentes(hoje);
        log.info("recorrencias executadas: {}", n);
    }
}
