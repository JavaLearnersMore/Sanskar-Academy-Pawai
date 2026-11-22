package com.sanskaracademy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import com.sanskaracademy.security.TwoFactorFilter;
import com.sanskaracademy.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                // public pages only. /verify and /setup-2fa need a logged-in user (password step done)
                .requestMatchers("/", "/login", "/register", "/verify-email", "/resend-verification",
                                 "/error", "/css/**", "/images/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/teacher/**").hasRole("TEACHER")
                .requestMatchers("/student/**").hasRole("STUDENT")
                .requestMatchers("/parent/**").hasRole("PARENT")
                .requestMatchers("/staff/**").hasAnyRole("STAFF", "TEACHER")
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .defaultSuccessUrl("/verify", true)
                .failureHandler((request, response, ex) -> response.sendRedirect(
                        request.getContextPath()
                        + (ex instanceof DisabledException ? "/login?pending" : "/login?error")))
            )
            .logout(logout -> logout
                .logoutUrl("/logout")                       // POST only (CSRF protected)
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            // the authenticator code is compulsory, not just a page the user can walk around
            .addFilterAfter(new TwoFactorFilter(), AnonymousAuthenticationFilter.class)
            .userDetailsService(userDetailsService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
