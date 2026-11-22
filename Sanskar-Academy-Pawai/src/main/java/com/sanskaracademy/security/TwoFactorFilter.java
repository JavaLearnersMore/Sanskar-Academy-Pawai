package com.sanskaracademy.security;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Makes the two-factor step compulsory.
 * A user who has typed the right password is "logged in" for Spring Security, but until the
 * authenticator code is accepted (AuthController.verifyCode sets SESSION_KEY) every page except
 * /verify, /setup-2fa and /logout redirects to /verify.
 */
public class TwoFactorFilter extends OncePerRequestFilter {

    public static final String SESSION_KEY = "TOTP_VERIFIED_USER";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && !isAllowedWithoutCode(request)) {

            HttpSession session = request.getSession(false);
            // the session value is bound to the user name, so a second login in the same browser
            // can never inherit the first user's verification
            boolean verified = session != null && auth.getName().equals(session.getAttribute(SESSION_KEY));

            if (!verified) {
                response.sendRedirect(request.getContextPath() + "/verify");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isAllowedWithoutCode(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/verify") || path.equals("/setup-2fa") || path.equals("/logout")
                || path.equals("/login") || path.equals("/error") || path.startsWith("/css/");
    }
}
