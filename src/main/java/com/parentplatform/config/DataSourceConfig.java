package com.parentplatform.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Choisit automatiquement la base de données au démarrage.
 *
 * <pre>
 *   app.db.mode = auto    → teste la base en ligne, bascule sur la base locale si injoignable
 *   app.db.mode = online  → force la base en ligne (Neon)
 *   app.db.mode = local   → force la base locale (PostgreSQL du poste)
 * </pre>
 *
 * En mode « auto » la base en ligne est prioritaire : c'est celle que partagent
 * le déploiement Render et le frontend Vercel. Si la connexion échoue (pas de
 * réseau, coupure Neon…), l'application démarre sur la base locale afin de
 * rester utilisable hors ligne.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${app.db.mode:auto}")
    private String configuredMode;

    @Value("${app.db.probe-timeout-seconds:5}")
    private int probeTimeoutSeconds;

    @Value("${app.db.online.url}")
    private String onlineUrl;
    @Value("${app.db.online.username}")
    private String onlineUsername;
    @Value("${app.db.online.password}")
    private String onlinePassword;

    @Value("${app.db.local.url}")
    private String localUrl;
    @Value("${app.db.local.username}")
    private String localUsername;
    @Value("${app.db.local.password}")
    private String localPassword;

    @Bean
    public DatabaseMode databaseMode() {
        return new DatabaseMode();
    }

    @Bean
    @Primary
    public DataSource dataSource(DatabaseMode databaseMode) {
        String mode = configuredMode == null ? "auto" : configuredMode.trim().toLowerCase();

        boolean useOnline;
        boolean fallback = false;
        String detail;

        switch (mode) {
            case DatabaseMode.LOCAL:
                useOnline = false;
                detail = "Mode forcé par la configuration (app.db.mode=local).";
                break;
            case DatabaseMode.ONLINE:
                useOnline = true;
                detail = "Mode forcé par la configuration (app.db.mode=online).";
                break;
            default:
                if (canConnect(onlineUrl, onlineUsername, onlinePassword)) {
                    useOnline = true;
                    detail = "Base en ligne joignable : données partagées avec le site déployé.";
                } else if (canConnect(localUrl, localUsername, localPassword)) {
                    useOnline = false;
                    fallback = true;
                    detail = "Base en ligne injoignable — bascule automatique sur PostgreSQL local.";
                } else {
                    useOnline = true;
                    fallback = true;
                    detail = "Aucune base joignable — nouvelle tentative sur la base en ligne.";
                    log.error("Aucune base de données n'est joignable (ni en ligne, ni locale). "
                            + "Vérifiez votre connexion internet ou démarrez PostgreSQL en local.");
                }
        }

        String url = useOnline ? onlineUrl : localUrl;
        databaseMode.setMode(useOnline ? DatabaseMode.ONLINE : DatabaseMode.LOCAL);
        databaseMode.setUrl(url);
        databaseMode.setHost(hostOf(url));
        databaseMode.setFallback(fallback);
        databaseMode.setDetail(detail);

        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ Parentia — base de données : {}", databaseMode.getLabel());
        log.info("║ Hôte    : {}", databaseMode.getHost());
        log.info("║ {}", detail);
        log.info("╚══════════════════════════════════════════════════════════════");

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(useOnline ? onlineUsername : localUsername);
        ds.setPassword(useOnline ? onlinePassword : localPassword);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setAutoCommit(false);
        ds.setPoolName(useOnline ? "parentia-online" : "parentia-local");
        ds.setConnectionTimeout(30_000);
        ds.setMaximumPoolSize(10);
        return ds;
    }

    /** Test de connexion court, pour ne pas bloquer le démarrage quand le réseau est absent. */
    private boolean canConnect(String url, String username, String password) {
        if (url == null || url.isBlank()) return false;
        Properties props = new Properties();
        props.setProperty("user", username == null ? "" : username);
        props.setProperty("password", password == null ? "" : password);
        props.setProperty("connectTimeout", String.valueOf(probeTimeoutSeconds));
        props.setProperty("socketTimeout", String.valueOf(probeTimeoutSeconds * 2));
        props.setProperty("loginTimeout", String.valueOf(probeTimeoutSeconds));
        try (Connection ignored = DriverManager.getConnection(url, props)) {
            return true;
        } catch (Exception e) {
            log.info("Base injoignable ({}) : {}", hostOf(url), e.getMessage());
            return false;
        }
    }

    /** Extrait « hôte/base » d'une URL JDBC, sans les identifiants. */
    private String hostOf(String jdbcUrl) {
        if (jdbcUrl == null) return "";
        String s = jdbcUrl.replaceFirst("^jdbc:postgresql://", "");
        int q = s.indexOf('?');
        return q > 0 ? s.substring(0, q) : s;
    }
}
