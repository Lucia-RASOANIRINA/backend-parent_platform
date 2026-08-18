package com.parentplatform.controller;

import com.parentplatform.api.ApiRoutes;
import com.parentplatform.model.Comment;
import com.parentplatform.model.Post;
import com.parentplatform.model.User;
import com.parentplatform.service.CommentService;
import com.parentplatform.service.PostService;
import com.parentplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(ApiRoutes.COMMENTS)
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private com.parentplatform.repository.CommentReactionRepository reactionRepository;

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody Comment comment) {
        Optional<User> userOpt = userService.findById(comment.getUser().getId());
        Post post = postService.findById(comment.getPost().getId());

        if (!userOpt.isPresent() || post == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Utilisateur ou post non trouvé");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Map<String, Object> response = commentService.add(comment, userOpt.get(), post);
        return ResponseEntity.ok(response);
    }

    /**
     * Commentaires d'une publication, avec leurs réponses et leurs réactions.
     * {@code userId} (facultatif) permet de savoir quelle réaction la personne
     * connectée a déjà posée.
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<?> getByPostId(@PathVariable Long postId,
                                         @RequestParam(required = false) Long userId) {
        Post post = postService.findById(postId);
        if (post == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Post non trouvé");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        List<Comment> comments = commentService.getCommentsByPost(post);

        // Réactions chargées en une seule requête pour toute la publication
        List<Long> ids = comments.stream().map(Comment::getId).collect(java.util.stream.Collectors.toList());
        Map<Long, Map<String, Long>> compteurs = new HashMap<>();
        Map<Long, String> maReaction = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] ligne : reactionRepository.parCommentaires(ids)) {
                Long commentId = (Long) ligne[0];
                String type = (String) ligne[1];
                Long auteur = (Long) ligne[2];
                compteurs.computeIfAbsent(commentId, k -> new LinkedHashMap<>())
                        .merge(type, 1L, Long::sum);
                if (userId != null && userId.equals(auteur)) maReaction.put(commentId, type);
            }
        }

        List<Map<String, Object>> formattedComments = new ArrayList<>();
        for (Comment comment : comments) {
            Map<String, Object> c = new HashMap<>();
            c.put("id", comment.getId());
            c.put("contenu", comment.getContenu());
            c.put("createdAt", comment.getCreatedAt());
            c.put("parentId", comment.getParentId());
            c.put("modifie", comment.isModifie());
            c.put("reactions", compteurs.getOrDefault(comment.getId(), Map.of()));
            c.put("maReaction", maReaction.get(comment.getId()));

            if (comment.getUser() != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", comment.getUser().getId());
                userMap.put("nom", comment.getUser().getNom());
                userMap.put("role", comment.getUser().getRole() != null ? comment.getUser().getRole().name() : null);
                c.put("user", userMap);
            }
            formattedComments.add(c);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("comments", formattedComments);
        response.put("count", formattedComments.size());
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long commentId,
                                           @RequestBody Map<String, String> payload,
                                           @RequestHeader("X-User-Id") Long userId) {
        try {
            Optional<Comment> commentOpt = commentService.findById(commentId);
            if (!commentOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Commentaire non trouvé"));
            }
            Comment comment = commentOpt.get();
            if (!comment.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Vous n'êtes pas l'auteur de ce commentaire"));
            }
            String newContent = payload.get("contenu");
            if (newContent == null || newContent.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Le contenu ne peut pas être vide"));
            }
            comment.setContenu(newContent);
            comment.setModifie(true);
            Comment saved = commentService.save(comment);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("comment", Map.of(
                    "id", saved.getId(),
                    "contenu", saved.getContenu(),
                    "modifie", saved.isModifie()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId,
                                           @RequestHeader("X-User-Id") Long userId) {
        try {
            Optional<Comment> commentOpt = commentService.findById(commentId);
            if (!commentOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Commentaire non trouvé"));
            }
            Comment comment = commentOpt.get();
            if (!comment.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Vous n'êtes pas l'auteur de ce commentaire"));
            }
            // Les réponses et les réactions rattachées disparaissent avec le commentaire
            reactionRepository.deleteByCommentId(commentId);
            for (Comment reponse : commentService.reponsesDe(commentId)) {
                reactionRepository.deleteByCommentId(reponse.getId());
                commentService.delete(reponse.getId());
            }
            commentService.delete(commentId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Commentaire supprimé"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Pose, remplace ou retire une réaction sur un commentaire.
     * Un même utilisateur n'a qu'une réaction par commentaire : renvoyer le même
     * type la supprime, en envoyer un autre la remplace.
     */
    @PostMapping("/{commentId}/reaction")
    public ResponseEntity<?> reagir(@PathVariable Long commentId,
                                    @RequestBody(required = false) Map<String, String> payload,
                                    @RequestHeader("X-User-Id") Long userId) {
        try {
            if (commentService.findById(commentId).isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Commentaire non trouvé"));
            }
            String type = payload != null && payload.get("type") != null
                    ? payload.get("type") : com.parentplatform.model.CommentReaction.JAIME;

            Optional<com.parentplatform.model.CommentReaction> existante =
                    reactionRepository.findByCommentIdAndUserId(commentId, userId);

            String active;
            if (existante.isPresent() && existante.get().getType().equals(type)) {
                reactionRepository.delete(existante.get());
                active = null;
            } else if (existante.isPresent()) {
                existante.get().setType(type);
                reactionRepository.save(existante.get());
                active = type;
            } else {
                reactionRepository.save(new com.parentplatform.model.CommentReaction(commentId, userId, type));
                active = type;
            }

            Map<String, Long> compteurs = new LinkedHashMap<>();
            for (com.parentplatform.model.CommentReaction r : reactionRepository.findByCommentId(commentId)) {
                compteurs.merge(r.getType(), 1L, Long::sum);
            }

            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("reactions", compteurs);
            reponse.put("maReaction", active);
            return ResponseEntity.ok(reponse);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/received/users/{userId}")
    public ResponseEntity<?> getUsersWhoCommentedMyPosts(@PathVariable Long userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Utilisateur non trouvé"));
        }
        List<Post> posts = postService.findByUserId(userId);
        Set<User> uniqueUsers = new HashSet<>();
        for (Post post : posts) {
            List<Comment> comments = commentService.getCommentsByPost(post);
            for (Comment comment : comments) {
                uniqueUsers.add(comment.getUser());
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "users", uniqueUsers));
    }
}