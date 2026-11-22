package com.sanskaracademy.controller;

import java.security.Principal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.EnquiryStatus;
import com.sanskaracademy.service.AuditService;
import com.sanskaracademy.service.EnquiryService;
import com.sanskaracademy.service.OwnerDashboardService;
import com.sanskaracademy.service.SchoolAccess;

/** Owner dashboard, admission enquiries and the audit log (all /admin/**, so only the admin can open them). */
@Controller
public class AdminOfficeController {

    private final OwnerDashboardService dashboard;
    private final EnquiryService enquiries;
    private final AuditService audit;
    private final SchoolAccess access;

    public AdminOfficeController(OwnerDashboardService dashboard, EnquiryService enquiries, AuditService audit, SchoolAccess access) {
        this.dashboard = dashboard;
        this.enquiries = enquiries;
        this.audit = audit;
        this.access = access;
    }

    @GetMapping("/admin/owner")
    public String owner(Model m) {
        m.addAttribute("d", dashboard.build());
        return "admin/owner-dashboard";
    }

    // ------------------------------------------------------------------ enquiries
    @GetMapping("/admin/enquiries")
    public String enquiryList(@RequestParam(required = false) String status, Model m) {
        EnquiryStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = EnquiryStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {
                // show all
            }
        }
        m.addAttribute("list", enquiries.list(st));
        m.addAttribute("due", enquiries.due());
        m.addAttribute("statuses", EnquiryStatus.values());
        m.addAttribute("status", st == null ? "" : st.name());
        m.addAttribute("today", LocalDate.now());
        return "admin/enquiries";
    }

    @PostMapping("/admin/enquiries")
    public String enquiryAdd(@RequestParam String studentName, @RequestParam(required = false) String parentName,
                             @RequestParam String phone, @RequestParam(required = false) String email,
                             @RequestParam(required = false) String interestedClass, @RequestParam(required = false) String source,
                             @RequestParam(required = false) String message, Principal p, RedirectAttributes ra) {
        try {
            enquiries.add(access.me(p), studentName, parentName, phone, email, interestedClass, source, message);
            ra.addFlashAttribute("success", "Enquiry saved. First follow-up is set for tomorrow.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/enquiries";
    }

    @PostMapping("/admin/enquiries/{id}/update")
    public String enquiryUpdate(@PathVariable Long id, @RequestParam EnquiryStatus status,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextFollowUp,
                                @RequestParam(required = false) String note, Principal p, RedirectAttributes ra) {
        try {
            enquiries.update(id, access.me(p), status, nextFollowUp, note);
            ra.addFlashAttribute("success", "Saved.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/enquiries";
    }

    @PostMapping("/admin/enquiries/{id}/delete")
    public String enquiryDelete(@PathVariable Long id, RedirectAttributes ra) {
        enquiries.delete(id);
        ra.addFlashAttribute("success", "Deleted.");
        return "redirect:/admin/enquiries";
    }

    // ------------------------------------------------------------------ audit log
    @GetMapping("/admin/audit-log")
    public String auditLog(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           @RequestParam(required = false) String user, @RequestParam(required = false) String method,
                           @RequestParam(defaultValue = "300") int limit, Model m) {
        LocalDate f = from == null ? LocalDate.now().minusDays(7) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        m.addAttribute("list", audit.search(f, t, user, method, limit));
        m.addAttribute("from", f);
        m.addAttribute("to", t);
        m.addAttribute("user", user == null ? "" : user);
        m.addAttribute("method", method == null ? "" : method);
        m.addAttribute("limit", limit);
        m.addAttribute("retention", audit.getRetentionDays());
        return "admin/audit-log";
    }
}
