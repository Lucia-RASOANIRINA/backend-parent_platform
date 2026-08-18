package com.parentplatform.api;

import java.util.List;

/**
 * Référentiel géographique de Madagascar utilisé par les filtres de la plateforme
 * (évènements, conférences, annuaire des professionnels).
 */
public final class Madagascar {

    private Madagascar() {}

    /** Les 23 régions administratives (découpage en vigueur depuis 2021). */
    public static final List<String> REGIONS = List.of(
            "Alaotra-Mangoro", "Amoron'i Mania", "Analamanga", "Analanjirofo", "Androy",
            "Anosy", "Atsimo-Andrefana", "Atsimo-Atsinanana", "Atsinanana", "Betsiboka",
            "Boeny", "Bongolava", "Diana", "Fitovinany", "Haute Matsiatra",
            "Ihorombe", "Itasy", "Melaky", "Menabe", "Sava",
            "Sofia", "Vakinankaratra", "Vatovavy"
    );

    /** Principales villes, proposées en suggestion dans les formulaires. */
    public static final List<String> VILLES = List.of(
            "Antananarivo", "Toamasina", "Antsirabe", "Fianarantsoa", "Mahajanga",
            "Toliara", "Antsiranana", "Ambovombe", "Morondava", "Manakara",
            "Sambava", "Ambatondrazaka", "Moramanga", "Farafangana", "Tolagnaro"
    );

    /** Langues gérées par la plateforme. */
    public static final List<String> LANGUES = List.of("fr", "mg", "en");
}
