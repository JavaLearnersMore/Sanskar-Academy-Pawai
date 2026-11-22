package com.sanskaracademy.controller;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.service.DiaryService;
import com.sanskaracademy.service.DoubtService;
import com.sanskaracademy.service.HomeworkService;
import com.sanskaracademy.service.MaterialService;
import com.sanskaracademy.service.ProgressService;
import com.sanskaracademy.service.SchoolAccess;
import com.sanskaracademy.service.SyllabusService;

/** Student pages (can act) and parent pages (read only, per child). Both use the templates in templates/learn/. */
@Controller
public class StudentLearningController {

    private final SchoolAccess access;
    private final HomeworkService homework;
    private final MaterialService materials;
    private final DoubtService doubts;
    private final SyllabusService syllabus;
    private final DiaryService diary;
    private final ProgressService progress;

    public StudentLearningController(SchoolAccess access, HomeworkService homework, MaterialService materials,
                                     DoubtService doubts, SyllabusService syllabus, DiaryService diary,
                                     ProgressService progress) {
        this.access = access;
        this.homework = homework;
        this.materials = materials;
        this.doubts = doubts;
        this.syllabus = syllabus;
        this.diary = diary;
        this.progress = progress;
    }

    /** puts "who am I looking at" into the model; returns the student whose data is shown (null if parent has no child) */
    private User who(String base, Long childId, Model m, Principal p) {
        User me = access.me(p);
        m.addAttribute("base", base);
        m.addAttribute("parentView", "/parent".equals(base));
        if ("/parent".equals(base)) {
            User kid = access.pickChild(me.getId(), childId);
            m.addAttribute("children", access.childrenOf(me.getId()));
            m.addAttribute("child", kid);
            return kid;
        }
        m.addAttribute("child", me);
        return me;
    }

    // ------------------------------------------------------------------ homework
    @GetMapping({ "/student/homework", "/parent/homework" })
    public String homework(@RequestParam(required = false) Long child, Model m, Principal p,
                           jakarta.servlet.http.HttpServletRequest req) {
        String base = req.getRequestURI().startsWith("/parent") ? "/parent" : "/student";
        User s = who(base, child, m, p);
        m.addAttribute("rows", s == null ? java.util.List.of() : homework.forStudent(s.getId()));
        m.addAttribute("today", LocalDate.now());
        return "learn/homework";
    }

    @PostMapping("/student/homework/{id}/submit")
    public String submit(@PathVariable Long id, @RequestParam(required = false) String answer,
                         @RequestParam(required = false) MultipartFile file, Principal p, RedirectAttributes ra) {
        try {
            homework.submit(access.me(p), id, answer, file);
            ra.addFlashAttribute("success", "Homework submitted. Your teacher was told.");
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Could not save the file");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/homework";
    }

    // ------------------------------------------------------------------ study material
    @GetMapping({ "/student/materials", "/parent/materials" })
    public String materials(@RequestParam(required = false) Long child, Model m, Principal p,
                            jakarta.servlet.http.HttpServletRequest req) {
        String base = req.getRequestURI().startsWith("/parent") ? "/parent" : "/student";
        User s = who(base, child, m, p);
        m.addAttribute("list", s == null ? java.util.List.of() : materials.forStudent(s.getId()));
        return "learn/materials";
    }

    // ------------------------------------------------------------------ doubts (student only)
    @GetMapping("/student/doubts")
    public String doubtsPage(Model m, Principal p) {
        User me = who("/student", null, m, p);
        Long classId = access.classOf(me.getId());
        m.addAttribute("subjects", classId == null ? java.util.List.of() : access.subjectsOfClass(classId));
        m.addAttribute("list", doubts.ofStudent(me.getId()));
        return "learn/doubts";
    }

    @PostMapping("/student/doubts")
    public String doubtAsk(@RequestParam Long subjectId, @RequestParam String question, Principal p, RedirectAttributes ra) {
        try {
            doubts.ask(access.me(p), subjectId, question);
            ra.addFlashAttribute("success", "Your doubt was sent to the teacher.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/doubts";
    }

    // ------------------------------------------------------------------ progress / syllabus / diary
    @GetMapping({ "/student/progress", "/parent/progress" })
    public String progressPage(@RequestParam(required = false) Long child, Model m, Principal p,
                               jakarta.servlet.http.HttpServletRequest req) {
        String base = req.getRequestURI().startsWith("/parent") ? "/parent" : "/student";
        User s = who(base, child, m, p);
        if (s != null) m.addAttribute("pr", progress.progressOf(s.getId()));
        return "learn/progress";
    }

    @GetMapping({ "/student/syllabus", "/parent/syllabus" })
    public String syllabusPage(@RequestParam(required = false) Long child, Model m, Principal p,
                               jakarta.servlet.http.HttpServletRequest req) {
        String base = req.getRequestURI().startsWith("/parent") ? "/parent" : "/student";
        User s = who(base, child, m, p);
        m.addAttribute("list", s == null ? java.util.List.of() : syllabus.forClass(access.classOf(s.getId())));
        m.addAttribute("today", LocalDate.now());
        return "learn/syllabus";
    }

    @GetMapping({ "/student/diary", "/parent/diary" })
    public String diaryPage(@RequestParam(required = false) Long child, Model m, Principal p,
                            jakarta.servlet.http.HttpServletRequest req) {
        String base = req.getRequestURI().startsWith("/parent") ? "/parent" : "/student";
        User s = who(base, child, m, p);
        m.addAttribute("list", s == null ? java.util.List.of()
                : ("/parent".equals(base) ? diary.forParentView(s.getId()) : diary.forStudent(s.getId())));
        return "learn/diary";
    }
}
