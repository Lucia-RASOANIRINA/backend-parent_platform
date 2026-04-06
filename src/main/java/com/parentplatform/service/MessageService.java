package com.parentplatform.service;

import com.parentplatform.model.Message;
import com.parentplatform.model.User;
import com.parentplatform.model.Conversation;
import com.parentplatform.repository.MessageRepository;
import com.parentplatform.repository.ConversationRepository;
import com.parentplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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

        // Mettre à jour ou créer la conversation
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

        // Incrémenter le compteur de non-lus pour le receveur
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
            // Récupérer les messages dans les deux sens
            List<Message> messages1 = messageRepository.findBySenderAndReceiverOrderByCreatedAtAsc(user1.get(), user2.get());
            List<Message> messages2 = messageRepository.findBySenderAndReceiverOrderByCreatedAtAsc(user2.get(), user1.get());

            // Fusionner les deux listes
            List<Message> allMessages = new ArrayList<>();
            allMessages.addAll(messages1);
            allMessages.addAll(messages2);

            // Trier par date
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

    private void resetUnreadCount(User currentUser, User otherUser) {
        Optional<Conversation> conv = conversationRepository.findByUser1AndUser2(currentUser, otherUser);
        if (conv.isEmpty()) {
            conv = conversationRepository.findByUser2AndUser1(currentUser, otherUser);
        }

        if (conv.isPresent()) {
            Conversation conversation = conv.get();
            if (conversation.getUser1().getId().equals(currentUser.getId())) {
                conversation.setUser1UnreadCount(0);
            } else {
                conversation.setUser2UnreadCount(0);
            }
            conversationRepository.save(conversation);
        }
    }

    public Map<String, Object> getConversationsList(Long userId) {
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

            // Récupérer toutes les conversations où l'utilisateur est impliqué
            List<Conversation> conversations = conversationRepository.findConversationsByUser(user);

            for (Conversation conv : conversations) {
                Map<String, Object> convMap = new HashMap<>();
                convMap.put("id", conv.getId());

                // Déterminer l'autre utilisateur
                User otherUser = conv.getUser1().getId().equals(userId) ? conv.getUser2() : conv.getUser1();

                Map<String, Object> otherUserMap = new HashMap<>();
                otherUserMap.put("id", otherUser.getId());
                otherUserMap.put("nom", otherUser.getNom());
                otherUserMap.put("email", otherUser.getEmail());
                otherUserMap.put("role", otherUser.getRole().name());
                convMap.put("otherUser", otherUserMap);

                convMap.put("lastMessage", conv.getLastMessage() != null ? conv.getLastMessage() : "");
                convMap.put("lastMessageTime", conv.getLastMessageTime());

                int unreadCount = conv.getUser1().getId().equals(userId) ?
                        conv.getUser1UnreadCount() : conv.getUser2UnreadCount();
                convMap.put("unreadCount", unreadCount);

                conversationList.add(convMap);
            }

            response.put("conversations", conversationList);
            response.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("conversations", conversationList);
            response.put("success", true);
            response.put("error", e.getMessage());
        }

        return response;
    }
}