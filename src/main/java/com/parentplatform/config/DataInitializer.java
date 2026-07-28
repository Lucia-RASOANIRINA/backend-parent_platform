package com.parentplatform.config;

import com.parentplatform.model.Comment;
import com.parentplatform.model.Evenement;
import com.parentplatform.model.Post;
import com.parentplatform.model.Resource;
import com.parentplatform.model.Role;
import com.parentplatform.model.User;
import com.parentplatform.repository.CommentRepository;
import com.parentplatform.repository.PostRepository;
import com.parentplatform.repository.ResourceRepository;
import com.parentplatform.repository.UserRepository;
import com.parentplatform.service.EvenementService;
import com.parentplatform.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Initialise des données RÉELLES au démarrage (phase de test) :
 *  - le compte ADMIN (luciarasoanirina8@gmail.com / admin123)
 *  - des utilisateurs de démonstration (parents, éducatrices, psychologues), tous en mot de passe "user123"
 *  - tous les utilisateurs non-admin reçoivent le mot de passe "user123"
 *  - des évènements de démonstration : uniquement Ateliers et Conférences
 *  - des ressources (vidéos + fiches) créées par des éducatrices et psychologues
 */
@Configuration
public class DataInitializer {

    private static final String ADMIN_EMAIL = "luciarasoanirina8@gmail.com";

