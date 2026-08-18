package com.parentplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Traduction par un service de traduction automatique.
 *
 * Le glossaire interne rendait un mot-à-mot difficilement lisible. On s'appuie
 * donc sur un vrai moteur, en gardant le glossaire comme filet : la plateforme
 * doit continuer de fonctionner quand la connexion tombe, ce qui arrive
 * régulièrement à Madagascar.
 *
 * Trois fournisseurs sont prévus, choisis dans application.properties :
 *
 *   mymemory        API publique gratuite, sans clé, ~5 000 mots/jour.
 *                   Bonne qualité fr↔en, correcte fr↔mg. C'est le défaut.
 *   google          Cloud Translation. Meilleure qualité en malgache ;
 *                   demande une clé, avec 500 000 caractères offerts par mois.
 *   libretranslate  Instance auto-hébergée. Ne gère pas encore le malgache.
 */
@Service
public class TraducteurExterne {

    private static final Logger log = LoggerFactory.getLogger(TraducteurExterne.class);

    @Value("${app.traduction.fournisseur:mymemory}")
    private String fournisseur;

    @Value("${app.traduction.google.cle:}")
    private String cleGoogle;

    @Value("${app.traduction.libretranslate.url:}")
    private String urlLibre;

    /** Adresse déclarée à MyMemory : elle relève le quota de 5 000 à 50 000 mots/jour. */
    @Value("${app.traduction.mymemory.email:}")
    private String emailMyMemory;

    @Value("${app.traduction.timeout-ms:6000}")
    private int timeoutMs;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();
    private final ObjectMapper json = new ObjectMapper();

    /**
     * Traduit un texte, ou renvoie null si le service n'a rien pu produire.
     * Un null n'est pas une erreur : l'appelant retombe sur le glossaire.
     */
    public String traduire(String texte, String depuis, String vers) {
        if (texte == null || texte.isBlank() || depuis.equals(vers)) return texte;
        try {
            return switch (fournisseur.toLowerCase()) {
                case "google" -> viaGoogle(texte, depuis, vers);
                case "libretranslate" -> viaLibreTranslate(texte, depuis, vers);
                case "aucun" -> null;
                default -> viaMyMemory(texte, depuis, vers);
            };
        } catch (Exception e) {
            // Réseau coupé, quota atteint, service indisponible : on n'insiste pas
            log.info("Traduction externe indisponible ({}), repli sur le glossaire : {}",
                    fournisseur, e.getMessage());
            return null;
        }
    }

    public boolean estActif() {
        return !"aucun".equalsIgnoreCase(fournisseur);
    }

    public String fournisseurActuel() { return fournisseur; }

    // --- MyMemory : gratuit, sans clé ---
    private String viaMyMemory(String texte, String depuis, String vers) throws Exception {
        // Le service limite chaque appel : on ne lui envoie pas de pavé
        String extrait = texte.length() > 480 ? texte.substring(0, 480) : texte;
        StringBuilder url = new StringBuilder("https://api.mymemory.translated.net/get?q=")
                .append(encode(extrait))
                .append("&langpair=").append(depuis).append("%7C").append(vers);
        if (!emailMyMemory.isBlank()) url.append("&de=").append(encode(emailMyMemory));

        JsonNode racine = json.readTree(appeler(url.toString()));
        if (racine.path("responseStatus").asInt() != 200) return null;
        if (racine.path("quotaFinished").asBoolean(false)) {
            log.warn("Quota MyMemory atteint pour aujourd'hui : repli sur le glossaire.");
            return null;
        }
        String resultat = racine.path("responseData").path("translatedText").asText(null);
        return valable(resultat, texte) ? resultat : null;
    }

    // --- Google Cloud Translation : clé requise, 500 000 caractères offerts par mois ---
    private String viaGoogle(String texte, String depuis, String vers) throws Exception {
        if (cleGoogle.isBlank()) return null;
        String url = "https://translation.googleapis.com/language/translate/v2"
                + "?key=" + encode(cleGoogle)
                + "&q=" + encode(texte)
                + "&source=" + depuis + "&target=" + vers + "&format=text";
        JsonNode racine = json.readTree(appeler(url));
        JsonNode traductions = racine.path("data").path("translations");
        if (!traductions.isArray() || traductions.isEmpty()) return null;
        String resultat = traductions.get(0).path("translatedText").asText(null);
        return valable(resultat, texte) ? resultat : null;
    }

    // --- LibreTranslate : instance auto-hébergée ---
    private String viaLibreTranslate(String texte, String depuis, String vers) throws Exception {
        if (urlLibre.isBlank()) return null;
        String corps = json.createObjectNode()
                .put("q", texte).put("source", depuis).put("target", vers).put("format", "text")
                .toString();
        HttpRequest requete = HttpRequest.newBuilder(URI.create(urlLibre.replaceAll("/$", "") + "/translate"))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corps, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> reponse = client.send(requete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (reponse.statusCode() != 200) return null;
        String resultat = json.readTree(reponse.body()).path("translatedText").asText(null);
        return valable(resultat, texte) ? resultat : null;
    }

    private String appeler(String url) throws Exception {
        HttpRequest requete = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> reponse = client.send(requete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (reponse.statusCode() != 200) throw new IllegalStateException("HTTP " + reponse.statusCode());
        return reponse.body();
    }

    /** Certains services renvoient un message d'erreur en guise de traduction. */
    private boolean valable(String resultat, String source) {
        if (resultat == null || resultat.isBlank()) return false;
        String majuscules = resultat.toUpperCase();
        if (majuscules.contains("MYMEMORY WARNING") || majuscules.contains("QUERY LENGTH LIMIT")) return false;
        return !resultat.equalsIgnoreCase(source);
    }

    private String encode(String valeur) {
        return URLEncoder.encode(valeur, StandardCharsets.UTF_8);
    }
}
