package com.parentplatform.controller;

import com.parentplatform.api.ApiRoutes;
import com.parentplatform.model.Message;
import com.parentplatform.model.User;
import com.parentplatform.service.MessageService;
import com.parentplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(ApiRoutes.MESSAGES)
public class MessageRestController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @GetMapping("/conversation/{userId1}/{userId2}")
    public ResponseEntity<?> getConversation(@PathVariable Long userId1, @PathVariable Long userId2) {
        try {
            List<Message> messages = messageService.getConversation(userId1, userId2);
            List<Map<String, Object>> formattedMessages = new ArrayList<>();
            for (Message msg : messages) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", msg.getId());
                m.put("contenu", msg.getContenu());
                m.put("createdAt", msg.getCreatedAt());
                m.put("isRead", msg.isRead());
                m.put("messageType", msg.getMessageType());
                m.put("fileData", msg.getFileData());
                m.put("fileName", msg.getFileName());
                m.put("fileType", msg.getFileType());
                m.put("durationSeconds", msg.getDurationSeconds());
                m.put("modifie", msg.isModifie());
                m.put("supprime", msg.isSupprime());

                Map<String, Object> sender = new HashMap<>();
                if (msg.getSender() != null) {
                    sender.put("id", msg.getSender().getId());
                    sender.put("nom", msg.getSender().getNom());
                }
                m.put("sender", sender);
                formattedMessages.add(m);
            }
            return ResponseEntity.ok(Map.of("messages", formattedMessages, "success", true));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    @GetMapping("/conversations/{userId}")
    public ResponseEntity<?> getConversations(@PathVariable Long userId,
                                              @RequestParam(required = false, defaultValue = "all") String filter) {
        try {
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found", "success", false));
            }
            Map<String, Object> result = messageService.getConversationsList(userId, filter);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    @PostMapping("/mark-read")
    public ResponseEntity<?> markConversationAsRead(@RequestBody Map<String, Long> request) {
        try {
            Long userId = request.get("userId");
            Long otherUserId = request.get("otherUserId");
            messageService.markMessagesAsRead(userId, otherUserId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Messages marqués comme lus"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/mark-unread")
    public ResponseEntity<?> markConversationAsUnread(@RequestBody Map<String, Long> request) {
        try {
            Long userId = request.get("userId");
            Long otherUserId = request.get("otherUserId");
            messageService.markMessagesAsUnread(userId, otherUserId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Messages marqués comme non lus"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Modification d'un message déjà envoyé (texte uniquement, par son auteur). */
    @PutMapping("/{messageId}")
    public ResponseEntity<?> modifierMessage(@PathVariable Long messageId,
                                             @RequestBody Map<String, String> payload,
                                             @RequestHeader("X-User-Id") Long userId) {
        try {
            Message message = messageService.findById(messageId);
            if (message == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Message introuvable"));
            }
            if (message.getSender() == null || !message.getSender().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Vous n'êtes pas l'auteur de ce message"));
            }
            if (message.isSupprime()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Ce message a été supprimé"));
            }
            if (!"TEXT".equals(message.getMessageType())) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Seuls les messages texte sont modifiables"));
            }
            String contenu = payload.get("contenu");
            if (contenu == null || contenu.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Le message ne peut pas être vide"));
            }
            Message enregistre = messageService.modifier(message, contenu.trim());
            return ResponseEntity.ok(Map.of("success", true, "message", Map.of(
                    "id", enregistre.getId(),
                    "contenu", enregistre.getContenu(),
                    "modifie", enregistre.isModifie())));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Retrait d'un message : il reste dans le fil, signalé comme supprimé. */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> supprimerMessage(@PathVariable Long messageId,
                                              @RequestHeader("X-User-Id") Long userId) {
        try {
            Message message = messageService.findById(messageId);
            if (message == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Message introuvable"));
            }
            if (message.getSender() == null || !message.getSender().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Vous n'êtes pas l'auteur de ce message"));
            }
            messageService.supprimer(message);
            return ResponseEntity.ok(Map.of("success", true, "message", "Message supprimé"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<?> deleteConversation(@PathVariable Long conversationId) {
        try {
            messageService.deleteConversation(conversationId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Conversation supprimée"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}