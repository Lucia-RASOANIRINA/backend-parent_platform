package com.parentplatform.service;

import com.parentplatform.model.Message;
import com.parentplatform.model.User;
import com.parentplatform.model.Conversation;
import com.parentplatform.repository.MessageRepository;
import com.parentplatform.repository.ConversationRepository;
import com.parentplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Message sendMessage(Message message) {
        Message savedMessage = messageRepository.save(message);
        updateConversation(message.getSender(), message.getReceiver(), message.getContenu());
        return savedMessage;
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        conversationRepository.deleteById(conversationId);
    }

    private void updateConversation(User sender, User receiver, String lastMessage) {
        Optional<Conversation> existingConv = conversationRepository.findByUser1AndUser2(sender, receiver);
        if (existingConv.isEmpty()) {
            existingConv = conversationRepository.findByUser2AndUser1(sender, receiver);
        }

        Conversation conversation;
        if (existingConv.isPresent()) {
            conversation = existingConv.get();
        } else {
            conversation = new Conversation();
            conversation.setUser1(sender);
            conversation.setUser2(receiver);
        }

        conversation.setLastMessage(lastMessage);
        conversation.setLastMessageTime(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        if (conversation.getUser1().getId().equals(receiver.getId())) {
            conversation.setUser1UnreadCount(conversation.getUser1UnreadCount() + 1);
        } else {
            conversation.setUser2UnreadCount(conversation.getUser2UnreadCount() + 1);
        }

        conversationRepository.save(conversation);
    }

    public List<Message> getConversation(Long userId1, Long userId2) {
        Optional<User> user1 = userService.findById(userId1);
        Optional<User> user2 = userService.findById(userId2);
        if (user1.isPresent() && user2.isPresent()) {
            List<Message> messages1 = messageRepository.findBySenderAndReceiverOrderByCreatedAtAsc(user1.get(), user2.get());
            List<Message> messages2 = messageRepository.findBySenderAndReceiverOrderByCreatedAtAsc(user2.get(), user1.get());
            List<Message> allMessages = new ArrayList<>();
            allMessages.addAll(messages1);
            allMessages.addAll(messages2);
            allMessages.sort((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()));
            return allMessages;
        }
        return List.of();
    }

    @Transactional
    public void markMessagesAsRead(Long currentUserId, Long otherUserId) {
        Optional<User> currentUser = userService.findById(currentUserId);
        Optional<User> otherUser = userService.findById(otherUserId);
        if (currentUser.isPresent() && otherUser.isPresent()) {
            messageRepository.markMessagesAsRead(currentUser.get(), otherUser.get());
            resetUnreadCount(currentUser.get(), otherUser.get());
        }
    }

    @Transactional
    public void markMessagesAsUnread(Long currentUserId, Long otherUserId) {
        Optional<User> currentUser = userService.findById(currentUserId);
        Optional<User> otherUser = userService.findById(otherUserId);
        if (currentUser.isPresent() && otherUser.isPresent()) {
            messageRepository.markMessagesAsUnread(currentUser.get(), otherUser.get());
            // Utiliser la même méthode que dans getConversationsList
            int unreadCount = messageRepository.countUnreadMessagesWithoutReply(currentUser.get(), otherUser.get());
            updateUnreadCount(currentUser.get(), otherUser.get(), unreadCount);
        }
    }

    private void updateUnreadCount(User currentUser, User otherUser, int unreadCount) {
        Optional<Conversation> conv = conversationRepository.findByUser1AndUser2(currentUser, otherUser);
        if (conv.isEmpty()) {
            conv = conversationRepository.findByUser2AndUser1(currentUser, otherUser);
        }
        conv.ifPresent(conversation -> {
            if (conversation.getUser1().getId().equals(currentUser.getId())) {
                conversation.setUser1UnreadCount(unreadCount);
            } else {
                conversation.setUser2UnreadCount(unreadCount);
            }
            conversationRepository.save(conversation);
        });
    }

    private void resetUnreadCount(User currentUser, User otherUser) {
        updateUnreadCount(currentUser, otherUser, 0);
    }

    // Nouvelle méthode avec filtrage
    public Map<String, Object> getConversationsList(Long userId, String filter) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> conversationList = new ArrayList<>();

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                response.put("conversations", conversationList);
                response.put("success", true);
                return response;
            }

            User user = userOpt.get();
            List<Conversation> conversations = conversationRepository.findConversationsByUser(user);

            for (Conversation conv : conversations) {
                User otherUser = conv.getUser1().getId().equals(userId) ? conv.getUser2() : conv.getUser1();

                Message lastMsg = messageRepository.findLastMessageBetweenUsers(user, otherUser, PageRequest.of(0, 1)).stream().findFirst().orElse(null);
                // NOUVEAU compteur : seulement les messages reçus après le dernier message envoyé par l'utilisateur
                int unreadCount = messageRepository.countUnreadMessagesWithoutReply(user, otherUser);

                Map<String, Object> convMap = new HashMap<>();
                convMap.put("id", conv.getId());
                convMap.put("otherUser", Map.of("id", otherUser.getId(), "nom", otherUser.getNom(), "email", otherUser.getEmail(), "role", otherUser.getRole().name()));
                convMap.put("lastMessage", lastMsg != null ? lastMsg.getContenu() : "");
                convMap.put("lastMessageTime", lastMsg != null ? lastMsg.getCreatedAt() : null);
                convMap.put("unreadCount", unreadCount);
                convMap.put("lastMessageSenderId", lastMsg != null ? lastMsg.getSender().getId() : null);

                conversationList.add(convMap);
            }

            // CORRECTION : tri avec LocalDateTime
            conversationList.sort((a, b) -> {
                LocalDateTime d1 = (LocalDateTime) a.get("lastMessageTime");
                LocalDateTime d2 = (LocalDateTime) b.get("lastMessageTime");
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d2.compareTo(d1);
            });

            // Application du filtre
            List<Map<String, Object>> filtered = conversationList.stream()
                    .filter(conv -> applyFilter(conv, filter, userId))
                    .collect(Collectors.toList());

            response.put("conversations", filtered);
            response.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("conversations", conversationList);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    private boolean applyFilter(Map<String, Object> conv, String filter, Long userId) {
        int unread = (int) conv.get("unreadCount");
        Long lastSenderId = (Long) conv.get("lastMessageSenderId");
        boolean hasMessage = lastSenderId != null;

        switch (filter) {
            case "unread":
                return unread > 0;
            case "read":
                return unread == 0 && hasMessage;
            case "read_no_reply":
                return unread == 0 && hasMessage && lastSenderId != userId;
            case "read_with_reply":
                return unread == 0 && hasMessage && lastSenderId == userId;
            default:
                return true;
        }
    }

    // Compatibilité avec l'ancienne méthode (sans filtre)
    public Map<String, Object> getConversationsList(Long userId) {
        return getConversationsList(userId, "all");
    }
}