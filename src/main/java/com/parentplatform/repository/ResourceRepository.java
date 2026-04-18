package com.parentplatform.repository;

import com.parentplatform.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByOwnerId(Long ownerId);

    @Query("SELECT r FROM Resource r WHERE r.ownerId != :userId")
    List<Resource> findByOwnerIdNot(@Param("userId") Long userId);

    @Query("SELECT r FROM Resource r WHERE " +
            "(:type IS NULL OR r.type = :type) AND " +
            "(:age IS NULL OR r.age LIKE CONCAT('%', :age, '%')) AND " +
            "(:search IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Resource> searchResources(@Param("type") String type,
                                   @Param("age") String age,
                                   @Param("search") String search);

    Optional<Resource> findBySurpriseWeekStart(LocalDate weekStart);

    @Query(value = "SELECT * FROM resources ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Resource findRandomResource();
}