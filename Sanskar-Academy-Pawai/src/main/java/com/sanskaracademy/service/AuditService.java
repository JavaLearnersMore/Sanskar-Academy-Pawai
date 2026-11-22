package com.sanskaracademy.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.AuditLog;
import com.sanskaracademy.repository.AuditLogRepository;

@Service
public class AuditService {

    private final AuditLogRepository repo;
    private final int retentionDays;

    public AuditService(AuditLogRepository repo, @Value("${app.audit.retention-days:180}") int retentionDays) {
        this.repo = repo;
        this.retentionDays = Math.max(30, retentionDays);
    }

    /** never throws: a problem with the log must not break the user's action */
    public void record(String username, String role, String method, String path, int status, String ip) {
        try {
            AuditLog a = new AuditLog();
            a.setLoggedAt(LocalDateTime.now());
            a.setUsername(cut(username, 80));
            a.setRole(cut(role, 30));
            a.setMethod(cut(method, 10));
            a.setPath(cut(path, 300));
            a.setStatus(status);
            a.setIp(cut(ip, 60));
            repo.save(a);
        } catch (RuntimeException ignored) {
            // best effort
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLog> search(LocalDate from, LocalDate to, String user, String method, int limit) {
        LocalDate f = from == null ? LocalDate.now().minusDays(7) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        return repo.search(f.atStartOfDay(), t.plusDays(1).atStartOfDay(),
                user == null ? "" : user.trim(), method == null ? "" : method.trim().toUpperCase(),
                PageRequest.of(0, Math.min(Math.max(limit, 10), 1000)));
    }

    @Transactional
    public int purge() {
        return repo.deleteOlderThan(LocalDateTime.now().minusDays(retentionDays));
    }

    public int getRetentionDays() { return retentionDays; }

    private static String cut(String s, int n) {
        if (s == null) return null;
        return s.length() > n ? s.substring(0, n) : s;
    }
}
