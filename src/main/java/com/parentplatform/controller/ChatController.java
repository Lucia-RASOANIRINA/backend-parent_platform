package com.parentplatform.controller;

import com.parentplatform.model.Message;
import com.parentplatform.model.User;
import com.parentplatform.service.MessageService;
import com.parentplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload Message message) {
        try {
            // Sauvegarder le message
            Message savedMessage = messageService.sendMessage(message);

            // Envoyer au receveur via WebSocket
            String receiverDestination = "/queue/messages";
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(savedMessage.getReceiver().getId()),
                    receiverDestination,
                    savedMessage
            );

            // Envoyer une confirmation à l'expéditeur
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(savedMessage.getSender().getId()),
                    receiverDestination,
                    savedMessage
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, Object> payload) {
        try {
            Long senderId = Long.valueOf(payload.get("senderId").toString());
            Long receiverId = Long.valueOf(payload.get("receiverId").toString());
            Boolean isTyping = (Boolean) payload.get("typing");

            Map<String, Object> typingData = new HashMap<>();
            typingData.put("senderId", senderId);
            typingData.put("typing", isTyping);

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(receiverId),
                    "/queue/typing",
                    typingData
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat.markRead")
    public void markAsRead(@Payload Map<String, Object> payload) {
        try {
            Long currentUserId = Long.valueOf(payload.get("currentUserId").toString());
            Long otherUserId = Long.valueOf(payload.get("otherUserId").toString());

            messageService.markMessagesAsRead(currentUserId, otherUserId);

            Map<String, Object> readData = new HashMap<>();
            readData.put("userId", currentUserId);
            readData.put("read", true);

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(otherUserId),
                    "/queue/read",
                    readData
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}