package com.sanskaracademy.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sanskaracademy.dto.RegisterDto;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.EmailService;
import com.sanskaracademy.service.TotpService;
import com.sanskaracademy.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final TotpService totpService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @GetMapping("/logout")
    public String logout() {
    	return "redirect:/login";
    }

    @GetMapping("/verify")
    public String verifyPage() {
        return "verify";
    }

    @PostMapping("/verify")
    public String verifyCode(@RequestParam int code,
                             Principal principal) {

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow();

        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            return "redirect:/verify?error";
        }

        String role = user.getRole().getName();

        switch (role) {
            case "ADMIN":
                return "redirect:/admin/dashboard";
            case "TEACHER":
                return "redirect:/teacher/dashboard";
            case "STUDENT":
                return "redirect:/student/dashboard";
            case "PARENT":
                return "redirect:/parent/dashboard";
            default:
                return "redirect:/login";
        }
    }
    
 // =========================
    // SHOW REGISTER PAGE
    // =========================
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "register";
    }

 // =========================
    // HANDLE REGISTER
    // =========================
    @PostMapping("/register")
    public String registerUser(@ModelAttribute RegisterDto dto,
                               Model model) {

        userService.registerUser(dto);

        model.addAttribute("message",
                "Registration successful! Please verify your email.");

        return "login";
    }
 // =========================
    // EMAIL VERIFICATION
    // =========================
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        model.addAttribute("username", user.getUsername());

        return "email-success";
    }

    // =========================
    // QR CODE SETUP (2FA)
    // =========================
    @GetMapping("/setup-2fa")
    public String setup2FA(@RequestParam String username,
                           Model model) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String uri = "otpauth://totp/SanskarAcademy:"
                + user.getUsername()
                + "?secret=" + user.getTotpSecret()
                + "&issuer=SanskarAcademy";

        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + uri;

        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("username", user.getUsername());

        return "setup-2fa";
    }
    
    @PostMapping("/confirm-2fa")
    public String confirm2FA(@RequestParam String username) {

        User user = userRepository.findByUsername(username).orElseThrow();

        user.setEnabled(true);  // enable account
        userRepository.save(user);

        return "redirect:/login";
    }
    
    @GetMapping("/resend-verification")
    public String resendVerification(Model model) {
        model.addAttribute("emailForm", new RegisterDto());
        return "resend-email";
    }

    @PostMapping("/resend-verification")
    public String resendVerificationEmail(@RequestParam String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        userRepository.save(user);

        emailService.sendVerificationEmail(email, token);

        return "redirect:/login";
    }
}