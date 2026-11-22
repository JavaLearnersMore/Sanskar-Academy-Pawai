package com.sanskaracademy.controller;

import java.security.Principal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.LeaveService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/teacher/leave")
@RequiredArgsConstructor
public class TeacherLeaveController {

    private final LeaveService leaveService;
    private final UserRepository userRepository;

    @GetMapping
    public String page(Principal principal, Model model) {
        User teacher = userRepository.findByUsername(principal.getName()).orElseThrow();
        model.addAttribute("myLeaves", leaveService.getByApplicant(teacher.getId()));
        model.addAttribute("studentLeaves", leaveService.getStudentLeavesForTeacher(teacher.getId()));
        model.addAttribute("today", LocalDate.now());
        return "teacher/leave";
    }

    @PostMapping("/apply")
    public String apply(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                        @RequestParam String reason,
                        Principal principal, RedirectAttributes ra) {
        try {
            User teacher = userRepository.findByUsername(principal.getName()).orElseThrow();
            leaveService.applyForTeacher(teacher, fromDate, toDate, reason);
            ra.addFlashAttribute("success", "Leave request sent to the admin");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/leave";
    }

    @PostMapping("/review/{id}")
    public String review(@PathVariable Long id,
                         @RequestParam String action,
                         @RequestParam(required = false) String remarks,
                         Principal principal, RedirectAttributes ra) {
        try {
            User teacher = userRepository.findByUsername(principal.getName()).orElseThrow();
            leaveService.review(id, teacher, "APPROVE".equals(action), remarks);
            ra.addFlashAttribute("success", "Leave " + ("APPROVE".equals(action) ? "approved" : "rejected"));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/leave";
    }
}
