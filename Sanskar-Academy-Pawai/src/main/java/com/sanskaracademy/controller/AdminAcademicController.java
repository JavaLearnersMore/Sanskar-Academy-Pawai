package com.sanskaracademy.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.sanskaracademy.repository.RoleRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AcademicService;

import lombok.RequiredArgsConstructor;

/** Admin: classes, subjects (teacher assignment), students in classes, timetable, parent links. */
@Controller
@RequestMapping("/admin/academics")
@RequiredArgsConstructor
public class AdminAcademicController {

    private final AcademicService academicService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @GetMapping
    public String page(@RequestParam(defaultValue = "classes") String tab, Model model) {

        List<User> parents = roleRepository.findByName("PARENT")
                .map(userRepository::findByRole).orElse(List.of());

        Map<Long, List<User>> childrenByParent = new HashMap<>();
        for (User p : parents) {
            childrenByParent.put(p.getId(), academicService.getChildren(p.getId()));
        }

        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;

        model.addAttribute("tab", tab);
        model.addAttribute("defaultYear", startYear + "-" + String.valueOf(startYear + 1).substring(2));
        model.addAttribute("classes", academicService.getAllClasses());
        model.addAttribute("studentCount", academicService.getStudentCountByClass());
        model.addAttribute("subjects", academicService.getAllSubjects());
        model.addAttribute("teachers", userRepository.findAllTeachers());
        model.addAttribute("students", userRepository.findAllStudents());
        model.addAttribute("classByStudent", academicService.getClassByStudent());
        model.addAttribute("lectures", academicService.getAllLectures());
        model.addAttribute("days", DayOfWeek.values());
        model.addAttribute("parents", parents);
        model.addAttribute("childrenByParent", childrenByParent);
        return "admin/academics";
    }

    @PostMapping("/class")
    public String createClass(@RequestParam String className, @RequestParam String academicYear,
                              RedirectAttributes ra) {
        try {
            academicService.createClass(className, academicYear);
            ra.addFlashAttribute("success", "Class created");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/academics?tab=classes";
    }

    @PostMapping("/enroll")
    public String enroll(@RequestParam Long studentId, @RequestParam Long classId, RedirectAttributes ra) {
        try {
            academicService.enrollStudent(studentId, classId);
            ra.addFlashAttribute("success", "Student added to class");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/academics?tab=classes";
    }

    @PostMapping("/unenroll/{studentId}")
    public String unenroll(@PathVariable Long studentId, RedirectAttributes ra) {
        academicService.unenrollStudent(studentId);
        ra.addFlashAttribute("success", "Student removed from class");
        return "redirect:/admin/academics?tab=classes";
    }

    @PostMapping("/subject")
    public String createSubject(@RequestParam String subjectName, @RequestParam Long classId,
                                @RequestParam Long teacherId, RedirectAttributes ra) {
        try {
            academicService.createSubject(subjectName, classId, teacherId);
            ra.addFlashAttribute("success", "Subject assigned to teacher");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/academics?tab=subjects";
    }

    @PostMapping("/subject/{id}/teacher")
    public String changeTeacher(@PathVariable Long id, @RequestParam Long teacherId, RedirectAttributes ra) {
        try {
            academicService.reassignTeacher(id, teacherId);
            ra.addFlashAttribute("success", "Teacher changed");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/academics?tab=subjects";
    }

    @PostMapping("/lecture")
    public String addLecture(@RequestParam Long subjectId,
                             @RequestParam DayOfWeek dayOfWeek,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
                             @RequestParam(required = false) String room,
                             RedirectAttributes ra) {
        try {
            academicService.addLecture(subjectId, dayOfWeek, startTime, endTime, room);
            ra.addFlashAttribute("success", "Lecture added to timetable");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/academics?tab=timetable";
    }

    @PostMapping("/lecture/{id}/edit")
    public String editLecture(@PathVariable Long id,
                              @RequestParam Long subjectId,
                              @RequestParam DayOfWeek dayOfWeek,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
                              @RequestParam(required = false) String room,
                              RedirectAttributes ra) {
        try {
            academicService.updateLecture(id, subjectId, dayOfWeek, startTime, endTime, room);
            ra.addFlashAttribute("success", "Lecture updated");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/academics?tab=timetable";
    }

    @PostMapping("/lecture/{id}/delete")
    public String deleteLecture(@PathVariable Long id, RedirectAttributes ra) {
        academicService.deleteLecture(id);
        ra.addFlashAttribute("success", "Lecture removed");
        return "redirect:/admin/academics?tab=timetable";
    }

    @PostMapping("/link")
    public String link(@RequestParam Long parentId, @RequestParam Long studentId, RedirectAttributes ra) {
        try {
            academicService.linkParent(parentId, studentId);
            ra.addFlashAttribute("success", "Parent linked to student");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/academics?tab=parents";
    }

    @PostMapping("/unlink")
    public String unlink(@RequestParam Long parentId, @RequestParam Long studentId, RedirectAttributes ra) {
        academicService.unlinkParent(parentId, studentId);
        ra.addFlashAttribute("success", "Link removed");
        return "redirect:/admin/academics?tab=parents";
    }
}
