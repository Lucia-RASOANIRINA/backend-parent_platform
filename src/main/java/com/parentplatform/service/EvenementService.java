package com.parentplatform.service;

import com.parentplatform.dto.EvenementFilter;
import com.parentplatform.model.Evenement;
import com.parentplatform.model.EvenementInscription;
import com.parentplatform.model.Notification;
import com.parentplatform.model.User;
import com.parentplatform.repository.EvenementInscriptionRepository;
import com.parentplatform.repository.EvenementRepository;
import com.parentplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EvenementService {

    @Autowired
    private EvenementRepository evenementRepository;

    @Autowired
    private EvenementInscriptionRepository inscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

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

    // =================================================================
    //  Recherche & filtrage
    // =================================================================

    /**
     * Applique tous les critères de l'espace évènements : recherche libre, type,
     * région, ville, langue, en ligne / présentiel, statut, disponibilité, prix,
     * période, puis tri.
     */
    public List<Evenement> rechercher(EvenementFilter f, Long userId) {
        List<Evenement> base = f.inclureNonPublies ? findAll() : findPublies();

        Set<Long> inscriptionsUtilisateur = (userId != null && Boolean.TRUE.equals(f.mesInscriptions))
                ? inscriptionsDe(userId)
                : Collections.emptySet();

        // Compteurs chargés en une fois : indispensable quand la base est distante
        Map<Long, Long> compteurs = Boolean.TRUE.equals(f.placesDisponibles) || "populaire".equals(f.tri)
                ? compteurs()
                : Collections.emptyMap();

        String recherche = f.q == null ? null : f.q.trim().toLowerCase();

        List<Evenement> resultat = base.stream()
                .filter(e -> recherche == null || recherche.isEmpty() || correspond(e, recherche))
                .filter(e -> vide(f.type) || f.type.equalsIgnoreCase(e.getType()))
                .filter(e -> vide(f.region) || f.region.equalsIgnoreCase(nz(e.getRegion())))
                .filter(e -> vide(f.ville) || nz(e.getVille()).toLowerCase().contains(f.ville.toLowerCase()))
                .filter(e -> vide(f.langue) || f.langue.equalsIgnoreCase(nz(e.getLangue())))
                .filter(e -> f.online == null || f.online == e.isOnline())
                .filter(e -> vide(f.statut) || f.statut.equalsIgnoreCase(e.getStatut()))
                .filter(e -> !Boolean.TRUE.equals(f.placesDisponibles) || placesRestantes(e, compteurs) > 0)
                .filter(e -> !Boolean.TRUE.equals(f.gratuit) || e.getPrix() == null || e.getPrix() == 0)
                .filter(e -> f.du == null || (e.getDate() != null && !e.getDate().isBefore(f.du)))
                .filter(e -> f.au == null || (e.getDate() != null && !e.getDate().isAfter(f.au)))
                .filter(e -> !Boolean.TRUE.equals(f.mesInscriptions) || inscriptionsUtilisateur.contains(e.getId()))
                .filter(e -> !Boolean.TRUE.equals(f.mesCreations)
                        || (userId != null && userId.equals(e.getCreatedById())))
                .collect(Collectors.toList());

        return trier(resultat, f.tri, compteurs);
    }

    /** Nombre d'inscrits par évènement, en une seule requête. */
    public Map<Long, Long> compteurs() {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] ligne : inscriptionRepository.compterParEvenement()) {
            map.put((Long) ligne[0], (Long) ligne[1]);
        }
        return map;
    }

    /** Évènements auxquels un utilisateur est inscrit, en une seule requête. */
    public Set<Long> inscriptionsDe(Long userId) {
        if (userId == null) return Collections.emptySet();
        return new HashSet<>(inscriptionRepository.evenementsDeLUtilisateur(userId));
    }

    private boolean correspond(Evenement e, String q) {
        return nz(e.getTitre()).toLowerCase().contains(q)
                || nz(e.getDescription()).toLowerCase().contains(q)
                || nz(e.getAnimateur()).toLowerCase().contains(q)
                || nz(e.getLieu()).toLowerCase().contains(q)
                || nz(e.getVille()).toLowerCase().contains(q)
                || nz(e.getRegion()).toLowerCase().contains(q)
                || nz(e.getTags()).toLowerCase().contains(q);
    }

    private List<Evenement> trier(List<Evenement> liste, String tri, Map<Long, Long> compteurs) {
        Comparator<Evenement> parDate = Comparator.comparing(
                Evenement::getDate, Comparator.nullsLast(Comparator.naturalOrder()));
        switch (tri == null ? "date" : tri) {
            case "recent":
                liste.sort(Comparator.comparing(Evenement::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));
                break;
            case "populaire":
                liste.sort(Comparator.comparingLong(
                                (Evenement e) -> compteurs.getOrDefault(e.getId(), 0L)).reversed()
                        .thenComparing(parDate));
                break;
            case "titre":
                liste.sort(Comparator.comparing(e -> nz(e.getTitre()).toLowerCase()));
                break;
            default:
                liste.sort(parDate);
        }
        return liste;
    }

    /** Valeurs disponibles pour alimenter les menus de filtres du frontend. */
    public Map<String, Object> facettes(boolean inclureNonPublies) {
        List<Evenement> base = inclureNonPublies ? findAll() : findPublies();
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("types", base.stream().map(Evenement::getType).filter(Objects::nonNull)
                .distinct().sorted().collect(Collectors.toList()));
        f.put("regions", base.stream().map(Evenement::getRegion)
                .filter(r -> r != null && !r.isBlank()).distinct().sorted().collect(Collectors.toList()));
        f.put("villes", base.stream().map(Evenement::getVille)
                .filter(v -> v != null && !v.isBlank()).distinct().sorted().collect(Collectors.toList()));
        f.put("animateurs", base.stream().map(Evenement::getAnimateur)
                .filter(a -> a != null && !a.isBlank()).distinct().sorted().collect(Collectors.toList()));
        f.put("langues", base.stream().map(Evenement::getLangue)
                .filter(l -> l != null && !l.isBlank()).distinct().sorted().collect(Collectors.toList()));
        f.put("total", base.size());
        f.put("enLigne", base.stream().filter(Evenement::isOnline).count());
        f.put("conferences", base.stream().filter(e -> "conference".equalsIgnoreCase(nz(e.getType()))).count());
        return f;
    }

    // =================================================================
    //  Création / modification
    // =================================================================

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
                String room = "ParentiaConf-" + UUID.randomUUID().toString().substring(0, 8);
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

        LocalDate ancienneDate = e.getDate();
        String ancienneHeure = e.getHeureDebut();

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
        if (data.getRegion() != null) e.setRegion(data.getRegion());
        if (data.getVille() != null) e.setVille(data.getVille());
        if (data.getLangue() != null) e.setLangue(data.getLangue());
        if (data.getPrix() != null) e.setPrix(data.getPrix());
        if (data.getTags() != null) e.setTags(data.getTags());
        e.setOnline(data.isOnline());
        if (data.getMeetingUrl() != null) e.setMeetingUrl(data.getMeetingUrl());
        e.setPublie(data.isPublie());
        prepareOnlineMeeting(e);
        e.setUpdatedAt(LocalDateTime.now());
        Evenement saved = evenementRepository.save(e);

        // Prévenir les inscrits si la date ou l'heure a bougé
        boolean dateChangee = data.getDate() != null && !Objects.equals(ancienneDate, saved.getDate());
        boolean heureChangee = data.getHeureDebut() != null && !Objects.equals(ancienneHeure, saved.getHeureDebut());
        if (dateChangee || heureChangee) {
            notificationService.notifierInscrits(saved, Notification.MODIFICATION,
                    "Évènement modifié",
                    "« " + saved.getTitre() + " » a été reprogrammé au "
                            + formatDate(saved.getDate())
                            + (saved.getHeureDebut() != null ? " à " + saved.getHeureDebut() : "") + ".");
        }
        return saved;
    }

    /** Annule un évènement sans le supprimer : les inscrits sont prévenus. */
    public Evenement annuler(Long id, String motif) {
        Evenement e = evenementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Évènement introuvable"));
        e.setAnnule(true);
        e.setMotifAnnulation(motif);
        e.setUpdatedAt(LocalDateTime.now());
        Evenement saved = evenementRepository.save(e);
        notificationService.notifierInscrits(saved, Notification.ANNULATION,
                "Évènement annulé",
                "« " + saved.getTitre() + " » du " + formatDate(saved.getDate()) + " est annulé."
                        + (motif != null && !motif.isBlank() ? " Motif : " + motif : ""));
        return saved;
    }

    /** Réactive un évènement annulé. */
    public Evenement reactiver(Long id) {
        Evenement e = evenementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Évènement introuvable"));
        e.setAnnule(false);
        e.setMotifAnnulation(null);
        e.setUpdatedAt(LocalDateTime.now());
        return evenementRepository.save(e);
    }

    /**
     * Duplique un évènement (nouvelle date, aucune inscription reprise).
     * Pratique pour les ateliers récurrents.
     */
    public Evenement dupliquer(Long id, LocalDate nouvelleDate, Long userId) {
        Evenement src = evenementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Évènement introuvable"));
        Evenement copie = new Evenement();
        copie.setTitre(src.getTitre());
        copie.setDescription(src.getDescription());
        copie.setType(src.getType());
        copie.setDate(nouvelleDate != null ? nouvelleDate : src.getDate().plusWeeks(1));
        copie.setHeureDebut(src.getHeureDebut());
        copie.setHeureFin(src.getHeureFin());
        copie.setLieu(src.getLieu());
        copie.setAnimateur(src.getAnimateur());
        copie.setCapacite(src.getCapacite());
        copie.setImageUrl(src.getImageUrl());
        copie.setRegion(src.getRegion());
        copie.setVille(src.getVille());
        copie.setLangue(src.getLangue());
        copie.setPrix(src.getPrix());
        copie.setTags(src.getTags());
        copie.setOnline(src.isOnline());
        copie.setMeetingUrl(null); // un nouveau salon est généré
        copie.setPublie(false);    // relu avant publication
        return create(copie, userId != null ? userId : src.getCreatedById());
    }

    @Transactional
    public void delete(Long id) {
        inscriptionRepository.deleteByEvenementId(id);
        notificationService.supprimerPourEvenement(id);
        evenementRepository.deleteById(id);
    }

    // =================================================================
    //  Inscriptions
    // =================================================================

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

    /** Variante sans requête : utilise les compteurs déjà chargés pour la liste. */
    public long placesRestantes(Evenement e, Map<Long, Long> compteurs) {
        int cap = e.getCapacite() == null ? 0 : e.getCapacite();
        return Math.max(0, cap - compteurs.getOrDefault(e.getId(), 0L));
    }

    public EvenementInscription inscrire(Long evenementId, Long userId) {
        Evenement e = evenementRepository.findById(evenementId)
                .orElseThrow(() -> new IllegalArgumentException("Évènement introuvable"));
        if (e.isAnnule()) {
            throw new IllegalArgumentException("Cet évènement a été annulé");
        }
        if (e.getDate() != null && e.getDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cet évènement est déjà terminé");
        }
        if (inscriptionRepository.existsByEvenementIdAndUserId(evenementId, userId)) {
            throw new IllegalArgumentException("Vous êtes déjà inscrit à cet évènement");
        }
        if (placesRestantes(e) <= 0) {
            throw new IllegalArgumentException("Plus de places disponibles");
        }
        String nom = userRepository.findById(userId).map(User::getNom).orElse("Utilisateur");
        EvenementInscription inscription =
                inscriptionRepository.save(new EvenementInscription(evenementId, userId, nom));

        notificationService.creer(userId, Notification.INSCRIPTION,
                "Inscription confirmée",
                "Vous participez à « " + e.getTitre() + " » le " + formatDate(e.getDate())
                        + (e.getHeureDebut() != null ? " à " + e.getHeureDebut() : "") + ".",
                notificationService.lienEvenement(), e.getId());
        return inscription;
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

    // =================================================================
    //  Rappels automatiques
    // =================================================================

    /**
     * Envoie un rappel aux inscrits des évènements qui ont lieu dans la fenêtre
     * indiquée. Appelée périodiquement par {@code RappelScheduler}.
     *
     * @return le nombre d'évènements pour lesquels un rappel a été émis
     */
    public int envoyerRappels(int heuresAvant) {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate limite = aujourdhui.plusDays(Math.max(1, heuresAvant / 24));
        int traites = 0;
        for (Evenement e : findPublies()) {
            if (e.isAnnule() || e.isRappelEnvoye() || e.getDate() == null) continue;
            if (e.getDate().isBefore(aujourdhui) || e.getDate().isAfter(limite)) continue;

            notificationService.notifierInscrits(e, Notification.RAPPEL,
                    "Rappel : " + e.getTitre(),
                    "C'est " + (e.getDate().isEqual(aujourdhui) ? "aujourd'hui" : "demain")
                            + (e.getHeureDebut() != null ? " à " + e.getHeureDebut() : "")
                            + (e.isOnline() ? " — en ligne." : " — à " + nz(e.getLieu()) + "."));
            e.setRappelEnvoye(true);
            evenementRepository.save(e);
            traites++;
        }
        return traites;
    }

    // =================================================================
    //  Export calendrier (.ics)
    // =================================================================

    /** Génère un fichier iCalendar importable dans Google Agenda / Outlook. */
    public String versIcs(Evenement e) {
        String debut = icsDateTime(e.getDate(), e.getHeureDebut(), "09h00");
        String fin = icsDateTime(e.getDate(), e.getHeureFin(), "11h00");
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n")
          .append("VERSION:2.0\r\n")
          .append("PRODID:-//Parentia//Agenda//FR\r\n")
          .append("CALSCALE:GREGORIAN\r\n")
          .append("METHOD:PUBLISH\r\n")
          .append("BEGIN:VEVENT\r\n")
          .append("UID:parentia-evenement-").append(e.getId()).append("@parentia.mg\r\n")
          .append("DTSTAMP:").append(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                  .format(LocalDateTime.now())).append("\r\n")
          .append("DTSTART:").append(debut).append("\r\n")
          .append("DTEND:").append(fin).append("\r\n")
          .append("SUMMARY:").append(echapper(e.getTitre())).append("\r\n")
          .append("DESCRIPTION:").append(echapper(nz(e.getDescription())
                  + (e.getAnimateur() != null ? "\\nAnimé par " + e.getAnimateur() : ""))).append("\r\n")
          .append("LOCATION:").append(echapper(e.isOnline() && e.getMeetingUrl() != null
                  ? e.getMeetingUrl() : nz(e.getLieu()))).append("\r\n")
          .append("STATUS:").append(e.isAnnule() ? "CANCELLED" : "CONFIRMED").append("\r\n")
          .append("END:VEVENT\r\n")
          .append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    /** Convertit « 10h00 » ou « 10:00 » en horodatage iCalendar local. */
    private String icsDateTime(LocalDate date, String heure, String defaut) {
        LocalDate d = date != null ? date : LocalDate.now();
        String h = (heure == null || heure.isBlank()) ? defaut : heure;
        String chiffres = h.replaceAll("[^0-9]", "");
        String hh = chiffres.length() >= 2 ? chiffres.substring(0, 2) : "09";
        String mm = chiffres.length() >= 4 ? chiffres.substring(2, 4) : "00";
        return String.format("%04d%02d%02dT%s%s00",
                d.getYear(), d.getMonthValue(), d.getDayOfMonth(), hh, mm);
    }

    private String echapper(String s) {
        return nz(s).replace("\\", "\\\\").replace(";", "\\;")
                .replace(",", "\\,").replace("\n", "\\n");
    }

    // =================================================================

    private String formatDate(LocalDate d) {
        return d == null ? "" : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
