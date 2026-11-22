package com.sanskaracademy.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sanskaracademy.dto.RegisterDto;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.security.TwoFactorFilter;
import com.sanskaracademy.service.EmailService;
import com.sanskaracademy.service.TotpService;
import com.sanskaracademy.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private static final int MAX_CODE_FAILURES = 5;
    private static final String FAIL_KEY = "TOTP_FAILURES";

    private final TotpService totpService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;

    @Value("${totp.issuer:SanskarAcademyClasses}")
    private String issuer;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // =========================
    // 2FA: enter the code
    // =========================
    @GetMapping("/verify")
    public String verifyPage(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        // first login: the user has to scan the QR code once
        if (Boolean.FALSE.equals(user.getTotpSetupDone())) {
            return "redirect:/setup-2fa";
        }
        return "verify";
    }

    @PostMapping("/verify")
    public String verifyCode(@RequestParam String code,
                             Principal principal,
                             HttpSession session) {

        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        int number;
        try {
            number = Integer.parseInt(code.trim());
        } catch (NumberFormatException e) {
            return failedCode(session, user);
        }

        if (user.getTotpSecret() == null || !totpService.verifyCode(user.getTotpSecret(), number)) {
            return failedCode(session, user);
        }

        // code accepted: from now on this session may use the application
        session.removeAttribute(FAIL_KEY);
        session.setAttribute(TwoFactorFilter.SESSION_KEY, user.getUsername());

        if (Boolean.FALSE.equals(user.getTotpSetupDone())) {
            user.setTotpSetupDone(true);       // QR can never be shown again
            userRepository.save(user);
        }

        switch (user.getRole().getName()) {
            case "ADMIN":
                return "redirect:/admin/dashboard";
            case "TEACHER":
                return "redirect:/teacher/dashboard";
            case "STUDENT":
                return "redirect:/student/dashboard";
            case "PARENT":
                return "redirect:/parent/dashboard";
            case "STAFF":
                return "redirect:/staff/dashboard";
            default:
                return "redirect:/login";
        }
    }

    private String failedCode(HttpSession session, User user) {
        Integer fails = (Integer) session.getAttribute(FAIL_KEY);
        fails = fails == null ? 1 : fails + 1;
        if (fails >= MAX_CODE_FAILURES) {
            session.invalidate();               // too many wrong codes: back to the password step
            return "redirect:/login?error";
        }
        session.setAttribute(FAIL_KEY, fails);
        return Boolean.FALSE.equals(user.getTotpSetupDone())
                ? "redirect:/setup-2fa?error"
                : "redirect:/verify?error";
    }

    // =========================
    // 2FA: first-time QR code (only for the logged-in user, only until the first code is accepted)
    // =========================
    @GetMapping("/setup-2fa")
    public String setup2FA(Principal principal, Model model) {

        User user = userRepository.findByUsername(principal.getName()).orElseThrow();

        if (!Boolean.FALSE.equals(user.getTotpSetupDone())) {
            return "redirect:/verify";
        }

        String otpauth = "otpauth://totp/" + issuer + ":" + user.getUsername()
                + "?secret=" + user.getTotpSecret() + "&issuer=" + issuer;

        // the QR image is drawn in the browser - the secret is never sent to another website
        model.addAttribute("otpauth", otpauth);
        model.addAttribute("secret", user.getTotpSecret());
        model.addAttribute("username", user.getUsername());
        return "setup-2fa";
    }

    @GetMapping("/logout")
    public String logoutGet() {
        return "redirect:/login";
    }

    // =========================
    // REGISTER
    // =========================
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute RegisterDto dto,
                               Model model) {
        try {
            userService.registerUser(dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("registerDto", dto);
            model.addAttribute("error", e.getMessage());
            return "register";
        }

        model.addAttribute("message",
                "Registration successful! Please verify your email, then wait for admin approval.");
        return "login";
    }

    // =========================
    // EMAIL VERIFICATION
    // =========================
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {

        User user = userRepository.findByVerificationToken(token).orElse(null);
        if (user == null) {
            model.addAttribute("errorMessage", "This verification link is invalid or was already used.");
            return "error/error-page";
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        model.addAttribute("username", user.getUsername());
        return "email-success";
    }

    @GetMapping("/resend-verification")
    public String resendVerification(Model model) {
        return "resend-email";
    }

    @PostMapping("/resend-verification")
    public String resendVerificationEmail(@RequestParam String email) {

        // same answer whether or not the email exists, so nobody can probe for registered emails
        userRepository.findByEmail(email.trim()).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                String token = UUID.randomUUID().toString();
                user.setVerificationToken(token);
                userRepository.save(user);
                emailService.sendVerificationEmail(user.getEmail(), token);
            }
        });
        return "redirect:/login?resent";
    }
}
