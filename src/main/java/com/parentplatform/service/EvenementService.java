package com.parentplatform.service;

import com.parentplatform.model.Evenement;
import com.parentplatform.model.EvenementInscription;
import com.parentplatform.model.User;
import com.parentplatform.repository.EvenementInscriptionRepository;
import com.parentplatform.repository.EvenementRepository;
import com.parentplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EvenementService {

    @Autowired
    private EvenementRepository evenementRepository;

    @Autowired
    private EvenementInscriptionRepository inscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Evenement> findAll() {
        return evenementRepository.findAllByOrderByDateAsc();
    }

    public List<Evenement> findPublies() {
        return evenementRepository.findByPublieTrueOrderByDateAsc();
    }

    public List<Evenement> findByCreator(Long userId) {
        return evenementRepository.findByCreatedByIdOrderByDateAsc(userId);
    }

    public Optional<Evenement> findById(Long id) {
        return evenementRepository.findById(id);
    }

    public Evenement create(Evenement evenement, Long userId) {
        if (userId != null) {
            Optional<User> user = userRepository.findById(userId);
            user.ifPresent(u -> {
                evenement.setCreatedById(u.getId());
                evenement.setCreatedByNom(u.getNom());
                evenement.setCreatedByRole(u.getRole());
            });
        }
        prepareOnlineMeeting(evenement);
        evenement.setCreatedAt(LocalDateTime.now());
        return evenementRepository.save(evenement);
    }

    /**
     * Conférence en ligne (réunion type Google Meet) : génère un lien Jitsi unique
     * et limite la capacité à 4 participants pour rester simple et fluide.
     */
    private void prepareOnlineMeeting(Evenement e) {
        if (e.isOnline() && "conference".equalsIgnoreCase(e.getType())) {
            if (e.getMeetingUrl() == null || e.getMeetingUrl().isBlank()) {
                String room = "ParentiaConf-" + java.util.UUID.randomUUID().toString().substring(0, 8);
                e.setMeetingUrl("https://meet.jit.si/" + room);
            }
            if (e.getCapacite() == null || e.getCapacite() > 4) {
                e.setCapacite(4);
            }
            if (e.getLieu() == null || e.getLieu().isBlank()) {
                e.setLieu("En ligne");
            }
        }
    }

    public Evenement update(Long id, Evenement data) {
        Evenement e = evenementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Évènement introuvable"));
        if (data.getTitre() != null) e.setTitre(data.getTitre());
        if (data.getDescription() != null) e.setDescription(data.getDescription());
        if (data.getType() != null) e.setType(data.getType());
        if (data.getDate() != null) e.setDate(data.getDate());
        if (data.getHeureDebut() != null) e.setHeureDebut(data.getHeureDebut());
        if (data.getHeureFin() != null) e.setHeureFin(data.getHeureFin());
        if (data.getLieu() != null) e.setLieu(data.getLieu());
        if (data.getAnimateur() != null) e.setAnimateur(data.getAnimateur());
        if (data.getCapacite() != null) e.setCapacite(data.getCapacite());
        if (data.getImageUrl() != null) e.setImageUrl(data.getImageUrl());
        e.setOnline(data.isOnline());
        if (data.getMeetingUrl() != null) e.setMeetingUrl(data.getMeetingUrl());
        e.setPublie(data.isPublie());
        prepareOnlineMeeting(e);
        e.setUpdatedAt(LocalDateTime.now());
        return evenementRepository.save(e);
    }

    @Transactional
    public void delete(Long id) {
        inscriptionRepository.deleteByEvenementId(id);
        evenementRepository.deleteById(id);
    }

    // ---- Inscriptions ----

    public long countInscriptions(Long evenementId) {
        return inscriptionRepository.countByEvenementId(evenementId);
    }

    public boolean isInscrit(Long evenementId, Long userId) {
        if (userId == null) return false;
        return inscriptionRepository.existsByEvenementIdAndUserId(evenementId, userId);
    }

    public long placesRestantes(Evenement e) {
        int cap = e.getCapacite() == null ? 0 : e.getCapacite();
        return Math.max(0, cap - countInscriptions(e.getId()));
    }

    public EvenementInscription inscrire(Long evenementId, Long userId) {
        Evenement e = evenementRepository.findById(evenementId)
                .orElseThrow(() -> new IllegalArgumentException("Évènement introuvable"));
        if (inscriptionRepository.existsByEvenementIdAndUserId(evenementId, userId)) {
            throw new IllegalArgumentException("Vous êtes déjà inscrit à cet évènement");
        }
        if (placesRestantes(e) <= 0) {
            throw new IllegalArgumentException("Plus de places disponibles");
        }
        String nom = userRepository.findById(userId).map(User::getNom).orElse("Utilisateur");
        return inscriptionRepository.save(new EvenementInscription(evenementId, userId, nom));
    }

    @Transactional
    public void desinscrire(Long evenementId, Long userId) {
        inscriptionRepository.findByEvenementIdAndUserId(evenementId, userId)
                .ifPresent(inscriptionRepository::delete);
    }

    public List<EvenementInscription> inscriptions(Long evenementId) {
        return inscriptionRepository.findByEvenementId(evenementId);
    }

    public long total() {
        return evenementRepository.count();
    }
}
