# Parentia — Backend

API REST de **Parentia**, la plateforme communautaire pour le bien-être des enfants
(parents, éducatrices, psychologues, administrateurs).

Construit avec **Spring Boot 3.2**, **Spring Data JPA** et **PostgreSQL**.

---

## 🚀 Prérequis

- **JDK 17** (le projet cible Java 17)
- **PostgreSQL 14+** en local
- Maven (le wrapper `./mvnw` est inclus, aucune installation requise)

## 🗄️ Base de données

Créez la base attendue par `src/main/resources/application.properties` :

```sql
CREATE DATABASE platform_parent;
```

Configuration par défaut (à adapter si besoin) :

```properties
server.port=8082
spring.datasource.url=jdbc:postgresql://localhost:5432/platform_parent
spring.datasource.username=postgres
spring.datasource.password=1701
spring.jpa.hibernate.ddl-auto=update   # les tables sont créées/mises à jour automatiquement
```

## ▶️ Lancement

```bash
./mvnw spring-boot:run        # démarre l'API sur http://localhost:8082
./mvnw clean package          # construit le .jar
java -jar target/parentplatform-0.0.1-SNAPSHOT.jar
```

> Au premier démarrage, un **compte administrateur** et des **évènements de démonstration**
> sont créés automatiquement (voir `config/DataInitializer.java`) :
>
> ```
> admin@parentia.mg / admin123
> ```

---

## 🔌 Principaux endpoints

### Authentification — `/api/auth`
| Méthode | Route | Description |
|--------|-------|-------------|
| POST | `/register` | Inscription (`PARENT`, `EDUCATEUR`, `PSY`, `ADMIN`) |
| POST | `/login` | Connexion (renvoie l'utilisateur + token) |
| GET  | `/users` | Liste des utilisateurs |
| PUT  | `/profile` | Mise à jour du profil (header `X-User-Id`) |

### Évènements — `/api/evenements`
| Méthode | Route | Description |
|--------|-------|-------------|
| GET | `/?userId=&all=` | Liste (places restantes + statut d'inscription) |
| GET | `/{id}` | Détail d'un évènement |
| POST | `/?userId=` | Créer un évènement |
| PUT | `/{id}` | Modifier |
| DELETE | `/{id}` | Supprimer |
| POST | `/{id}/inscription?userId=` | S'inscrire |
| DELETE | `/{id}/inscription?userId=` | Se désinscrire |
| GET | `/{id}/inscriptions` | Liste des inscrits |

### Administration — `/api/admin`
| Méthode | Route | Description |
|--------|-------|-------------|
| GET | `/stats` | Statistiques globales (totaux + par rôle) |
| GET | `/users?role=` | Utilisateurs (filtrables par rôle) |
| PUT | `/users/{id}/role` | Changer le rôle |
| DELETE | `/users/{id}` | Supprimer un utilisateur |
| DELETE | `/posts/{id}` · `/ressources/{id}` | Modération |

### Autres modules
- **Posts** `/api/posts` — publications, images, fichiers
- **Ressources** `/api/resources` — fiches, PDF, **vidéos** (`videoUrl`), likes, notes, commentaires
- **Messagerie / Chat** — `/api/messages` + WebSocket (STOMP) via `/ws`

---

## 📁 Structure

```
src/main/java/com/parentplatform/
├── controller/   AuthController, EvenementController, AdminController, PostController, ResourceController …
├── service/      UserService, EvenementService, PostService, ResourceService …
├── repository/   *Repository (Spring Data JPA)
├── model/        User, Role, Evenement, EvenementInscription, Post, Resource …
└── config/       WebSocketConfig, DataInitializer
```

## 🔐 CORS

Toutes les origines sont autorisées en développement
(`spring.web.cors.allowed-origin-patterns=*`) pour permettre l'accès depuis le frontend Vite.

## ⚠️ Note sur la version de Java

Le projet est prévu pour **JDK 17**. Si vous utilisez un JDK plus récent (ex. 21/25),
la compilation reste compatible (`--release 17`), mais privilégiez un **JDK 17** pour
l'exécution afin d'éviter d'éventuelles incompatibilités d'Hibernate/Byte-Buddy au runtime.
