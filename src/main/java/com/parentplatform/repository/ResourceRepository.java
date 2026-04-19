package com.parentplatform.repository;

import com.parentplatform.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query("UPDATE Resource r SET r.likes = r.likes + 1 WHERE r.id = :id")
    void incrementLikes(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Resource r SET r.averageRating = :avg, r.totalRatings = :count WHERE r.id = :id")
    void updateRatingStats(@Param("id") Long id, @Param("avg") Double avg, @Param("count") Long count);

    @Query(value = "SELECT * FROM resources r WHERE " +
            "(:type IS NULL OR r.type = CAST(:type AS TEXT)) AND " +
            "(:age IS NULL OR r.age = CAST(:age AS TEXT)) AND " +
            "(:search IS NULL OR r.title ILIKE CONCAT('%', CAST(:search AS TEXT), '%') OR r.description ILIKE CONCAT('%', CAST(:search AS TEXT), '%')) AND " +
            "(:owner = 'mine' AND r.owner_id = CAST(:userId AS BIGINT) OR :owner = 'shared')",
            nativeQuery = true)
    List<Resource> findByFilters(@Param("type") String type,
                                 @Param("age") String age,
                                 @Param("search") String search,
                                 @Param("owner") String owner,
                                 @Param("userId") Long userId);
}