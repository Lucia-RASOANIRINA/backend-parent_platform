package com.parentplatform.repository;

import com.parentplatform.model.EvenementInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvenementInscriptionRepository extends JpaRepository<EvenementInscription, Long> {

    long countByEvenementId(Long evenementId);

    boolean existsByEvenementIdAndUserId(Long evenementId, Long userId);

    Optional<EvenementInscription> findByEvenementIdAndUserId(Long evenementId, Long userId);

    List<EvenementInscription> findByUserId(Long userId);

    List<EvenementInscription> findByEvenementId(Long evenementId);

    void deleteByEvenementId(Long evenementId);
}
