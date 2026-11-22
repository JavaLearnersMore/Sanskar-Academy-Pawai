package com.sanskaracademy.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sanskaracademy.entity.LeaveStatus;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.AttendanceRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AcademicService;
import com.sanskaracademy.service.LeaveService;
import com.sanskaracademy.service.SalaryService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final AcademicService academicService;
    private final LeaveService leaveService;
    private final SalaryService salaryService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {

        User teacher = userRepository.findByUsername(principal.getName()).orElseThrow();

        List<Subject> subjects = academicService.getTeacherSubjects(teacher.getId());
        long classCount = subjects.stream().map(s -> s.getClassEntity().getId()).distinct().count();

        long pendingStudentLeaves = leaveService.getStudentLeavesForTeacher(teacher.getId())
                .stream().filter(l -> l.getStatus() == LeaveStatus.PENDING).count();

        model.addAttribute("teacher", teacher);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("subjects", subjects);
        model.addAttribute("subjectCount", subjects.size());
        model.addAttribute("classCount", classCount);
        model.addAttribute("totalStudents", userRepository.findAllStudents().size());
        model.addAttribute("markedToday",
                attendanceRepository.countByMarkedByIdAndAttendanceDate(teacher.getId(), LocalDate.now()));
        model.addAttribute("todayLectures", academicService.getTeacherLecturesToday(teacher.getId()));
        model.addAttribute("timetable", academicService.getTeacherLectures(teacher.getId()));
        model.addAttribute("pendingStudentLeaves", pendingStudentLeaves);
        model.addAttribute("salaries", salaryService.getForTeacher(teacher.getId()).stream().limit(3).toList());

        return "teacher/dashboard";
    }
}
