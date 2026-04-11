package com.parentplatform.repository;

import com.parentplatform.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByOwnerId(Long ownerId);

    @Query("SELECT r FROM Resource r WHERE r.shared = true AND r.ownerId != :userId")
    List<Resource> findSharedResources(@Param("userId") Long userId);

    @Query("SELECT r FROM Resource r WHERE " +
            "(:type IS NULL OR r.type = :type) AND " +
            "(:age IS NULL OR r.age LIKE CONCAT('%', CAST(:age AS text), '%')) AND " +
            "(:search IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))")
    List<Resource> searchResources(@Param("type") String type,
                                   @Param("age") String age,
                                   @Param("search") String search);

    @Query(value = "SELECT * FROM resources ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Resource findRandomResource();
}