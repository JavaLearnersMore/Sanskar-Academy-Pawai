package com.sanskaracademy.controller;

import com.sanskaracademy.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/parent")
@RequiredArgsConstructor
public class ParentController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {

        model.addAttribute("notifications",
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L));

        return "parent/dashboard";
    }
}