package com.parentplatform.config;

import com.parentplatform.service.EvenementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Envoie automatiquement les rappels d'évènements aux inscrits.
 *
 * La tâche tourne selon {@code app.rappels.cron} (toutes les 30 minutes par
 * défaut) et prévient les participants des évènements qui approchent.
 */
@Component
@EnableScheduling
public class RappelScheduler {

    private static final Logger log = LoggerFactory.getLogger(RappelScheduler.class);

    @Autowired
    private EvenementService evenementService;

    @Value("${app.rappels.heures-avant:24}")
    private int heuresAvant;

    @Scheduled(cron = "${app.rappels.cron:0 0/30 * * * *}")
    public void rappelsEvenements() {
        try {
            int traites = evenementService.envoyerRappels(heuresAvant);
            if (traites > 0) {
                log.info("Rappels envoyés pour {} évènement(s) à venir.", traites);
            }
        } catch (Exception e) {
            log.warn("Envoi des rappels impossible : {}", e.getMessage());
        }
    }
}
