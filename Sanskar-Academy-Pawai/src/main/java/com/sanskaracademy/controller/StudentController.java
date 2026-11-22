package com.sanskaracademy.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sanskaracademy.entity.Attendance;
import com.sanskaracademy.entity.AttendanceStatus;
import com.sanskaracademy.entity.ClassEntity;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.AttendanceRepository;
import com.sanskaracademy.repository.NotificationRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AcademicService;
import com.sanskaracademy.service.ExamService;
import com.sanskaracademy.service.FeeService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationRepository notificationRepository;
    private final FeeService feeService;
    private final AcademicService academicService;
    private final ExamService examService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {

        User student = userRepository.findByUsername(principal.getName()).orElseThrow();
        Long id = student.getId();

        long present = attendanceRepository.countAttendanceByStatus(id, AttendanceStatus.PRESENT);
        long absent = attendanceRepository.countAttendanceByStatus(id, AttendanceStatus.ABSENT);
        long total = present + absent;
        long percent = total == 0 ? 0 : Math.round(present * 100.0 / total);

        List<Attendance> recentAttendance = attendanceRepository.findWithSubjectByStudent(id)
                .stream()
                .sorted(Comparator.comparing(Attendance::getAttendanceDate).reversed())
                .limit(6)
                .toList();

        model.addAttribute("student", student);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("presentCount", present);
        model.addAttribute("absentCount", absent);
        model.addAttribute("attendancePercent", percent);
        model.addAttribute("recentAttendance", recentAttendance);

        ClassEntity myClass = academicService.getStudentClass(id).orElse(null);
        model.addAttribute("myClass", myClass);
        if (myClass != null) {
            model.addAttribute("todayLectures", academicService.getClassLecturesToday(myClass.getId()));
            model.addAttribute("timetable", academicService.getClassLectures(myClass.getId()));
        }

        model.addAttribute("upcomingExams", examService.getStudentExams(id).stream()
                .filter(x -> "AVAILABLE".equals(x.getState()) || "IN_PROGRESS".equals(x.getState()))
                .limit(3).toList());
        model.addAttribute("recentResults", examService.getStudentResults(id).stream().limit(3).toList());

        model.addAttribute("fee", feeService.getSummary(id));
        model.addAttribute("payments", feeService.getPayments(id).stream().limit(5).toList());
        model.addAttribute("notifications",
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(id)
                        .stream().limit(5).toList());

        return "student/dashboard";
    }
}
