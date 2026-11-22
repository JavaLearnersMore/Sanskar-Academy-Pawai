package com.sanskaracademy.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.dto.AiReasoningQuestion;
import com.sanskaracademy.entity.Exam;
import com.sanskaracademy.entity.QuestionType;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AiReasoningService;
import com.sanskaracademy.service.ExamGuardService;
import com.sanskaracademy.service.ExamService;

/** Teacher: anti-cheating settings of an online exam, and AI "thinking" (written) questions. */
@Controller
@RequestMapping("/teacher/exams")
public class TeacherExamSecurityController {

    private final ExamGuardService guard;
    private final ExamService examService;
    private final AiReasoningService reasoning;
    private final UserRepository users;

    public TeacherExamSecurityController(ExamGuardService guard, ExamService examService,
                                         AiReasoningService reasoning, UserRepository users) {
        this.guard = guard;
        this.examService = examService;
        this.reasoning = reasoning;
        this.users = users;
    }

    private User me(Principal p) {
        return users.findByUsername(p.getName()).orElseThrow();
    }

    @PostMapping("/{id}/security")
    public String save(@PathVariable Long id,
                       @RequestParam(defaultValue = "false") boolean strict,
                       @RequestParam(defaultValue = "3") int maxWarnings,
                       @RequestParam(defaultValue = "false") boolean blockCopy,
                       @RequestParam(defaultValue = "false") boolean shuffle,
                       @RequestParam(required = false) Integer poolSize,
                       @RequestParam(required = false) Integer perQuestionSeconds,
                       Principal principal, RedirectAttributes ra) {
        try {
            guard.saveSettings(id, me(principal).getId(), strict, maxWarnings, blockCopy, shuffle, poolSize, perQuestionSeconds);
            ra.addFlashAttribute("success", "Exam safety settings saved");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/exams/" + id;
    }

    @PostMapping("/{id}/ai-written")
    public String aiWritten(@PathVariable Long id, @RequestParam String topic,
                            @RequestParam(defaultValue = "3") int count,
                            @RequestParam(defaultValue = "Medium") String difficulty,
                            @RequestParam(defaultValue = "WHY") String style,
                            @RequestParam(defaultValue = "3") int marksEach,
                            Principal principal, RedirectAttributes ra) {
        try {
            User t = me(principal);
            Exam exam = examService.getTeacherExam(id, t.getId());
            List<AiReasoningQuestion> qs = reasoning.generate(t.getId(), exam.getSubject().getClassEntity().getClassName(),
                    exam.getSubject().getSubjectName(), topic, count, difficulty, style);
            int marks = Math.max(1, Math.min(marksEach, 20));
            for (AiReasoningQuestion q : qs) {
                examService.addQuestion(id, t.getId(), QuestionType.SHORT, q.question(), null, null, null, null, null,
                        q.modelAnswer(), marks);
            }
            ra.addFlashAttribute("success", qs.size() + " thinking questions added. Please read them before publishing.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/exams/" + id;
    }
}
