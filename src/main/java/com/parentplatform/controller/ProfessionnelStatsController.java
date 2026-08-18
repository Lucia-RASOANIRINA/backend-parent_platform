package com.parentplatform.controller;

import com.parentplatform.api.ApiRoutes;
import com.parentplatform.model.*;
import com.parentplatform.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Bilan d'activité d'un professionnel (éducatrice ou psychologue).
 *
 * Le profil ne montrait que des champs de saisie : impossible de savoir si ses
 * conférences trouvent leur public, ni qui réagit à ses contenus. Toutes les
 * valeurs renvoyées ici sont calculées à partir de la base — aucune estimation.
 */
@RestController
@RequestMapping(ApiRoutes.STATS + "/professionnel")
public class ProfessionnelStatsController {

    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private LikePostRepository likePostRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private ResourceLikeRepository resourceLikeRepository;
    @Autowired private ResourceCommentRepository resourceCommentRepository;
    @Autowired private ResourceRatingRepository resourceRatingRepository;
    @Autowired private EvenementRepository evenementRepository;
    @Autowired private EvenementInscriptionRepository inscriptionRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<?> bilan(@PathVariable Long userId) {
        try {
            User pro = userRepository.findById(userId).orElse(null);
            if (pro == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Utilisateur non trouvé"));
            }

            Map<String, Object> bilan = new LinkedHashMap<>();
            bilan.put("evenements", bilanEvenements(userId));
            bilan.put("publications", bilanPublications(pro));
            bilan.put("ressources", bilanRessources(userId));
            bilan.put("audience", audience(userId, pro));

            return ResponseEntity.ok(Map.of("success", true, "bilan", bilan));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Ateliers et conférences animés, avec le détail des inscrits. */
    private Map<String, Object> bilanEvenements(Long userId) {
        List<Evenement> miens = evenementRepository.findByCreatedByIdOrderByDateAsc(userId);
        List<Long> ids = miens.stream().map(Evenement::getId).collect(Collectors.toList());

        Map<Long, List<EvenementInscription>> parEvenement = new HashMap<>();
        for (Long id : ids) parEvenement.put(id, inscriptionRepository.findByEvenementId(id));

        long totalInscrits = parEvenement.values().stream().mapToLong(List::size).sum();
        long enLigne = miens.stream()
                .filter(e -> e.getMeetingUrl() != null && !e.getMeetingUrl().isBlank())
                .count();

        List<Map<String, Object>> detail = miens.stream().map(e -> {
            List<EvenementInscription> inscrits = parEvenement.getOrDefault(e.getId(), List.of());
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("id", e.getId());
            ligne.put("titre", e.getTitre());
            ligne.put("date", e.getDate());
            ligne.put("enLigne", e.getMeetingUrl() != null && !e.getMeetingUrl().isBlank());
            ligne.put("annule", e.isAnnule());
            ligne.put("places", e.getCapacite());
            ligne.put("inscrits", inscrits.size());
            // Le taux de remplissage n'a de sens que si un nombre de places est fixé
            ligne.put("remplissage", e.getCapacite() != null && e.getCapacite() > 0
                    ? Math.round(inscrits.size() * 100.0 / e.getCapacite()) : null);
            ligne.put("participants", inscrits.stream()
                    .map(i -> Map.of("id", i.getUserId(), "nom", i.getUserNom() == null ? "" : i.getUserNom()))
                    .collect(Collectors.toList()));
            return ligne;
        }).collect(Collectors.toList());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total", miens.size());
        res.put("enLigne", enLigne);
        res.put("totalInscrits", totalInscrits);
        res.put("moyenneInscrits", miens.isEmpty() ? 0 : Math.round(totalInscrits * 10.0 / miens.size()) / 10.0);
        res.put("detail", detail);
        return res;
    }

    /** Publications de l'auteur : portée et réactions reçues. */
    private Map<String, Object> bilanPublications(User pro) {
        List<Post> miennes = postRepository.findByUserOrderByCreatedAtDesc(pro);
        long likes = 0, commentaires = 0;
        for (Post p : miennes) {
            likes += likePostRepository.findByPost(p).size();
            commentaires += commentRepository.findByPostOrderByCreatedAtDesc(p).size();
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total", miennes.size());
        res.put("likes", likes);
        res.put("commentaires", commentaires);
        res.put("interactionsParPublication",
                miennes.isEmpty() ? 0 : Math.round((likes + commentaires) * 10.0 / miennes.size()) / 10.0);
        return res;
    }

    /** Ressources déposées : consultation, appréciations et notes. */
    private Map<String, Object> bilanRessources(Long userId) {
        List<Resource> miennes = resourceRepository.findByOwnerId(userId);
        long likes = 0, commentaires = 0, notes = 0;
        double sommeNotes = 0;
        for (Resource r : miennes) {
            likes += resourceLikeRepository.findAll().stream()
                    .filter(l -> Objects.equals(l.getResourceId(), r.getId())).count();
            commentaires += resourceCommentRepository.findByResourceId(r.getId()).size();
            Long n = resourceRatingRepository.countByResourceId(r.getId());
            Double moy = resourceRatingRepository.getAverageRating(r.getId());
            if (n != null && n > 0 && moy != null) { notes += n; sommeNotes += moy * n; }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total", miennes.size());
        res.put("likes", likes);
        res.put("commentaires", commentaires);
        res.put("notes", notes);
        res.put("noteMoyenne", notes == 0 ? null : Math.round(sommeNotes * 10.0 / notes) / 10.0);
        return res;
    }

    /**
     * Qui suit ce professionnel : une même personne peut s'inscrire à un atelier,
     * aimer une publication et commenter une ressource. On la compte une fois,
     * en gardant le détail de ses interactions.
     */
    private List<Map<String, Object>> audience(Long userId, User pro) {
        Map<Long, Map<String, Object>> parPersonne = new LinkedHashMap<>();

        java.util.function.BiConsumer<Long, String> ajouter = (id, canal) -> {
            if (id == null || Objects.equals(id, userId)) return;
            Map<String, Object> ligne = parPersonne.computeIfAbsent(id, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                User u = userRepository.findById(k).orElse(null);
                m.put("id", k);
                m.put("nom", u != null ? u.getNom() : "—");
                m.put("role", u != null && u.getRole() != null ? u.getRole().name() : null);
                m.put("inscriptions", 0);
                m.put("likes", 0);
                m.put("commentaires", 0);
                return m;
            });
            ligne.put(canal, ((Integer) ligne.get(canal)) + 1);
        };

        for (Evenement e : evenementRepository.findByCreatedByIdOrderByDateAsc(userId)) {
            for (EvenementInscription i : inscriptionRepository.findByEvenementId(e.getId())) {
                ajouter.accept(i.getUserId(), "inscriptions");
            }
        }
        for (Post p : postRepository.findByUserOrderByCreatedAtDesc(pro)) {
            for (LikePost l : likePostRepository.findByPost(p)) {
                ajouter.accept(l.getUser() != null ? l.getUser().getId() : null, "likes");
            }
            for (Comment c : commentRepository.findByPostOrderByCreatedAtDesc(p)) {
                ajouter.accept(c.getUser() != null ? c.getUser().getId() : null, "commentaires");
            }
        }
        for (Resource r : resourceRepository.findByOwnerId(userId)) {
            for (ResourceComment c : resourceCommentRepository.findByResourceId(r.getId())) {
                ajouter.accept(c.getUserId(), "commentaires");
            }
        }

        // Les plus engagés d'abord : c'est l'information utile en haut de liste
        return parPersonne.values().stream()
                .sorted(Comparator.comparingInt(this::totalInteractions).reversed())
                .collect(Collectors.toList());
    }

    private int totalInteractions(Map<String, Object> ligne) {
        return (Integer) ligne.get("inscriptions") + (Integer) ligne.get("likes") + (Integer) ligne.get("commentaires");
    }
}
