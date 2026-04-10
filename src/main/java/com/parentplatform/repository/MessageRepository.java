package com.parentplatform.repository;

import com.parentplatform.model.Message;
import com.parentplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySenderAndReceiverOrderByCreatedAtAsc(User sender, User receiver);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiver = :receiver AND m.sender = :sender")
    void markMessagesAsRead(@Param("receiver") User receiver, @Param("sender") User sender);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = false WHERE m.receiver = :receiver AND m.sender = :sender")
    void markMessagesAsUnread(@Param("receiver") User receiver, @Param("sender") User sender);

    // Compte les messages non lus reçus d'un utilisateur
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = :user AND m.sender = :otherUser AND m.isRead = false")
    int countUnreadMessagesFromOther(@Param("user") User user, @Param("otherUser") User otherUser);

    // NOUVEAU : compte les messages reçus APRÈS le dernier message envoyé par l'utilisateur courant (sans réponse)
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = :user AND m.sender = :otherUser AND m.isRead = false AND m.createdAt > COALESCE(" +
            "(SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.sender = :user AND m2.receiver = :otherUser), '1970-01-01')")
    int countUnreadMessagesWithoutReply(@Param("user") User user, @Param("otherUser") User otherUser);

    // Récupère le dernier message d'une conversation
    @Query("SELECT m FROM Message m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.createdAt DESC")
    List<Message> findLastMessageBetweenUsers(@Param("user1") User user1, @Param("user2") User user2, org.springframework.data.domain.Pageable pageable);
}