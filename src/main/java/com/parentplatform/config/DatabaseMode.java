package com.parentplatform.config;

import java.time.LocalDateTime;

/**
 * Mémorise la base de données réellement utilisée par l'application.
 *
 * Parentia peut tourner sur deux bases :
 *  - « online » : la base hébergée (Neon.tech) utilisée par le déploiement Render ;
 *  - « local »  : le PostgreSQL de la machine du développeur.
 *
 * Le choix est fait une seule fois au démarrage par {@link DataSourceConfig},
 * puis exposé au frontend via /api/status pour afficher le mode actif.
 */
public class DatabaseMode {

    public static final String ONLINE = "online";
    public static final String LOCAL = "local";

    private String mode = ONLINE;
    private String url = "";
    private String host = "";
    private boolean fallback = false;
    private String detail = "";
    private final LocalDateTime resolvedAt = LocalDateTime.now();

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    /** true si la base préférée était injoignable et qu'on a basculé sur l'autre. */
    public boolean isFallback() { return fallback; }
    public void setFallback(boolean fallback) { this.fallback = fallback; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }

    public boolean isOnline() { return ONLINE.equals(mode); }

    /** Libellé lisible pour l'interface. */
    public String getLabel() {
        return isOnline() ? "Base en ligne (Neon)" : "Base locale (PostgreSQL)";
    }
}
