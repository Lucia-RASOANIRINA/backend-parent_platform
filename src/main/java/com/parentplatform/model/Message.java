package com.parentplatform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "is_read")
    private boolean isRead = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** TEXT, IMAGE, FILE, AUDIO (message vocal) */
    @Column(name = "message_type")
    private String messageType = "TEXT";

    @Column(columnDefinition = "TEXT")
    private String fileData;

    private String fileName;

    /** Type MIME de la pièce jointe (image/png, audio/webm…). */
    @Column(name = "file_type")
    private String fileType;

    /** Durée en secondes pour un message vocal. */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /** true si l'auteur a modifié son message (affiché « modifié »). */
    private Boolean modifie = Boolean.FALSE;

    /** Suppression douce : le message reste dans le fil, signalé comme retiré. */
    private Boolean supprime = Boolean.FALSE;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Message() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public boolean isModifie() { return Boolean.TRUE.equals(modifie); }
    public void setModifie(boolean modifie) { this.modifie = modifie; }

    public boolean isSupprime() { return Boolean.TRUE.equals(supprime); }
    public void setSupprime(boolean supprime) { this.supprime = supprime; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}