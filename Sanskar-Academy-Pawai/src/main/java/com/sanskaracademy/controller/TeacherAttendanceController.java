package com.sanskaracademy.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AcademicService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/teacher/attendance")
@RequiredArgsConstructor
public class TeacherAttendanceController {

    private final AcademicService academicService;
    private final UserRepository userRepository;

    @GetMapping
    public String sheet(@RequestParam(required = false) Long subjectId,
                        @RequestParam(required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Principal principal,
                        Model model) {

        User teacher = userRepository.findByUsername(principal.getName()).orElseThrow();
        LocalDate day = date != null ? date : LocalDate.now();

        model.addAttribute("subjects", academicService.getTeacherSubjects(teacher.getId()));
        model.addAttribute("date", day);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("subjectId", subjectId);

        if (subjectId != null) {
            try {
                Subject subject = academicService.getTeacherSubject(subjectId, teacher.getId());
                model.addAttribute("subject", subject);
                model.addAttribute("rows", academicService.getAttendanceRows(teacher.getId(), subjectId, day));
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "teacher/attendance";
    }

    @PostMapping("/save")
    public String save(@RequestParam Long subjectId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam Map<String, String> params,
                       Principal principal,
                       RedirectAttributes ra) {

        // form fields look like  status_12=PRESENT
        Map<Long, String> statuses = new HashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getKey().startsWith("status_")) {
                try {
                    statuses.put(Long.valueOf(e.getKey().substring(7)), e.getValue());
                } catch (NumberFormatException ignored) {
                    // skip bad field
                }
            }
        }

        try {
            User teacher = userRepository.findByUsername(principal.getName()).orElseThrow();
            int saved = academicService.markAttendance(teacher.getId(), subjectId, date, statuses);
            ra.addFlashAttribute("success", "Attendance saved for " + saved + " students");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/attendance?subjectId=" + subjectId + "&date=" + date;
    }
}
