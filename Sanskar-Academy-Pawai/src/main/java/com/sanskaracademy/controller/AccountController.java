package com.sanskaracademy.controller;

import java.security.Principal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.security.TwoFactorFilter;
import com.sanskaracademy.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** "My Account" for every role: change my password, get a new authenticator QR for myself. */
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    private User me(Principal p) {
        return userRepository.findByUsername(p.getName()).orElseThrow();
    }

    @GetMapping
    public String page(Principal principal, Model model) {
        User u = me(principal);
        model.addAttribute("fullName", u.getFullName());
        model.addAttribute("role", u.getRole().getName());
        return "account";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Principal principal, RedirectAttributes ra) {
        User u = me(principal);
        if (!passwordEncoder.matches(currentPassword, u.getPassword())) {
            ra.addFlashAttribute("error", "Current password is wrong.");
        } else if (newPassword.length() < 8 || newPassword.length() > 64) {
            ra.addFlashAttribute("error", "New password must be 8 to 64 characters.");
        } else if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New password and confirm password are not the same.");
        } else if (newPassword.equals(currentPassword)) {
            ra.addFlashAttribute("error", "New password must be different from the current one.");
        } else {
            u.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(u);
            ra.addFlashAttribute("success", "Password changed. Use the new password the next time you log in.");
        }
        return "redirect:/account";
    }

    /** new authenticator key for myself (new phone / lost app). Asks the password first. */
    @PostMapping("/regenerate-qr")
    public String regenerateQr(@RequestParam String currentPassword,
                               Principal principal, HttpSession session, RedirectAttributes ra) {
        User u = me(principal);
        if (!passwordEncoder.matches(currentPassword, u.getPassword())) {
            ra.addFlashAttribute("error", "Password is wrong. The authenticator key was not changed.");
            return "redirect:/account";
        }
        userService.regenerateSecretForUser(u.getUsername());   // also marks the QR as "not scanned yet"
        session.removeAttribute(TwoFactorFilter.SESSION_KEY);    // forces /verify -> /setup-2fa with the new QR
        return "redirect:/verify";
    }
}
