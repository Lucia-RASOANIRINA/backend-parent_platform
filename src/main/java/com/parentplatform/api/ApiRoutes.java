package com.parentplatform.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Point d'entrée unique des routes de l'API Parentia.
 *
 * Toutes les URL du backend sont déclarées ici : chaque contrôleur référence une
 * constante de cette classe plutôt qu'une chaîne écrite en dur. Un seul fichier
 * suffit donc pour savoir — et changer — où répond chaque module.
 *
 * Le frontend consomme la même carte via GET /api (voir ApiGatewayController),
 * ce qui garantit que les deux côtés parlent des mêmes routes.
 */
public final class ApiRoutes {

    private ApiRoutes() {}

    /** Préfixe commun à toute l'API. */
    public static final String BASE = "/api";

    // --- Modules ---
    public static final String AUTH = BASE + "/auth";
    public static final String ADMIN = BASE + "/admin";
    public static final String POSTS = BASE + "/posts";
    public static final String COMMENTS = BASE + "/comments";
    public static final String LIKES = BASE + "/likes";
    public static final String RESOURCES = BASE + "/resources";
    public static final String EVENEMENTS = BASE + "/evenements";
    public static final String CONFERENCES = EVENEMENTS + "/conferences";
    public static final String MESSAGES = BASE + "/messages";
    public static final String NOTIFICATIONS = BASE + "/notifications";
    public static final String STATS = BASE + "/stats";

    // --- Supervision (utilisées par le frontend pour choisir local ou en ligne) ---
    public static final String HEALTH = BASE + "/health";
    public static final String STATUS = BASE + "/status";

    /** WebSocket (chat temps réel). */
    public static final String WEBSOCKET = "/ws";

    /**
     * Carte lisible de l'API : module → chemin.
     * Sert de sommaire au frontend et de documentation vivante.
     */
    public static Map<String, String> map() {
        Map<String, String> routes = new LinkedHashMap<>();
        routes.put("auth", AUTH);
        routes.put("admin", ADMIN);
        routes.put("posts", POSTS);
        routes.put("comments", COMMENTS);
        routes.put("likes", LIKES);
        routes.put("resources", RESOURCES);
        routes.put("evenements", EVENEMENTS);
        routes.put("conferences", CONFERENCES);
        routes.put("messages", MESSAGES);
        routes.put("notifications", NOTIFICATIONS);
        routes.put("stats", STATS);
        routes.put("health", HEALTH);
        routes.put("status", STATUS);
        routes.put("websocket", WEBSOCKET);
        return routes;
    }
}
