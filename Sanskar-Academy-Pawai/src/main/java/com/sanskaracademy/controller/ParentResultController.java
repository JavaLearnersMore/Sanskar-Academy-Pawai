package com.sanskaracademy.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.ExamAttempt;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AcademicService;
import com.sanskaracademy.service.ExamService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/parent/results")
@RequiredArgsConstructor
public class ParentResultController {

    private final ExamService examService;
    private final AcademicService academicService;
    private final UserRepository userRepository;

    @GetMapping
    public String list(Principal principal, Model model) {
        User parent = userRepository.findByUsername(principal.getName()).orElseThrow();

        List<User> children = academicService.getChildren(parent.getId());
        List<ChildResults> rows = new ArrayList<>();
        for (User c : children) {
            rows.add(new ChildResults(c, examService.getStudentResults(c.getId())));
        }
        model.addAttribute("rows", rows);
        return "parent/results";
    }

    @GetMapping("/{attemptId}")
    public String detail(@PathVariable Long attemptId, Principal principal, Model model, RedirectAttributes ra) {
        User parent = userRepository.findByUsername(principal.getName()).orElseThrow();
        try {
            ExamAttempt a = examService.getAttempt(attemptId);
            if (!academicService.isParentOf(parent.getId(), a.getStudent().getId())) {
                throw new IllegalArgumentException("This result does not belong to your child");
            }
            model.addAttribute("attempt", a);
            return "parent/result-detail";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/parent/results";
        }
    }

    public record ChildResults(User child, List<ExamAttempt> attempts) { }
}