    @Bean
    CommandLineRunner seed(UserService userService,
                           UserRepository userRepository,
                           EvenementService evenementService,
                           ResourceRepository resourceRepository,
                           PostRepository postRepository,
                           CommentRepository commentRepository) {
        return args -> {
            // --- Administrateur ---
            userService.ensureUser("Lucia Rasoanirina", ADMIN_EMAIL, "admin123", Role.ADMIN);
            System.out.println("[Parentia] Admin : " + ADMIN_EMAIL + " / admin123");

            // --- Utilisateurs de démonstration (mot de passe user123) ---
            User par1 = userService.ensureUser("Hanitra Rakoto", "hanitra.parent@parentia.mg", "user123", Role.PARENT);
            User par2 = userService.ensureUser("Tiana Rabe", "tiana.parent@parentia.mg", "user123", Role.PARENT);
            User par3 = userService.ensureUser("Naina Andria", "naina.parent@parentia.mg", "user123", Role.PARENT);
            User educ1 = userService.ensureUser("Voahirana Razafy", "voahirana.educ@parentia.mg", "user123", Role.EDUCATEUR);
            User educ2 = userService.ensureUser("Miora Andrian", "miora.educ@parentia.mg", "user123", Role.EDUCATEUR);
            User psy1 = userService.ensureUser("Dr. Faniry Rakotomalala", "faniry.psy@parentia.mg", "user123", Role.PSY);
            User psy2 = userService.ensureUser("Dr. Lova Ranaivo", "lova.psy@parentia.mg", "user123", Role.PSY);

            // --- Phase de test : tous les non-admin en "user123" ---
            int reset = userService.resetPasswordForNonAdmins("user123");
            System.out.println("[Parentia] " + reset + " utilisateurs (non-admin) -> mot de passe user123");

            // --- Évènements (uniquement Ateliers et Conférences) ---
            if (evenementService.total() == 0) {
                createEvent(evenementService, "Accompagner les émotions de l'enfant",
                        "Comprendre et apaiser les colères, la peur et la frustration grâce à des outils simples et concrets.",
                        "atelier", LocalDate.of(2026, 7, 12), "10h00", "11h30", "Antananarivo", "Dr. Faniry Rakotomalala", 20, Role.ADMIN, false);

                createEvent(evenementService, "Le sommeil du jeune enfant",
                        "Mettre en place une routine du soir sereine et adaptée à l'âge de votre enfant.",
                        "conference", LocalDate.of(2026, 7, 18), "18h00", "19h30", "Fianarantsoa", "Voahirana Razafy", 30, Role.ADMIN, false);

                createEvent(evenementService, "Motricité & jeux Montessori",
                        "Des activités d'éveil à reproduire à la maison pour favoriser l'autonomie des tout-petits.",
                        "atelier", LocalDate.of(2026, 8, 9), "09h30", "11h00", "Antananarivo", "Miora Andrian", 16, Role.ADMIN, false);

                createEvent(evenementService, "Petit groupe : gérer les écrans en famille",
                        "Échange en visio en petit comité pour poser un cadre sain autour des écrans selon l'âge.",
                        "conference", LocalDate.of(2026, 8, 20), "20h00", "21h00", "En ligne", "Voahirana Razafy", 4, Role.EDUCATEUR, true);

                System.out.println("[Parentia] " + evenementService.total() + " évènements de démonstration créés.");
            }

            // --- Ressources créées par les éducatrices / psychologues ---
            if (resourceRepository.count() == 0) {
                createVideoResource(resourceRepository, "Aider son enfant à gérer ses émotions",
                        "Une vidéo pédagogique : 4 étapes concrètes pour accompagner votre enfant dans la gestion de ses émotions au quotidien.",
                        "3-5", "https://www.youtube.com/watch?v=kmU8O3p7Fas",
                        "https://picsum.photos/seed/parentia-emotions-video/640/360", educ1.getId());

                createVideoResource(resourceRepository, "Comprendre et calmer les colères de l'enfant",
                        "Comprendre ce qui se passe dans le cerveau de l'enfant lors d'une colère et comment l'apaiser avec bienveillance.",
                        "1-3", "https://www.youtube.com/watch?v=BBxqa-Qg67s",
                        "https://picsum.photos/seed/parentia-coleres/640/360", psy1.getId());

                createFicheResource(resourceRepository, "Fiche : routine du soir sereine",
                        "Bain, histoire, lumière douce : une routine du coucher en 5 étapes pour faciliter l'endormissement.",
                        "1-3 ans", "https://picsum.photos/seed/parentia-sommeil/640/360", educ2.getId());

                createFicheResource(resourceRepository, "Fiche : nommer les émotions",
                        "Un outil simple pour aider l'enfant à mettre des mots sur ce qu'il ressent et apaiser les tensions.",
                        "3-5 ans", "https://picsum.photos/seed/parentia-emotions/640/360", psy2.getId());

                System.out.println("[Parentia] " + resourceRepository.count() + " ressources de démonstration créées.");
            }

            // --- Publications & discussions (datées depuis mars 2026) ---
            if (postRepository.count() == 0) {
                Post p1 = post(postRepository, educ1, "Atelier motricité ce matin avec les tout-petits ! Parcours de coussins, transvasement d'eau et tri par couleurs : l'autonomie se construit pas à pas.",
                        LocalDateTime.of(2026, 3, 4, 9, 12), 24);
                comment(commentRepository, p1, par1, "Merci pour ces idées, on teste ça ce week-end !", LocalDateTime.of(2026, 3, 4, 18, 30));
                comment(commentRepository, p1, psy1, "Le transvasement est excellent pour la concentration.", LocalDateTime.of(2026, 3, 5, 8, 5));

                Post p2 = post(postRepository, psy1, "Gérer les colères avant 5 ans : nommer l'émotion (« tu es très en colère ») aide l'enfant à se calmer plus vite que de le raisonner. Patience et constance.",
                        LocalDateTime.of(2026, 3, 11, 14, 0), 58);
                comment(commentRepository, p2, par2, "Tellement vrai, ça a changé nos soirées.", LocalDateTime.of(2026, 3, 11, 20, 12));
                comment(commentRepository, p2, par3, "Une question : et quand ça arrive en public ?", LocalDateTime.of(2026, 3, 12, 7, 45));

                Post p3 = post(postRepository, par2, "Merci à la communauté pour vos conseils sur le sommeil. La routine du soir (bain, histoire, lumière douce) a tout changé en deux semaines.",
                        LocalDateTime.of(2026, 3, 19, 21, 30), 41);
                comment(commentRepository, p3, educ2, "Bravo, la régularité est la clé !", LocalDateTime.of(2026, 3, 20, 6, 15));

                post(postRepository, educ2, "Idée d'activité Montessori : un plateau de tri avec des pompons et une pince à glaçons. Motricité fine garantie !",
                        LocalDateTime.of(2026, 3, 27, 10, 20), 33);

                Post p5 = post(postRepository, psy2, "Le « non » de l'enfant de 2 ans n'est pas de l'opposition gratuite : c'est l'affirmation de soi. On peut proposer des choix limités pour l'accompagner.",
                        LocalDateTime.of(2026, 4, 2, 11, 5), 47);
                comment(commentRepository, p5, par1, "Les choix limités, ça marche super bien chez nous.", LocalDateTime.of(2026, 4, 2, 19, 0));

                post(postRepository, par1, "Première rentrée à la crèche la semaine prochaine… des conseils pour gérer la séparation ?",
                        LocalDateTime.of(2026, 4, 9, 8, 40), 18);

                Post p7 = post(postRepository, educ1, "Petit rappel : avant 3 ans, l'enfant apprend surtout par l'imitation. Montrez plutôt que d'expliquer longuement.",
                        LocalDateTime.of(2026, 4, 16, 15, 25), 52);
                comment(commentRepository, p7, par3, "Ça déculpabilise, merci !", LocalDateTime.of(2026, 4, 16, 22, 10));

                post(postRepository, psy1, "L'écran avant 3 ans : on privilégie l'interaction réelle. Si écran il y a, c'est court et accompagné d'un adulte.",
                        LocalDateTime.of(2026, 4, 23, 9, 0), 39);

                post(postRepository, par3, "Astuce repas : laisser l'enfant manger seul (même si c'est le chaos) développe son autonomie. On respire et on nettoie après !",
                        LocalDateTime.of(2026, 5, 1, 12, 30), 29);

                Post p10 = post(postRepository, educ2, "Comptine du jour pour apaiser : « Une souris verte ». La répétition rassure les tout-petits.",
                        LocalDateTime.of(2026, 5, 8, 10, 0), 22);
                comment(commentRepository, p10, par2, "Mon fils l'adore !", LocalDateTime.of(2026, 5, 8, 17, 45));

                post(postRepository, psy2, "Les pleurs du soir (« décharge » de la journée) sont normaux chez le nourrisson. Un câlin et une ambiance calme suffisent souvent.",
                        LocalDateTime.of(2026, 5, 15, 18, 20), 44);

                post(postRepository, par1, "Quelqu'un a testé la méthode du « time-in » plutôt que le « time-out » ? Curieuse de vos retours.",
                        LocalDateTime.of(2026, 5, 22, 20, 5), 26);

                Post p13 = post(postRepository, educ1, "Bricolage de saison : empreintes de mains pour une carte. Peinture lavable et beaucoup de rires garantis.",
                        LocalDateTime.of(2026, 6, 1, 9, 50), 31);
                comment(commentRepository, p13, par1, "On fait ça pour la fête des pères !", LocalDateTime.of(2026, 6, 1, 14, 0));

                post(postRepository, psy1, "Rappel bienveillant : il n'existe pas de parent parfait. Un parent « suffisamment bon » est exactement ce dont l'enfant a besoin.",
                        LocalDateTime.of(2026, 6, 10, 8, 15), 67);

                post(postRepository, par2, "Bilan de 3 mois sur Parentia : tellement de soutien et d'idées concrètes. Merci à toute la communauté !",
                        LocalDateTime.of(2026, 6, 18, 19, 40), 38);

                System.out.println("[Parentia] " + postRepository.count() + " publications + commentaires créés (depuis mars 2026).");
            }
        };
    }

