package com.sanskaracademy.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.dto.AiQuestion;
import com.sanskaracademy.dto.ChatLine;
import com.sanskaracademy.dto.LearnState;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AcademicService;
import com.sanskaracademy.service.AiTutorService;
import com.sanskaracademy.service.ExamService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** AI tutor: learn a chapter, ask doubts, practise questions. Nothing here is stored in the database. */
@Controller
@RequestMapping("/student/learn")
@RequiredArgsConstructor
public class StudentLearnController {

    private static final String KEY = "learnState";

    private final AiTutorService aiTutorService;
    private final AcademicService academicService;
    private final UserRepository userRepository;
    private final ExamService examService;

    private static final String LOCK_MSG =
            "AI Learn is locked while your exam is running. Finish and submit the exam first.";

    private User me(Principal p) {
        return userRepository.findByUsername(p.getName()).orElseThrow();
    }

    private LearnState state(HttpSession session) {
        Object o = session.getAttribute(KEY);
        if (o instanceof LearnState s) return s;
        LearnState s = new LearnState();
        session.setAttribute(KEY, s);
        return s;
    }

    private String className(Long studentId) {
        return academicService.getStudentClass(studentId).map(c -> c.getClassName()).orElse("");
    }

    @GetMapping
    public String page(Principal principal, HttpSession session, Model model, RedirectAttributes ra) {
        User student = me(principal);
        if (examService.hasActiveExam(student.getId())) {
            ra.addFlashAttribute("error", LOCK_MSG);
            return "redirect:/student/exams";
        }
        model.addAttribute("state", state(session));
        model.addAttribute("aiEnabled", aiTutorService.isEnabled());
        model.addAttribute("className", className(student.getId()));
        return "student/learn";
    }

    @PostMapping("/explain")
    public String explain(@RequestParam String subject,
                          @RequestParam String topic,
                          @RequestParam(defaultValue = "Simple") String level,
                          Principal principal, HttpSession session, RedirectAttributes ra) {
        if (examService.hasActiveExam(me(principal).getId())) {
            ra.addFlashAttribute("error", LOCK_MSG);
            return "redirect:/student/exams";
        }
        if (topic.isBlank() || subject.isBlank()) {
            ra.addFlashAttribute("error", "Please enter a subject and a chapter");
            return "redirect:/student/learn";
        }
        try {
            User s = me(principal);
            String lesson = aiTutorService.explain(s.getId(), className(s.getId()), subject.trim(), topic.trim(), level);
            LearnState st = new LearnState();
            st.setSubjectName(subject.trim());
            st.setTopic(topic.trim());
            st.setLevel(level);
            st.setLesson(lesson);
            session.setAttribute(KEY, st);
            return "redirect:/student/learn#lessonCard";
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/learn";
    }

    @PostMapping("/ask")
    public String ask(@RequestParam String question,
                      Principal principal, HttpSession session, RedirectAttributes ra) {
        if (examService.hasActiveExam(me(principal).getId())) {
            ra.addFlashAttribute("error", LOCK_MSG);
            return "redirect:/student/exams";
        }
        LearnState st = state(session);
        if (!st.hasLesson() || question.isBlank()) {
            return "redirect:/student/learn";
        }
        try {
            User s = me(principal);
            List<ChatLine> history = st.getChat();
            String answer = aiTutorService.answerDoubt(s.getId(), className(s.getId()),
                    st.getTopic(), st.getLesson(), history, question.trim());
            st.getChat().add(new ChatLine("Student", question.trim()));
            st.getChat().add(new ChatLine("Tutor", answer));
            // keep the session small
            while (st.getChat().size() > 20) st.getChat().remove(0);
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/learn#chat";
    }

    @PostMapping("/practice")
    public String practice(@RequestParam(defaultValue = "5") int count,
                           @RequestParam(defaultValue = "Medium") String difficulty,
                           Principal principal, HttpSession session, RedirectAttributes ra) {
        if (examService.hasActiveExam(me(principal).getId())) {
            ra.addFlashAttribute("error", LOCK_MSG);
            return "redirect:/student/exams";
        }
        LearnState st = state(session);
        if (!st.hasLesson()) {
            ra.addFlashAttribute("error", "Learn a chapter first");
            return "redirect:/student/learn";
        }
        try {
            User s = me(principal);
            List<AiQuestion> qs = aiTutorService.generateQuestions(s.getId(), className(s.getId()),
                    st.getSubjectName(), st.getTopic(), count, difficulty);
            st.setPracticeTopic(st.getTopic());
            st.setPracticeList(qs);
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/learn#practice";
    }

    @PostMapping("/reset")
    public String reset(HttpSession session) {
        session.removeAttribute(KEY);
        return "redirect:/student/learn";
    }
}
