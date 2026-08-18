package com.parentplatform.service;

import java.util.*;

/**
 * Glossaire trilingue de la plateforme (français, malagasy, anglais).
 *
 * Parentia fonctionne aussi hors connexion et sans compte payant : appeler un
 * service de traduction en ligne à chaque publication n'était pas envisageable.
 * On traduit donc à partir d'un lexique du domaine — parentalité, petite
 * enfance, vie de la communauté — et l'on signale ouvertement les mots absents
 * plutôt que de les deviner.
 *
 * Le lexique se complète : chaque mot signalé par l'interface est un mot à
 * ajouter ici.
 */
public final class Glossaire {

    private Glossaire() {}

    /** fr → { mg, en }. La clé est écrite en minuscules, sans accent. */
    private static final Map<String, String[]> LEXIQUE = new HashMap<>();

    /** mg → fr et en → fr, construits à partir du lexique. */
    private static final Map<String, String> VERS_FR = new HashMap<>();

    private static void mot(String fr, String mg, String en) {
        LEXIQUE.put(cle(fr), new String[]{mg, en});
        VERS_FR.putIfAbsent(cle(mg), fr);
        VERS_FR.putIfAbsent(cle(en), fr);
    }

    static {
        // --- Famille et enfance ---
        mot("enfant", "zaza", "child");
        mot("enfants", "ankizy", "children");
        mot("bébé", "zazakely", "baby");
        mot("bébés", "zazakely", "babies");
        mot("parent", "ray aman-dreny", "parent");
        mot("parents", "ray aman-dreny", "parents");
        mot("mère", "reny", "mother");
        mot("père", "ray", "father");
        mot("maman", "neny", "mum");
        mot("papa", "dada", "dad");
        mot("famille", "fianakaviana", "family");
        mot("familles", "fianakaviana", "families");
        mot("frère", "rahalahy", "brother");
        mot("sœur", "anabavy", "sister");
        mot("grand-mère", "renibe", "grandmother");
        mot("grand-père", "raibe", "grandfather");
        mot("fille", "zazavavy", "girl");
        mot("garçon", "zazalahy", "boy");

        // --- Vie quotidienne ---
        mot("maison", "trano", "home");
        mot("école", "sekoly", "school");
        mot("jour", "andro", "day");
        mot("jours", "andro", "days");
        mot("nuit", "alina", "night");
        mot("matin", "maraina", "morning");
        mot("soir", "hariva", "evening");
        mot("semaine", "herinandro", "week");
        mot("mois", "volana", "month");
        mot("année", "taona", "year");
        mot("temps", "fotoana", "time");
        mot("repas", "sakafo", "meal");
        mot("manger", "mihinana", "eat");
        mot("dormir", "matory", "sleep");
        mot("sommeil", "torimaso", "sleep");
        mot("jouer", "milalao", "play");
        mot("jeu", "lalao", "game");
        mot("jeux", "lalao", "games");
        mot("livre", "boky", "book");
        mot("livres", "boky", "books");
        mot("histoire", "tantara", "story");
        mot("chanson", "hira", "song");
        mot("eau", "rano", "water");
        mot("lait", "ronono", "milk");

        // --- Émotions et accompagnement ---
        mot("amour", "fitiavana", "love");
        mot("joie", "hafaliana", "joy");
        mot("peur", "tahotra", "fear");
        mot("colère", "hatezerana", "anger");
        mot("tristesse", "alahelo", "sadness");
        mot("émotion", "fihetseham-po", "emotion");
        mot("émotions", "fihetseham-po", "emotions");
        mot("patience", "faharetana", "patience");
        mot("confiance", "fahatokisana", "trust");
        mot("courage", "herim-po", "courage");
        mot("calme", "milamina", "calm");
        mot("fatigue", "havizanana", "tiredness");
        mot("santé", "fahasalamana", "health");
        mot("soutien", "fanohanana", "support");
        mot("conseil", "torohevitra", "advice");
        mot("conseils", "torohevitra", "advice");
        mot("aide", "fanampiana", "help");
        mot("question", "fanontaniana", "question");
        mot("questions", "fanontaniana", "questions");
        mot("réponse", "valiny", "answer");
        mot("expérience", "traikefa", "experience");
        mot("apprendre", "mianatra", "learn");
        mot("comprendre", "mahazo", "understand");
        mot("écouter", "mihaino", "listen");
        mot("partager", "mizara", "share");
        mot("parler", "miresaka", "talk");
        mot("grandir", "mitombo", "grow");

        // --- Vie de la plateforme ---
        mot("bonjour", "salama", "hello");
        mot("merci", "misaotra", "thank you");
        mot("bienvenue", "tongasoa", "welcome");
        mot("communauté", "vondrona", "community");
        mot("atelier", "atrikasa", "workshop");
        mot("ateliers", "atrikasa", "workshops");
        mot("conférence", "kaonferansa", "conference");
        mot("conférences", "kaonferansa", "conferences");
        mot("évènement", "hetsika", "event");
        mot("évènements", "hetsika", "events");
        mot("ressource", "loharano", "resource");
        mot("ressources", "loharano", "resources");
        mot("publication", "lahatsoratra", "post");
        mot("publications", "lahatsoratra", "posts");
        mot("message", "hafatra", "message");
        mot("messages", "hafatra", "messages");
        mot("commentaire", "hevitra", "comment");
        mot("commentaires", "hevitra", "comments");
        mot("groupe", "vondrona", "group");
        mot("éducatrice", "mpanabe", "educator");
        mot("psychologue", "psikologa", "psychologist");
        mot("activité", "hetsika", "activity");
        mot("activités", "hetsika", "activities");

        // --- Mots-outils fréquents ---
        mot("et", "sy", "and");
        mot("ou", "na", "or");
        mot("avec", "miaraka amin'ny", "with");
        mot("sans", "tsy misy", "without");
        mot("pour", "ho an'ny", "for");
        mot("dans", "ao anatin'ny", "in");
        mot("sur", "amin'ny", "on");
        mot("mais", "fa", "but");
        mot("très", "tena", "very");
        mot("beaucoup", "betsaka", "a lot");
        mot("peu", "kely", "little");
        mot("bien", "tsara", "well");
        mot("mal", "ratsy", "badly");
        mot("bon", "tsara", "good");
        mot("bonne", "tsara", "good");
        mot("grand", "lehibe", "big");
        mot("petit", "kely", "small");
        mot("nouveau", "vaovao", "new");
        mot("premier", "voalohany", "first");
        mot("toujours", "foana", "always");
        mot("jamais", "tsy mihitsy", "never");
        mot("aujourd'hui", "androany", "today");
        mot("demain", "rahampitso", "tomorrow");
        mot("hier", "omaly", "yesterday");
        mot("oui", "eny", "yes");
        mot("non", "tsia", "no");
    }

    /** Normalise un mot pour la recherche : minuscules, accents retirés. */
    public static String cle(String mot) {
        if (mot == null) return "";
        String base = java.text.Normalizer.normalize(mot.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD);
        return base.replaceAll("\\p{M}", "").trim();
    }

    /**
     * Traduit un mot d'une langue vers une autre, ou renvoie null si le mot
     * n'est pas au lexique. Le passage se fait toujours par le français, qui
     * est la langue de référence de la plateforme.
     */
    public static String traduireMot(String mot, String depuis, String vers) {
        if (mot == null || mot.isBlank() || depuis.equals(vers)) return mot;

        String fr = "fr".equals(depuis) ? mot : VERS_FR.get(cle(mot));
        if (fr == null) return null;
        if ("fr".equals(vers)) return fr;

        String[] paire = LEXIQUE.get(cle(fr));
        if (paire == null) return null;
        return "mg".equals(vers) ? paire[0] : paire[1];
    }

    public static int taille() { return LEXIQUE.size(); }
}
