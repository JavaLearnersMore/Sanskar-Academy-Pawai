package com.sanskaracademy.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.ExamAttempt;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.ExamService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/student/exams")
@RequiredArgsConstructor
public class StudentExamController {

    private final ExamService examService;
    private final UserRepository userRepository;
    private final com.sanskaracademy.service.ExamGuardService guardService;

    private User me(Principal p) {
        return userRepository.findByUsername(p.getName()).orElseThrow();
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        Long id = me(principal).getId();
        model.addAttribute("exams", examService.getStudentExams(id));
        model.addAttribute("results", examService.getStudentResults(id));
        return "student/exams";
    }

    @PostMapping("/{examId}/start")
    public String start(@PathVariable Long examId, Principal principal, RedirectAttributes ra) {
        try {
            ExamAttempt a = examService.startAttempt(me(principal).getId(), examId);
            return "redirect:/student/exams/attempt/" + a.getId();
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/student/exams";
        }
    }

    /** exam paper (while IN_PROGRESS) or the result (after submit) */
    @GetMapping("/attempt/{attemptId}")
    public String attempt(@PathVariable Long attemptId, Principal principal, Model model, RedirectAttributes ra) {
        try {
            ExamAttempt a = examService.getStudentAttempt(attemptId, me(principal).getId());
            model.addAttribute("attempt", a);
            model.addAttribute("exam", a.getExam());
            if (a.getStatus() == com.sanskaracademy.entity.AttemptStatus.IN_PROGRESS) {
                if (a.isExpired()) {
                    examService.submitAttempt(a.getStudent().getId(), a.getId(), Map.of());
                    return "redirect:/student/exams/attempt/" + a.getId();
                }
                model.addAttribute("paper", guardService.paper(a));
                model.addAttribute("sec", guardService.settingsOf(a.getExam().getId()));
                model.addAttribute("qstate", guardService.state(a));
                model.addAttribute("violations", guardService.violationsOf(a.getId()));
                model.addAttribute("paperTotal", guardService.paperTotal(a));
                return "student/exam-take";
            }
            model.addAttribute("violations", guardService.violationsOf(a.getId()));
            model.addAttribute("autoSubmitted", guardService.autoSubmitted(a.getId()));
            return "student/exam-result";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/student/exams";
        }
    }

    /** form fields: q_{questionId} = A/B/C/D or the written answer */
    @PostMapping("/attempt/{attemptId}/submit")
    public String submit(@PathVariable Long attemptId, HttpServletRequest req,
                         Principal principal, RedirectAttributes ra) {
        Map<Long, String> answers = new HashMap<>();
        req.getParameterMap().forEach((k, v) -> {
            if (k.startsWith("q_") && v.length > 0) {
                try {
                    answers.put(Long.valueOf(k.substring(2)), v[0]);
                } catch (NumberFormatException ignored) {
                    // ignore unknown field
                }
            }
        });
        try {
            examService.submitAttempt(me(principal).getId(), attemptId, answers);
            ra.addFlashAttribute("success", "Exam submitted!");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/exams/attempt/" + attemptId;
    }
}
