package com.sanskaracademy.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sanskaracademy.service.AuditService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes one line to the audit log for every state-changing request (POST / PUT / PATCH / DELETE) of a logged-in user:
 * who, when, which address, and whether it worked. Form contents (passwords, remarks...) are NOT stored.
 */
@Component
public class AuditFilter extends OncePerRequestFilter {

    private final AuditService audit;

    public AuditFilter(AuditService audit) {
        this.audit = audit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String m = request.getMethod();
        return !("POST".equals(m) || "PUT".equals(m) || "PATCH".equals(m) || "DELETE".equals(m));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            if (a != null && a.isAuthenticated() && !"anonymousUser".equals(String.valueOf(a.getPrincipal()))) {
                String role = a.getAuthorities().stream().findFirst().map(x -> x.getAuthority().replace("ROLE_", "")).orElse("");
                audit.record(a.getName(), role, request.getMethod(), request.getRequestURI(), response.getStatus(), request.getRemoteAddr());
            }
        }
    }
}
