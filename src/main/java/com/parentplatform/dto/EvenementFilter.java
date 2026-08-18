package com.parentplatform.dto;

import java.time.LocalDate;

/**
 * Critères de recherche de l'espace évènements / conférences.
 *
 * Tous les champs sont facultatifs : un champ nul signifie « ne pas filtrer ».
 * Le filtrage est fait en mémoire côté service : le volume d'évènements d'une
 * communauté reste modeste, et cela évite des requêtes SQL à paramètres
 * optionnels difficiles à maintenir.
 */
public class EvenementFilter {

    /** Recherche libre : titre, description, animateur, lieu, mots-clés. */
    public String q;

    /** atelier | conference | rencontre | webinaire */
    public String type;

    /** Région de Madagascar. */
    public String region;

    /** Ville / commune. */
    public String ville;

    /** fr | mg | en */
    public String langue;

    /** true = uniquement en ligne, false = uniquement en présentiel, null = les deux. */
    public Boolean online;

    /** A_VENIR | AUJOURDHUI | TERMINE | ANNULE */
    public String statut;

    /** true = uniquement les évènements avec des places libres. */
    public Boolean placesDisponibles;

    /** true = uniquement les évènements gratuits. */
    public Boolean gratuit;

    /** Évènements auxquels l'utilisateur est inscrit. */
    public Boolean mesInscriptions;

    /** Évènements créés par l'utilisateur. */
    public Boolean mesCreations;

    public LocalDate du;
    public LocalDate au;

    /** date | recent | populaire | titre */
    public String tri = "date";

    /** true pour inclure les évènements non publiés (administration). */
    public boolean inclureNonPublies = false;
}