    private Post post(PostRepository repo, User author, String contenu, LocalDateTime when, int likes) {
        Post p = new Post();
        p.setContenu(contenu);
        p.setUser(author);
        p.setCreatedAt(when);
        p.setLikesCount(likes);
        return repo.save(p);
    }

    private void comment(CommentRepository repo, Post post, User user, String text, LocalDateTime when) {
        Comment c = new Comment(text, user, post);
        c.setCreatedAt(when.toString());
        repo.save(c);
    }

    private void createEvent(EvenementService service, String titre, String desc, String type,
                             LocalDate date, String debut, String fin, String lieu, String animateur,
                             int capacite, Role role, boolean online) {
        Evenement e = new Evenement();
        e.setTitre(titre);
        e.setDescription(desc);
        e.setType(type);
        e.setDate(date);
        e.setHeureDebut(debut);
        e.setHeureFin(fin);
        e.setLieu(lieu);
        e.setAnimateur(animateur);
        e.setCapacite(capacite);
        e.setCreatedByRole(role);
        e.setCreatedByNom(role == Role.ADMIN ? "Lucia Rasoanirina" : animateur);
        e.setOnline(online);
        e.setPublie(true);
        service.create(e, null);
    }

    private void createVideoResource(ResourceRepository repo, String title, String desc, String age,
                                     String videoUrl, String thumbnail, Long ownerId) {
        Resource r = new Resource();
        r.setTitle(title);
        r.setDescription(desc);
        r.setType("video");
        r.setAge(age);
        r.setVideoUrl(videoUrl);
        r.setThumbnail(thumbnail);
        r.setOwnerId(ownerId);
        r.setShared(true);
        repo.save(r);
    }

    private void createFicheResource(ResourceRepository repo, String title, String desc, String age,
                                     String thumbnail, Long ownerId) {
        Resource r = new Resource();
        r.setTitle(title);
        r.setDescription(desc);
        r.setFullContent(desc);
        r.setType("fiche");
        r.setAge(age);
        r.setThumbnail(thumbnail);
        r.setOwnerId(ownerId);
        r.setShared(true);
        repo.save(r);
    }
}
