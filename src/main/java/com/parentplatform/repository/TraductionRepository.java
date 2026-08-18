package com.parentplatform.repository;

import com.parentplatform.model.Traduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TraductionRepository extends JpaRepository<Traduction, Long> {

    Optional<Traduction> findByTypeContenuAndContenuIdAndLangue(String typeContenu, Long contenuId, String langue);

    List<Traduction> findByTypeContenuAndContenuId(String typeContenu, Long contenuId);

    /** Chargement groupé : une seule requête pour tout un fil. */
    @Query("SELECT t FROM Traduction t WHERE t.typeContenu = :type AND t.langue = :langue AND t.contenuId IN :ids")
    List<Traduction> findLot(@Param("type") String type, @Param("langue") String langue, @Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    void deleteByTypeContenuAndContenuId(String typeContenu, Long contenuId);
}
