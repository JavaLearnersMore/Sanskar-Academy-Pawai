package com.sanskaracademy.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.dto.AiQuestion;
import com.sanskaracademy.entity.Exam;
import com.sanskaracademy.entity.QuestionType;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AcademicService;
import com.sanskaracademy.service.AiTutorService;
import com.sanskaracademy.service.ExamService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/teacher/exams")
@RequiredArgsConstructor
public class TeacherExamController {

    private final ExamService examService;
    private final AiTutorService aiTutorService;
    private final AcademicService academicService;
    private final UserRepository userRepository;

    private User me(Principal p) {
        return userRepository.findByUsername(p.getName()).orElseThrow();
    }

    // ---------------- list + create ----------------
    @GetMapping
    public String list(Principal principal, Model model) {
        User t = me(principal);
        model.addAttribute("exams", examService.getTeacherExams(t.getId()));
        model.addAttribute("subjects", academicService.getTeacherSubjects(t.getId()));
        model.addAttribute("classes", academicService.getAllClasses());
        model.addAttribute("pendingReview", examService.countPendingReview(t.getId()));
        return "teacher/exams";
    }

    @PostMapping("/create")
    public String create(@RequestParam Long subjectId,
                         @RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "30") int durationMinutes,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDate,
                         Principal principal, RedirectAttributes ra) {
        try {
            Exam e = examService.createExam(me(principal), subjectId, title, description, durationMinutes, dueDate);
            ra.addFlashAttribute("success", "Exam created. Now add questions.");
            return "redirect:/teacher/exams/" + e.getId();
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/exams";
        }
    }

    /** teacher adds a subject of his own (so the exam dropdown is never empty) */
    @PostMapping("/my-subject")
    public String addMySubject(@RequestParam String subjectName, @RequestParam Long classId,
                               Principal principal, RedirectAttributes ra) {
        try {
            academicService.createSubject(subjectName, classId, me(principal).getId());
            ra.addFlashAttribute("success", "Subject added. You can now create an exam for it.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/exams";
    }

    // ---------------- live monitor ----------------
    @GetMapping("/{id}/monitor")
    public String monitor(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        try {
            Long tid = me(principal).getId();
            java.util.List<com.sanskaracademy.dto.MonitorRowDto> rows = examService.getMonitor(id, tid);
            model.addAttribute("exam", examService.getTeacherExam(id, tid));
            model.addAttribute("rows", rows);
            model.addAttribute("total", rows.size());
            model.addAttribute("notStarted", rows.stream().filter(r -> "NOT_STARTED".equals(r.getState())).count());
            model.addAttribute("writing", rows.stream().filter(r -> "IN_PROGRESS".equals(r.getState())).count());
            model.addAttribute("done", rows.stream().filter(r -> "SUBMITTED".equals(r.getState()) || "REVIEW".equals(r.getState())).count());
            return "teacher/exam-monitor";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/exams";
        }
    }

    // ---------------- builder ----------------
    @GetMapping("/{id}")
    public String builder(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("exam", examService.getTeacherExam(id, me(principal).getId()));
            model.addAttribute("aiEnabled", aiTutorService.isEnabled());
            return "teacher/exam-builder";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/exams";
        }
    }

    @PostMapping("/{id}/questions")
    public String addQuestion(@PathVariable Long id,
                              @RequestParam QuestionType type,
                              @RequestParam String questionText,
                              @RequestParam(required = false) String optionA,
                              @RequestParam(required = false) String optionB,
                              @RequestParam(required = false) String optionC,
                              @RequestParam(required = false) String optionD,
                              @RequestParam(required = false) String correctOption,
                              @RequestParam(required = false) String modelAnswer,
                              @RequestParam(defaultValue = "1") int marks,
                              Principal principal, RedirectAttributes ra) {
        try {
            examService.addQuestion(id, me(principal).getId(), type, questionText,
                    optionA, optionB, optionC, optionD, correctOption, modelAnswer, marks);
            ra.addFlashAttribute("success", "Question added");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/exams/" + id;
    }

    @PostMapping("/{id}/ai-questions")
    public String aiQuestions(@PathVariable Long id,
                              @RequestParam String topic,
                              @RequestParam(defaultValue = "5") int count,
                              @RequestParam(defaultValue = "Medium") String difficulty,
                              @RequestParam(defaultValue = "1") int marksEach,
                              Principal principal, RedirectAttributes ra) {
        try {
            User t = me(principal);
            Exam exam = examService.getTeacherExam(id, t.getId());
            List<AiQuestion> qs = aiTutorService.generateQuestions(t.getId(),
                    exam.getSubject().getClassEntity().getClassName(),
                    exam.getSubject().getSubjectName(), topic, count, difficulty);
            int added = examService.addAiQuestions(id, t.getId(), qs, marksEach);
            ra.addFlashAttribute("success", added + " AI questions added. Please review them before publishing.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/exams/" + id;
    }

    @PostMapping("/{id}/questions/{qid}/delete")
    public String deleteQuestion(@PathVariable Long id, @PathVariable Long qid,
                                 Principal principal, RedirectAttributes ra) {
        try {
            examService.deleteQuestion(id, qid, me(principal).getId());
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/exams/" + id;
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            examService.publish(id, me(principal).getId());
            ra.addFlashAttribute("success", "Exam published. Students of the class have been notified.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/exams/" + id;
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            examService.close(id, me(principal).getId());
            ra.addFlashAttribute("success", "Exam closed");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/exams/" + id + "/results";
    }

    // ---------------- results ----------------
    @GetMapping("/{id}/results")
    public String results(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        try {
            Long tid = me(principal).getId();
            model.addAttribute("exam", examService.getTeacherExam(id, tid));
            model.addAttribute("stats", examService.getStats(id, tid));
            model.addAttribute("attempts", examService.getSubmittedAttempts(id, tid));
            return "teacher/exam-results";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/exams";
        }
    }

    @GetMapping("/attempts/{attemptId}")
    public String attempt(@PathVariable Long attemptId, Principal principal, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("attempt", examService.getAttemptForTeacher(attemptId, me(principal).getId()));
            return "teacher/attempt-detail";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/exams";
        }
    }

    /** form fields: marks_{answerId}, feedback_{answerId} */
    @PostMapping("/attempts/{attemptId}/marks")
    public String saveMarks(@PathVariable Long attemptId, HttpServletRequest req,
                            Principal principal, RedirectAttributes ra) {
        Map<Long, Integer> marks = new HashMap<>();
        Map<Long, String> feedback = new HashMap<>();
        try {
            req.getParameterMap().forEach((k, v) -> {
                if (v.length == 0) return;
                if (k.startsWith("marks_") && !v[0].isBlank()) {
                    marks.put(Long.valueOf(k.substring(6)), Integer.valueOf(v[0].trim()));
                } else if (k.startsWith("feedback_")) {
                    feedback.put(Long.valueOf(k.substring(9)), v[0]);
                }
            });
            examService.overrideMarks(attemptId, me(principal).getId(), marks, feedback);
            ra.addFlashAttribute("success", "Marks saved. Student and parents were notified.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/exams/attempts/" + attemptId;
    }

    // ---------------- insights ----------------
    @GetMapping("/insights")
    public String insights(Principal principal, Model model) {
        model.addAttribute("levels", examService.getStudentLevels(me(principal).getId()));
        return "teacher/insights";
    }
}