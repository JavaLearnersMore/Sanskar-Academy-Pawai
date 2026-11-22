package com.sanskaracademy.controller;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
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

import com.sanskaracademy.service.StaffHrService;

/** Admin side of the staff module: staff, attendance, holidays, leave, payroll. */
@Controller
@RequestMapping("/admin/staff")
public class AdminStaffController {

    private final StaffHrService hr;

    public AdminStaffController(StaffHrService hr) {
        this.hr = hr;
    }

    // ---------- staff list + settings ----------
    @GetMapping
    public String list(Model model) {
        model.addAttribute("staffList", hr.allStaff());
        model.addAttribute("availableUsers", hr.usersAvailableForStaff());
        model.addAttribute("setting", hr.getSetting());
        model.addAttribute("days", DayOfWeek.values());
        return "admin/staff";
    }

    @PostMapping("/add")
    public String add(@RequestParam(required = false) Long existingUserId,
                      @RequestParam(required = false) String fullName,
                      @RequestParam(required = false) String username,
                      @RequestParam(required = false) String password,
                      @RequestParam(required = false) String email,
                      @RequestParam(required = false) String mobile,
                      @RequestParam String staffType,
                      @RequestParam(required = false) String designation,
                      @RequestParam BigDecimal salary,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDate,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
                      @RequestParam(required = false) List<String> offDays,
                      RedirectAttributes ra) {
        try {
            hr.addStaff(existingUserId, fullName, username, password, email, mobile, staffType, designation,
                    salary, joinDate, birthDate, offDays);
            ra.addFlashAttribute("success", "Staff member added");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @RequestParam String staffType,
                         @RequestParam(required = false) String designation,
                         @RequestParam BigDecimal salary,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDate,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
                         @RequestParam(required = false) List<String> offDays,
                         @RequestParam(defaultValue = "false") boolean active,
                         RedirectAttributes ra) {
        try {
            hr.updateStaff(id, staffType, designation, salary, joinDate, birthDate, offDays, active);
            ra.addFlashAttribute("success", "Staff details saved");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/settings")
    public String settings(@RequestParam BigDecimal paidLeave, @RequestParam int divisor,
                           @RequestParam(required = false) List<String> offDays, RedirectAttributes ra) {
        try {
            hr.saveSetting(paidLeave, divisor, offDays);
            ra.addFlashAttribute("success", "Salary rules saved");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/staff";
    }

    // ---------- attendance ----------
    @GetMapping("/attendance")
    public String attendance(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             Model model) {
        LocalDate d = date == null ? LocalDate.now() : date;
        model.addAttribute("date", d);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("rows", hr.sheetFor(d));
        return "admin/staff-attendance";
    }

    @PostMapping("/attendance")
    public String saveAttendance(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam Map<String, String> all, RedirectAttributes ra) {
        Map<Long, String> statuses = new HashMap<>();
        Map<Long, String> remarks = new HashMap<>();
        for (Map.Entry<String, String> e : all.entrySet()) {
            try {
                if (e.getKey().startsWith("status_")) statuses.put(Long.valueOf(e.getKey().substring(7)), e.getValue());
                else if (e.getKey().startsWith("remark_")) remarks.put(Long.valueOf(e.getKey().substring(7)), e.getValue());
            } catch (NumberFormatException ignore) {
                // ignore foreign parameters
            }
        }
        try {
            int n = hr.saveSheet(date, statuses, remarks);
            ra.addFlashAttribute("success", "Attendance saved for " + n + " staff");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/staff/attendance?date=" + date;
    }

    // ---------- holidays ----------
    @GetMapping("/holidays")
    public String holidays(Model model) {
        model.addAttribute("holidays", hr.allHolidays());
        return "admin/staff-holidays";
    }

    @PostMapping("/holidays")
    public String addHoliday(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam String name,
                             @RequestParam(defaultValue = "false") boolean wishes, RedirectAttributes ra) {
        try {
            hr.addHoliday(date, name, wishes);
            ra.addFlashAttribute("success", "Holiday added");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/staff/holidays";
    }

    @PostMapping("/holidays/{id}/delete")
    public String deleteHoliday(@PathVariable Long id, RedirectAttributes ra) {
        hr.deleteHoliday(id);
        ra.addFlashAttribute("success", "Holiday removed");
        return "redirect:/admin/staff/holidays";
    }

    // ---------- leave ----------
    @GetMapping("/leaves")
    public String leaves(Model model) {
        model.addAttribute("leaves", hr.allLeaves());
        return "admin/staff-leaves";
    }

    @PostMapping("/leaves/{id}/decide")
    public String decide(@PathVariable Long id, @RequestParam String action,
                         @RequestParam(required = false) String note, RedirectAttributes ra) {
        try {
            hr.decideLeave(id, "APPROVE".equals(action), note);
            ra.addFlashAttribute("success", "Leave " + ("APPROVE".equals(action) ? "approved" : "rejected"));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/staff/leaves";
    }

    // ---------- payroll ----------
    @GetMapping("/payroll")
    public String payroll(@RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year,
                          Model model) {
        LocalDate now = LocalDate.now();
        int m = month == null ? now.getMonthValue() : month;
        int y = year == null ? now.getYear() : year;
        model.addAttribute("month", m);
        model.addAttribute("year", y);
        model.addAttribute("rows", hr.payrollOf(m, y));
        model.addAttribute("setting", hr.getSetting());
        return "admin/staff-payroll";
    }

    @PostMapping("/payroll/generate")
    public String generate(@RequestParam int month, @RequestParam int year, RedirectAttributes ra) {
        try {
            int n = hr.generatePayroll(month, year);
            ra.addFlashAttribute("success", "Salary calculated for " + n + " staff (paid ones are not changed)");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/staff/payroll?month=" + month + "&year=" + year;
    }

    @PostMapping("/payroll/{id}/adjust")
    public String adjust(@PathVariable Long id, @RequestParam BigDecimal other, @RequestParam BigDecimal bonus,
                         @RequestParam(required = false) String remarks,
                         @RequestParam int month, @RequestParam int year, RedirectAttributes ra) {
        try {
            hr.adjustPayroll(id, other, bonus, remarks);
            ra.addFlashAttribute("success", "Saved");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/staff/payroll?month=" + month + "&year=" + year;
    }

    @PostMapping("/payroll/{id}/pay")
    public String pay(@PathVariable Long id, @RequestParam int month, @RequestParam int year, RedirectAttributes ra) {
        try {
            hr.markPaid(id);
            ra.addFlashAttribute("success", "Marked as paid; the staff member was informed");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/staff/payroll?month=" + month + "&year=" + year;
    }

    @GetMapping("/payroll/{id}/slip")
    public String slip(@PathVariable Long id, Model model) {
        model.addAttribute("p", hr.getPayroll(id));
        model.addAttribute("back", "/admin/staff/payroll");
        return "staff/slip";
    }
}
