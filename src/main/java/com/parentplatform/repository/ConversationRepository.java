package com.parentplatform.repository;

import com.parentplatform.model.Conversation;
import com.parentplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUser1AndUser2(User user1, User user2);

    Optional<Conversation> findByUser2AndUser1(User user1, User user2);

    @Query("SELECT c FROM Conversation c WHERE c.user1 = :user OR c.user2 = :user ORDER BY c.updatedAt DESC")
    List<Conversation> findConversationsByUser(@Param("user") User user);
}