package com.parentplatform.controller;

import com.parentplatform.api.ApiRoutes;
import com.parentplatform.config.DatabaseMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Porte d'entrée unique de l'API Parentia.
 *
 *  - GET /api        : sommaire de tous les modules (une seule route donne accès à tout)
 *  - GET /api/health : test rapide, sans base de données — le frontend s'en sert pour
 *                      décider s'il travaille en local ou en ligne
 *  - GET /api/status : mode de base de données actif et informations d'exécution
 */
@RestController
@RequestMapping(ApiRoutes.BASE)
public class ApiGatewayController {

    @Autowired
    private DatabaseMode databaseMode;

    @Value("${spring.application.name:parentplatform}")
    private String appName;

    @Value("${server.port:8082}")
    private String port;

    /** Sommaire de l'API : toutes les routes de la plateforme en un seul appel. */
    @GetMapping
    public ResponseEntity<?> index() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("application", "Parentia API");
        body.put("description", "Plateforme communautaire pour le bien-être des enfants — Madagascar");
        body.put("routes", ApiRoutes.map());
        body.put("database", databaseSummary());
        return ResponseEntity.ok(body);
    }

    /**
     * Sonde de disponibilité. Volontairement sans accès base : elle doit répondre
     * en quelques millisecondes pour que le frontend choisisse vite sa cible.
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("status", "UP");
        body.put("application", appName);
        body.put("port", port);
        body.put("time", LocalDateTime.now().toString());
        return ResponseEntity.ok(body);
    }

    /** État détaillé : quelle base est utilisée, et pourquoi. */
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("status", "UP");
        body.put("database", databaseSummary());
        body.put("routes", ApiRoutes.map());
        body.put("time", LocalDateTime.now().toString());
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> databaseSummary() {
        Map<String, Object> db = new LinkedHashMap<>();
        db.put("mode", databaseMode.getMode());
        db.put("label", databaseMode.getLabel());
        db.put("host", databaseMode.getHost());
        db.put("fallback", databaseMode.isFallback());
        db.put("detail", databaseMode.getDetail());
        db.put("resolvedAt", databaseMode.getResolvedAt().toString());
        return db;
    }
}
