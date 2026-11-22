package com.sanskaracademy.controller;

import java.security.Principal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.LeaveService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/student/leave")
@RequiredArgsConstructor
public class StudentLeaveController {

    private final LeaveService leaveService;
    private final UserRepository userRepository;

    @GetMapping
    public String page(Principal principal, Model model) {
        User student = userRepository.findByUsername(principal.getName()).orElseThrow();
        model.addAttribute("leaves", leaveService.getForStudent(student.getId()));
        model.addAttribute("today", LocalDate.now());
        return "student/leave";
    }

    @PostMapping("/apply")
    public String apply(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                        @RequestParam String reason,
                        Principal principal, RedirectAttributes ra) {
        try {
            User student = userRepository.findByUsername(principal.getName()).orElseThrow();
            leaveService.applyForStudent(student, student.getId(), fromDate, toDate, reason);
            ra.addFlashAttribute("success", "Leave request sent");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/leave";
    }
}
