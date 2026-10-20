package com.sanskaracademy.controller;

import com.sanskaracademy.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final AttendanceService attendanceService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {

        // Example usage
        // model.addAttribute("attendance", attendanceService.getStudentAttendance(studentId));

        return "student/dashboard";
    }
}