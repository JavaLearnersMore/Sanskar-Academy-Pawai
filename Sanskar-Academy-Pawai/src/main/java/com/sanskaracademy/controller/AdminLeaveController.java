package com.sanskaracademy.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.LeaveApplication;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.LeaveService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/leaves")
@RequiredArgsConstructor
public class AdminLeaveController {

    private final LeaveService leaveService;
    private final UserRepository userRepository;

    @GetMapping
    public String page(@RequestParam(defaultValue = "ALL") String filter, Model model) {

        List<LeaveApplication> all = leaveService.getAll();
        List<LeaveApplication> shown = all;
        final String f = filter;

        switch (filter) {
            case "PENDING":
            case "APPROVED":
            case "REJECTED":
                shown = all.stream().filter(l -> l.getStatus().name().equals(f)).toList();
                break;
            case "TEACHER":
                shown = all.stream().filter(l -> l.getStudent() == null).toList();
                break;
            case "STUDENT":
                shown = all.stream().filter(l -> l.getStudent() != null).toList();
                break;
            default:
                filter = "ALL";
        }

        model.addAttribute("leaves", shown);
        model.addAttribute("filter", filter);
        model.addAttribute("pendingCount", leaveService.countPending());
        return "admin/leaves";
    }

    @PostMapping("/{id}/review")
    public String review(@PathVariable Long id,
                         @RequestParam String action,
                         @RequestParam(required = false) String remarks,
                         Principal principal,
                         RedirectAttributes ra) {
        try {
            User admin = userRepository.findByUsername(principal.getName()).orElseThrow();
            leaveService.review(id, admin, "APPROVE".equals(action), remarks);
            ra.addFlashAttribute("success", "Leave " + ("APPROVE".equals(action) ? "approved" : "rejected"));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/leaves";
    }
}
