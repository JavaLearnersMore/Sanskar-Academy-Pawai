package com.sanskaracademy.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.service.MeetingService;
import com.sanskaracademy.service.SchoolAccess;

@Controller
public class MeetingController {

    private final MeetingService meetings;
    private final SchoolAccess access;

    public MeetingController(MeetingService meetings, SchoolAccess access) {
        this.meetings = meetings;
        this.access = access;
    }

    private static String go(RedirectAttributes ra, String url, String ok, RuntimeException e) {
        if (e != null) ra.addFlashAttribute("error", e.getMessage());
        else ra.addFlashAttribute("success", ok);
        return "redirect:" + url;
    }

    // ------------------------------------------------------------------ teacher
    @GetMapping("/teacher/meetings")
    public String teacherPage(Model m, Principal p) {
        m.addAttribute("list", meetings.ofTeacher(access.me(p).getId()));
        m.addAttribute("now", java.time.LocalDateTime.now());
        return "meeting/teacher";
    }

    @PostMapping("/teacher/meetings")
    public String addSlots(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
                           @RequestParam(defaultValue = "4") int count, @RequestParam(defaultValue = "15") int minutes,
                           @RequestParam(required = false) String mode, Principal p, RedirectAttributes ra) {
        try {
            int n = meetings.addSlots(access.me(p), date, start, count, minutes, mode);
            return go(ra, "/teacher/meetings", n + " slot(s) opened. Parents can book them now.", null);
        } catch (RuntimeException e) {
            return go(ra, "/teacher/meetings", null, e);
        }
    }

    @PostMapping("/teacher/meetings/{id}/confirm")
    public String confirm(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        try {
            meetings.confirm(id, access.me(p).getId());
            return go(ra, "/teacher/meetings", "Meeting confirmed. The parent was told.", null);
        } catch (RuntimeException e) {
            return go(ra, "/teacher/meetings", null, e);
        }
    }

    @PostMapping("/teacher/meetings/{id}/done")
    public String done(@PathVariable Long id, @RequestParam(required = false) String note, Principal p, RedirectAttributes ra) {
        try {
            meetings.done(id, access.me(p).getId(), note);
            return go(ra, "/teacher/meetings", "Marked as done.", null);
        } catch (RuntimeException e) {
            return go(ra, "/teacher/meetings", null, e);
        }
    }

    @PostMapping("/teacher/meetings/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam(required = false) String reason, Principal p, RedirectAttributes ra) {
        try {
            meetings.teacherCancel(id, access.me(p).getId(), reason);
            return go(ra, "/teacher/meetings", "Done.", null);
        } catch (RuntimeException e) {
            return go(ra, "/teacher/meetings", null, e);
        }
    }

    // ------------------------------------------------------------------ parent
    @GetMapping("/parent/meetings")
    public String parentPage(Model m, Principal p) {
        User me = access.me(p);
        m.addAttribute("open", meetings.openFor(me));
        m.addAttribute("mine", meetings.ofParent(me.getId()));
        m.addAttribute("children", access.childrenOf(me.getId()));
        m.addAttribute("now", java.time.LocalDateTime.now());
        return "meeting/parent";
    }

    @PostMapping("/parent/meetings/{id}/book")
    public String book(@PathVariable Long id, @RequestParam Long studentId, @RequestParam(required = false) String purpose,
                       Principal p, RedirectAttributes ra) {
        try {
            meetings.book(access.me(p), id, studentId, purpose);
            return go(ra, "/parent/meetings", "Booked. The teacher will confirm.", null);
        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            return go(ra, "/parent/meetings", null, new IllegalArgumentException("Sorry, this slot was just taken. Please choose another."));
        } catch (RuntimeException e) {
            return go(ra, "/parent/meetings", null, e);
        }
    }

    @PostMapping("/parent/meetings/{id}/cancel")
    public String parentCancel(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        try {
            meetings.parentCancel(access.me(p), id);
            return go(ra, "/parent/meetings", "Booking cancelled.", null);
        } catch (RuntimeException e) {
            return go(ra, "/parent/meetings", null, e);
        }
    }
}
