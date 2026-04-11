package com.parentplatform.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 10000)
    private String fullContent;

    @Column(nullable = false)
    private String type; // fiche, video, pdf, bloc

    private String age;

    private String thumbnail;

    private int likes = 0;

    private Double averageRating = 0.0;

    private Integer totalRatings = 0;

    private Long ownerId;

    private boolean shared = false;

    private LocalDate surpriseWeekStart; // début de semaine où c'est la surprise

    @ElementCollection
    @CollectionTable(name = "resource_preview_blocks", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "block_name")
    private List<String> previewBlocks = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters et setters (à générer)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFullContent() { return fullContent; }
    public void setFullContent(String fullContent) { this.fullContent = fullContent; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public Integer getTotalRatings() { return totalRatings; }
    public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public boolean isShared() { return shared; }
    public void setShared(boolean shared) { this.shared = shared; }
    public LocalDate getSurpriseWeekStart() { return surpriseWeekStart; }
    public void setSurpriseWeekStart(LocalDate surpriseWeekStart) { this.surpriseWeekStart = surpriseWeekStart; }
    public List<String> getPreviewBlocks() { return previewBlocks; }
    public void setPreviewBlocks(List<String> previewBlocks) { this.previewBlocks = previewBlocks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}