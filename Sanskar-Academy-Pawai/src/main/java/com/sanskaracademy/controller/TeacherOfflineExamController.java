package com.sanskaracademy.controller;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.Exam;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.ExamService;
import com.sanskaracademy.service.OfflineExamService;

/** Paper (offline) exams: create, print the question paper, enter marks + answer-sheet photos, publish. */
@Controller
@RequestMapping("/teacher/exams")
public class TeacherOfflineExamController {

    private final OfflineExamService offline;
    private final ExamService examService;
    private final UserRepository users;

    public TeacherOfflineExamController(OfflineExamService offline, ExamService examService, UserRepository users) {
        this.offline = offline;
        this.examService = examService;
        this.users = users;
    }

    private User me(Principal p) {
        return users.findByUsername(p.getName()).orElseThrow();
    }

    @PostMapping("/offline/create")
    public String create(@RequestParam Long subjectId, @RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "60") int durationMinutes,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate examDate,
                         Principal principal, RedirectAttributes ra) {
        try {
            Exam e = offline.create(me(principal), subjectId, title, description, durationMinutes, examDate);
            ra.addFlashAttribute("success", "Paper exam created. Add the questions, then print the paper.");
            return "redirect:/teacher/exams/" + e.getId();
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/exams";
        }
    }

    /** printable question paper; ?key=1 adds the answer key (for the teacher only) */
    @GetMapping("/{id}/paper")
    public String paper(@PathVariable Long id, @RequestParam(defaultValue = "0") int key,
                        Principal principal, Model model, RedirectAttributes ra) {
        try {
            Exam e = examService.getTeacherExam(id, me(principal).getId());
            model.addAttribute("exam", e);
            model.addAttribute("info", offline.infoOf(id));
            model.addAttribute("withKey", key == 1);
            return "teacher/exam-paper";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/exams";
        }
    }

    @GetMapping("/{id}/offline")
    public String marks(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        try {
            Long tid = me(principal).getId();
            Exam e = examService.getTeacherExam(id, tid);
            List<OfflineExamService.Row> rows = offline.rows(id, tid);
            int entered = 0;
            for (OfflineExamService.Row r : rows) if (r.getMarks() != null) entered++;
            model.addAttribute("exam", e);
            model.addAttribute("info", offline.infoOf(id));
            model.addAttribute("rows", rows);
            model.addAttribute("entered", entered);
            return "teacher/exam-offline";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/exams";
        }
    }

    @PostMapping("/{id}/offline/student/{studentId}")
    public String saveStudent(@PathVariable Long id, @PathVariable Long studentId,
                              @RequestParam(required = false) Integer marks,
                              @RequestParam(required = false) String remarks,
                              @RequestParam(value = "files", required = false) List<MultipartFile> files,
                              Principal principal, RedirectAttributes ra) {
        try {
            offline.saveStudent(id, me(principal).getId(), studentId, marks, remarks, files);
            ra.addFlashAttribute("success", "Saved");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (IOException ex) {
            ra.addFlashAttribute("error", "Could not save the file. Please try again.");
        }
        return "redirect:/teacher/exams/" + id + "/offline#s" + studentId;
    }

    @PostMapping("/{id}/offline/sheet/{sheetId}/delete")
    public String deleteSheet(@PathVariable Long id, @PathVariable Long sheetId, Principal principal, RedirectAttributes ra) {
        try {
            offline.deleteSheet(sheetId, me(principal).getId());
            ra.addFlashAttribute("success", "File removed");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/exams/" + id + "/offline";
    }

    @PostMapping("/{id}/offline/publish")
    public String publish(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            int n = offline.publish(id, me(principal).getId());
            ra.addFlashAttribute("success", n > 0
                    ? "Results published. " + n + " student(s) and their parents were informed."
                    : "Results updated.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/exams/" + id + "/offline";
    }
}
