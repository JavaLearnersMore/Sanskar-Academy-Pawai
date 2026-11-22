package com.sanskaracademy.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sanskaracademy.dto.ChildSummaryDto;
import com.sanskaracademy.entity.AttendanceStatus;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.AttendanceRepository;
import com.sanskaracademy.repository.NotificationRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AcademicService;
import com.sanskaracademy.service.FeeService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/parent")
@RequiredArgsConstructor
public class ParentController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AcademicService academicService;
    private final AttendanceRepository attendanceRepository;
    private final FeeService feeService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {

        User parent = userRepository.findByUsername(principal.getName()).orElseThrow();

        List<ChildSummaryDto> children = new ArrayList<>();
        for (User child : academicService.getChildren(parent.getId())) {

            long present = attendanceRepository.countAttendanceByStatus(child.getId(), AttendanceStatus.PRESENT);
            long absent = attendanceRepository.countAttendanceByStatus(child.getId(), AttendanceStatus.ABSENT);
            long total = present + absent;

            children.add(ChildSummaryDto.builder()
                    .studentId(child.getId())
                    .studentName(child.getFullName())
                    .className(academicService.getStudentClass(child.getId())
                            .map(c -> c.getClassName()).orElse("Not assigned"))
                    .fee(feeService.getSummary(child.getId()))
                    .presentCount(present)
                    .absentCount(absent)
                    .attendancePercent(total == 0 ? 0 : Math.round(present * 100.0 / total))
                    .build());
        }

        model.addAttribute("parent", parent);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("children", children);
        model.addAttribute("notifications",
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(parent.getId())
                        .stream().limit(8).toList());

        return "parent/dashboard";
    }
}
