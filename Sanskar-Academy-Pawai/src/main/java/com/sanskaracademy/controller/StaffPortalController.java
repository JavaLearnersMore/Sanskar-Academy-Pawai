package com.sanskaracademy.controller;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.StaffAttendance;
import com.sanskaracademy.entity.StaffPayroll;
import com.sanskaracademy.entity.StaffProfile;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.StaffHrService;

/** Staff portal: my attendance, my leave (apply), my salary slips. Works for STAFF and for teachers added as staff. */
@Controller
@RequestMapping("/staff")
public class StaffPortalController {

    private final StaffHrService hr;
    private final UserRepository users;

    public StaffPortalController(StaffHrService hr, UserRepository users) {
        this.hr = hr;
        this.users = users;
    }

    private StaffProfile me(Principal principal) {
        User u = users.findByUsername(principal.getName()).orElseThrow();
        return hr.staffOf(u);
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal,
                            @RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year,
                            Model model) {
        StaffProfile st = me(principal);
        model.addAttribute("staff", st);
        if (st == null) return "staff/dashboard";
        LocalDate now = LocalDate.now();
        int m = month == null ? now.getMonthValue() : month;
        int y = year == null ? now.getYear() : year;
        List<StaffAttendance> att = hr.monthAttendance(st, m, y);
        int present = 0, absent = 0, half = 0, leave = 0;
        for (StaffAttendance a : att) {
            switch (a.getStatus()) {
                case ABSENT: absent++; break;
                case HALF_DAY: half++; break;
                case LEAVE: leave++; break;
                default: present++;
            }
        }
        model.addAttribute("month", m);
        model.addAttribute("year", y);
        model.addAttribute("att", att);
        model.addAttribute("present", present);
        model.addAttribute("absent", absent);
        model.addAttribute("half", half);
        model.addAttribute("leave", leave);
        model.addAttribute("paidLeave", hr.getSetting().getPaidLeavePerMonth());
        model.addAttribute("offDays", hr.offDaysOf(st));
        model.addAttribute("holidays", hr.upcomingHolidays());
        return "staff/dashboard";
    }

    @GetMapping("/leave")
    public String leave(Principal principal, Model model) {
        StaffProfile st = me(principal);
        model.addAttribute("staff", st);
        model.addAttribute("today", LocalDate.now());
        if (st != null) model.addAttribute("leaves", hr.leavesOf(st));
        return "staff/leave";
    }

    @PostMapping("/leave/apply")
    public String apply(Principal principal,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                        @RequestParam String reason, RedirectAttributes ra) {
        StaffProfile st = me(principal);
        try {
            if (st == null) throw new IllegalArgumentException("You are not in the staff list yet. Ask the admin.");
            hr.applyLeave(st, fromDate, toDate, reason);
            ra.addFlashAttribute("success", "Leave request sent to the admin");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/leave";
    }

    @PostMapping("/leave/{id}/cancel")
    public String cancel(Principal principal, @PathVariable Long id, RedirectAttributes ra) {
        StaffProfile st = me(principal);
        try {
            if (st == null) throw new IllegalArgumentException("Not in the staff list");
            hr.cancelOwnLeave(st, id);
            ra.addFlashAttribute("success", "Request cancelled");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/leave";
    }

    @GetMapping("/salary")
    public String salary(Principal principal, Model model) {
        StaffProfile st = me(principal);
        model.addAttribute("staff", st);
        if (st != null) model.addAttribute("slips", hr.myPayroll(st));
        return "staff/salary";
    }

    @GetMapping("/salary/{id}")
    public String slip(Principal principal, @PathVariable Long id, Model model) {
        StaffProfile st = me(principal);
        StaffPayroll p = hr.getPayroll(id);
        if (st == null || !p.getStaff().getId().equals(st.getId()) || p.getStatus() != com.sanskaracademy.entity.PayrollStatus.PAID)
            throw new IllegalArgumentException("Slip not found");
        model.addAttribute("p", p);
        model.addAttribute("back", "/staff/salary");
        return "staff/slip";
    }
}
