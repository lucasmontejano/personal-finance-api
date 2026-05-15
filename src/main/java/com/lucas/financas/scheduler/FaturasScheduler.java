package com.lucas.financas.scheduler;

import com.lucas.financas.model.Fatura;
import com.lucas.financas.model.StatusFatura;
import com.lucas.financas.repository.FaturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class FaturasScheduler {

    private static final Logger log = LoggerFactory.getLogger(FaturasScheduler.class);

    private final FaturaRepository faturaRepository;

    public FaturasScheduler(FaturaRepository faturaRepository) {
        this.faturaRepository = faturaRepository;
    }

    // todo dia as 2h da manha fecha as faturas cuja data de fechamento ja passou
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void fechar() {
        LocalDate hoje = LocalDate.now();
        int fechadas = fecharAbertasAte(hoje);
        log.info("faturas fechadas hoje ({}): {}", hoje, fechadas);
    }

    // exposto pro endpoint manual (smoke / debug)
    @Transactional
    public int fecharAbertasAte(LocalDate data) {
        List<Fatura> aFechar = faturaRepository.findByStatusAndDataFechamentoLessThanEqual(StatusFatura.ABERTA, data);
        for (Fatura f : aFechar) {
            f.setStatus(StatusFatura.FECHADA);
        }
        return aFechar.size();
    }
}
