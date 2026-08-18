package com.parentplatform.controller;

import com.parentplatform.api.ApiRoutes;
import com.parentplatform.model.Traduction;
import com.parentplatform.service.TraductionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/** Lecture des traductions produites automatiquement pour les contenus des membres. */
@RestController
@RequestMapping(ApiRoutes.BASE + "/traductions")
public class TraductionController {

    @Autowired private TraductionService traductionService;

    /** Traduction d'un contenu unique. */
    @GetMapping("/{type}/{id}")
    public ResponseEntity<?> une(@PathVariable String type, @PathVariable Long id,
                                 @RequestParam String langue) {
        return traductionService.lire(type.toUpperCase(), id, langue)
                .<ResponseEntity<?>>map(t -> ResponseEntity.ok(Map.of("success", true, "traduction", enJson(t))))
                .orElseGet(() -> {
                    Map<String, Object> vide = new LinkedHashMap<>();
                    vide.put("success", true);
                    vide.put("traduction", null);
                    return ResponseEntity.ok(vide);
                });
    }

    /**
     * Traductions d'un lot de contenus : un fil de publications se traduit en
     * une requête plutôt qu'une par carte.
     */
    @GetMapping("/{type}")
    public ResponseEntity<?> lot(@PathVariable String type,
                                 @RequestParam String langue,
                                 @RequestParam String ids) {
        List<Long> identifiants = Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::valueOf).collect(Collectors.toList());

        Map<Long, Traduction> trouvees = traductionService.lireLot(type.toUpperCase(), identifiants, langue);
        Map<String, Object> parId = new LinkedHashMap<>();
        trouvees.forEach((id, t) -> parId.put(String.valueOf(id), enJson(t)));
        return ResponseEntity.ok(Map.of("success", true, "traductions", parId));
    }

    private Map<String, Object> enJson(Traduction t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("langue", t.getLangue());
        m.put("texte", t.getTexte());
        m.put("motsInconnus", t.getMotsInconnus() == null || t.getMotsInconnus().isBlank()
                ? List.of() : List.of(t.getMotsInconnus().split("\\|")));
        return m;
    }
}
