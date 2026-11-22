package com.sanskaracademy.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.LeaveApplication;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AcademicService;
import com.sanskaracademy.service.LeaveService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/parent/leave")
@RequiredArgsConstructor
public class ParentLeaveController {

    private final LeaveService leaveService;
    private final AcademicService academicService;
    private final UserRepository userRepository;

    @GetMapping
    public String page(Principal principal, Model model) {

        User parent = userRepository.findByUsername(principal.getName()).orElseThrow();
        List<User> children = academicService.getChildren(parent.getId());

        List<LeaveApplication> leaves = new ArrayList<>();
        for (User c : children) {
            leaves.addAll(leaveService.getForStudent(c.getId()));
        }
        leaves.sort((a, b) -> b.getAppliedAt().compareTo(a.getAppliedAt()));

        model.addAttribute("children", children);
        model.addAttribute("leaves", leaves);
        model.addAttribute("today", LocalDate.now());
        return "parent/leave";
    }

    @PostMapping("/apply")
    public String apply(@RequestParam Long studentId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                        @RequestParam String reason,
                        Principal principal, RedirectAttributes ra) {
        try {
            User parent = userRepository.findByUsername(principal.getName()).orElseThrow();
            leaveService.applyForStudent(parent, studentId, fromDate, toDate, reason);
            ra.addFlashAttribute("success", "Leave request sent to the teacher");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/parent/leave";
    }
}
