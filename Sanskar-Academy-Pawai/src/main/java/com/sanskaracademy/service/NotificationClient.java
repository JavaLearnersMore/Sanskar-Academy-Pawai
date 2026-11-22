package com.sanskaracademy.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.sanskaracademy.entity.User;

/**
 * Talks to the separate Notification microservice (own database, own port).
 * Linked only by the user id. When NOTIFICATION_URL / NOTIFICATION_API_KEY are not set,
 * everything here quietly does nothing, so the main application never depends on it.
 */
@Service
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final String baseUrl;
    private final String apiKey;
    private final RestClient client;
    private final ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "notification-client");
        t.setDaemon(true);
        return t;
    });

    public NotificationClient(@Value("${notification.service.url:}") String baseUrl,
                              @Value("${notification.service.api-key:}") String apiKey) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(3));
        f.setReadTimeout(Duration.ofSeconds(8));
        this.client = RestClient.builder().requestFactory(f).build();
    }

    public boolean isEnabled() {
        return !baseUrl.isEmpty() && !apiKey.isEmpty();
    }

    /** Fire and forget: the person is told only if he/she switched messages on and chose this message type. */
    public void sendAsync(User user, String templateCode, Map<String, String> params, String dedupeKey) {
        if (!isEnabled() || user == null) return;
        final Long id = user.getId();
        final Map<String, Object> profile = profileOf(user);
        pool.submit(() -> {
            try {
                put("/api/contacts/" + id + "/profile", profile);
                Map<String, Object> body = new HashMap<>();
                body.put("externalUserId", id);
                body.put("templateCode", templateCode);
                body.put("params", params);
                body.put("dedupeKey", dedupeKey);
                post("/api/send", body);
            } catch (Exception e) {
                log.warn("Notification service call failed: {}", e.getClass().getSimpleName());
            }
        });
    }

    /** Everybody of these roles who opted in (festival wishes). */
    public void broadcastAsync(java.util.List<String> roles, String templateCode, Map<String, String> params, String dedupeKey) {
        if (!isEnabled()) return;
        pool.submit(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("roles", roles);
                body.put("templateCode", templateCode);
                body.put("params", params);
                body.put("dedupeKey", dedupeKey);
                post("/api/broadcast", body);
            } catch (Exception e) {
                log.warn("Notification broadcast failed: {}", e.getClass().getSimpleName());
            }
        });
    }

    /** Used by the Message settings page. Syncs the profile first, then reads the person's choices. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadPreferences(User user) {
        put("/api/contacts/" + user.getId() + "/profile", profileOf(user));
        return client.get().uri(baseUrl + "/api/contacts/" + user.getId() + "/preferences")
                .header("X-API-Key", apiKey).retrieve().body(Map.class);
    }

    public void savePreferences(User user, Map<String, Object> prefs) {
        put("/api/contacts/" + user.getId() + "/profile", profileOf(user));
        put("/api/contacts/" + user.getId() + "/preferences", prefs);
    }

    private Map<String, Object> profileOf(User u) {
        Map<String, Object> p = new HashMap<>();
        p.put("role", u.getRole() == null ? null : u.getRole().getName());
        p.put("fullName", u.getFullName());
        p.put("email", u.getEmail());
        p.put("phone", u.getMobileNumber());
        return p;
    }

    private void put(String path, Object body) {
        client.put().uri(baseUrl + path).header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
    }

    private void post(String path, Object body) {
        client.post().uri(baseUrl + path).header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
    }
}
