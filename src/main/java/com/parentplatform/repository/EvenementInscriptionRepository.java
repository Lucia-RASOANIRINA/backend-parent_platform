package com.parentplatform.repository;

import com.parentplatform.model.EvenementInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvenementInscriptionRepository extends JpaRepository<EvenementInscription, Long> {

    /**
     * Nombre d'inscrits de tous les évènements en une seule requête.
     * Évite une requête par évènement lors de l'affichage des listes : avec une
     * base distante, chaque aller-retour coûte cher.
     */
    @Query("SELECT i.evenementId, COUNT(i) FROM EvenementInscription i GROUP BY i.evenementId")
    List<Object[]> compterParEvenement();

    /** Identifiants des évènements auxquels un utilisateur est inscrit. */
    @Query("SELECT i.evenementId FROM EvenementInscription i WHERE i.userId = :userId")
    List<Long> evenementsDeLUtilisateur(@Param("userId") Long userId);

    long countByEvenementId(Long evenementId);

    boolean existsByEvenementIdAndUserId(Long evenementId, Long userId);

    Optional<EvenementInscription> findByEvenementIdAndUserId(Long evenementId, Long userId);

    List<EvenementInscription> findByUserId(Long userId);

    List<EvenementInscription> findByEvenementId(Long evenementId);

    void deleteByEvenementId(Long evenementId);
}
