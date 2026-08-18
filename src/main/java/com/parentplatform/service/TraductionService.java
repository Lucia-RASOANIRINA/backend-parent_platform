package com.parentplatform.service;

import com.parentplatform.model.Traduction;
import com.parentplatform.repository.TraductionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduit les contenus écrits par les membres dans les trois langues de la
 * plateforme.
 *
 * La traduction est confiée à un moteur externe ({@link TraducteurExterne}),
 * seul capable de rendre une phrase lisible. Le {@link Glossaire} interne ne
 * sert que lorsque ce moteur est injoignable — connexion coupée, quota atteint :
 * le mot-à-mot reste alors compréhensible, et les mots absents du lexique sont
 * signalés pour que l'interface les souligne.
 */
@Service
public class TraductionService {

    public static final List<String> LANGUES = List.of("fr", "mg", "en");

    /** Un mot : lettres accentuées, apostrophes et traits d'union compris. */
    private static final Pattern MOT = Pattern.compile("[\\p{L}][\\p{L}'’\\-]*");

    @Autowired private TraductionRepository repository;
    @Autowired private TraducteurExterne traducteurExterne;

    /**
     * Produit et enregistre les traductions d'un contenu.
     * Appelée à la création ; rejouée telle quelle si le contenu est modifié.
     *
     * @param langueSource langue supposée du texte d'origine (« fr » par défaut)
     */
    public void traduireContenu(String typeContenu, Long contenuId, String texte, String langueSource) {
        if (contenuId == null || texte == null || texte.isBlank()) return;
        String source = LANGUES.contains(langueSource) ? langueSource : "fr";

        supprimerPour(typeContenu, contenuId);

        for (String cible : LANGUES) {
            if (cible.equals(source)) continue;

            // Un vrai moteur produit une phrase lisible ; le glossaire, lui,
            // ne sait faire que du mot-à-mot. On ne s'en sert qu'en dernier ressort.
            String parMoteur = traducteurExterne.traduire(texte, source, cible);
            if (parMoteur != null) {
                repository.save(new Traduction(typeContenu, contenuId, cible, parMoteur, ""));
                continue;
            }

            Resultat r = traduire(texte, source, cible);
            repository.save(new Traduction(typeContenu, contenuId, cible, r.texte, String.join("|", r.inconnus)));
        }
    }

    /** Une traduction n'a pas de sens sans son contenu : elle disparaît avec lui. */
    public void supprimerPour(String typeContenu, Long contenuId) {
        if (contenuId == null) return;
        repository.deleteByTypeContenuAndContenuId(typeContenu, contenuId);
    }

    public Optional<Traduction> lire(String typeContenu, Long contenuId, String langue) {
        return repository.findByTypeContenuAndContenuIdAndLangue(typeContenu, contenuId, langue);
    }

    /** Traductions d'un lot de contenus, indexées par identifiant. */
    public Map<Long, Traduction> lireLot(String typeContenu, List<Long> ids, String langue) {
        if (ids == null || ids.isEmpty()) return Map.of();
        Map<Long, Traduction> parId = new HashMap<>();
        for (Traduction t : repository.findLot(typeContenu, langue, ids)) parId.put(t.getContenuId(), t);
        return parId;
    }

    /** Texte traduit et liste des mots restés dans la langue d'origine. */
    public static class Resultat {
        public final String texte;
        public final List<String> inconnus;
        Resultat(String texte, List<String> inconnus) { this.texte = texte; this.inconnus = inconnus; }
    }

    /**
     * Traduction mot à mot. La ponctuation, les chiffres et la casse initiale
     * sont préservés : le texte garde sa forme même là où le lexique manque.
     */
    public Resultat traduire(String texte, String depuis, String vers) {
        StringBuilder sortie = new StringBuilder();
        LinkedHashSet<String> inconnus = new LinkedHashSet<>();

        Matcher m = MOT.matcher(texte);
        int position = 0;
        while (m.find()) {
            sortie.append(texte, position, m.start());
            String mot = m.group();
            String traduit = Glossaire.traduireMot(mot, depuis, vers);

            if (traduit == null) {
                sortie.append(mot);
                // Les mots d'une seule lettre et les nombres écrits ne disent rien d'utile
                if (mot.length() > 1) inconnus.add(mot);
            } else {
                sortie.append(respecterCasse(mot, traduit));
            }
            position = m.end();
        }
        sortie.append(texte.substring(position));
        return new Resultat(sortie.toString(), new ArrayList<>(inconnus));
    }

    /** « Enfant » doit rester capitalisé une fois traduit en « Zaza ». */
    private String respecterCasse(String original, String traduit) {
        if (original.isEmpty() || traduit.isEmpty()) return traduit;
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(traduit.charAt(0)) + traduit.substring(1);
        }
        return traduit;
    }
}
