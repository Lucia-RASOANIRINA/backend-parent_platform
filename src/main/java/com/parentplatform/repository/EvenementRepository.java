package com.parentplatform.repository;

import com.parentplatform.model.Evenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EvenementRepository extends JpaRepository<Evenement, Long> {

    List<Evenement> findAllByOrderByDateAsc();

    List<Evenement> findByPublieTrueOrderByDateAsc();

    List<Evenement> findByDateGreaterThanEqualAndPublieTrueOrderByDateAsc(LocalDate date);

    List<Evenement> findByCreatedByIdOrderByDateAsc(Long createdById);
}
