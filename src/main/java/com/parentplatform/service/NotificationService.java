package com.parentplatform.service;

import com.parentplatform.model.Evenement;
import com.parentplatform.model.EvenementInscription;
import com.parentplatform.model.Notification;
import com.parentplatform.repository.EvenementInscriptionRepository;
import com.parentplatform.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Création et lecture des notifications (rappels d'évènements, inscriptions,
 * annulations…).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private EvenementInscriptionRepository inscriptionRepository;

    // ---- Lecture ----

    public List<Notification> lister(Long userId, int limite) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, Math.max(1, limite)));
    }

    public long nonLues(Long userId) {
        return repository.countByUserIdAndLuFalse(userId);
    }

    public void marquerLu(Long id) {
        repository.findById(id).ifPresent(n -> {
            n.setLu(true);
            repository.save(n);
        });
    }

    public void marquerToutLu(Long userId) {
        repository.marquerToutLu(userId);
    }

    public void supprimer(Long id) {
        repository.deleteById(id);
    }

    public void supprimerTout(Long userId) {
        repository.deleteByUserId(userId);
    }

    // ---- Écriture ----

    public Notification creer(Long userId, String type, String titre, String message, String lien, Long evenementId) {
        if (userId == null) return null;
        return repository.save(new Notification(userId, type, titre, message, lien, evenementId));
    }

    /** Évite d'empiler plusieurs fois le même rappel pour un même évènement. */
    public Notification creerUneFois(Long userId, String type, String titre, String message, String lien, Long evenementId) {
        if (userId == null || evenementId == null) return null;
        if (repository.existsByUserIdAndEvenementIdAndType(userId, evenementId, type)) return null;
        return creer(userId, type, titre, message, lien, evenementId);
    }

    /** Notifie tous les inscrits d'un évènement (modification, annulation, rappel…). */
    public int notifierInscrits(Evenement evenement, String type, String titre, String message) {
        int envoyes = 0;
        for (EvenementInscription i : inscriptionRepository.findByEvenementId(evenement.getId())) {
            Notification n = creerUneFois(i.getUserId(), type, titre, message,
                    lienEvenement(), evenement.getId());
            if (n != null) envoyes++;
        }
        if (envoyes > 0) {
            log.info("{} notification(s) « {} » envoyée(s) pour l'évènement #{}", envoyes, type, evenement.getId());
        }
        return envoyes;
    }

    /** Le frontend redirige ensuite vers l'espace évènements du rôle connecté. */
    public String lienEvenement() {
        return "/evenements";
    }

    public void supprimerPourEvenement(Long evenementId) {
        repository.deleteByEvenementId(evenementId);
    }

    // ---- Formatage pour l'API ----

    public Map<String, Object> format(Notification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("type", n.getType());
        m.put("titre", n.getTitre());
        m.put("message", n.getMessage());
        m.put("lien", n.getLien());
        m.put("evenementId", n.getEvenementId());
        m.put("lu", n.isLu());
        m.put("createdAt", n.getCreatedAt());
        return m;
    }
}
