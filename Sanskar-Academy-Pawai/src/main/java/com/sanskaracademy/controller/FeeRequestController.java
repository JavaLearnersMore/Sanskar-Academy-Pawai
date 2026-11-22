package com.sanskaracademy.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.dto.FeeSummaryDto;
import com.sanskaracademy.entity.PaymentMode;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.service.FeeRequestService;
import com.sanskaracademy.service.FeeService;
import com.sanskaracademy.service.SchoolAccess;

/** Parent sends a payment reference; admin verifies and approves. */
@Controller
public class FeeRequestController {

    private final FeeRequestService requests;
    private final FeeService fees;
    private final SchoolAccess access;

    public FeeRequestController(FeeRequestService requests, FeeService fees, SchoolAccess access) {
        this.requests = requests;
        this.fees = fees;
        this.access = access;
    }

    @GetMapping("/parent/pay-request")
    public String parentPage(Model m, Principal p) {
        User me = access.me(p);
        List<FeeSummaryDto> sums = new ArrayList<>();
        for (User k : access.childrenOf(me.getId())) sums.add(fees.getSummary(k.getId()));
        m.addAttribute("sums", sums);
        m.addAttribute("list", requests.ofParent(me.getId()));
        m.addAttribute("upiId", requests.getUpiId());
        m.addAttribute("payee", requests.getPayeeName());
        m.addAttribute("today", LocalDate.now());
        return "fees/parent-request";
    }

    @PostMapping("/parent/pay-request")
    public String submit(@RequestParam Long studentId, @RequestParam BigDecimal amount, @RequestParam PaymentMode mode,
                         @RequestParam String reference,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidOn,
                         @RequestParam(required = false) String note, Principal p, RedirectAttributes ra) {
        try {
            requests.submit(access.me(p), studentId, amount, mode, reference, paidOn, note);
            ra.addFlashAttribute("success", "Sent to the office. You will get a receipt after they verify the payment.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/parent/pay-request";
    }

    @GetMapping("/admin/fee-requests")
    public String adminPage(Model m) {
        m.addAttribute("pending", requests.pending());
        m.addAttribute("recent", requests.recent());
        return "fees/admin-requests";
    }

    @PostMapping("/admin/fee-requests/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String note, Principal p, RedirectAttributes ra) {
        try {
            requests.approve(id, access.me(p), note);
            ra.addFlashAttribute("success", "Approved. The payment is recorded and the parent was told.");
        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            ra.addFlashAttribute("error", "Someone else just handled this request. Please refresh.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/fee-requests";
    }

    @PostMapping("/admin/fee-requests/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String note, Principal p, RedirectAttributes ra) {
        try {
            requests.reject(id, access.me(p), note);
            ra.addFlashAttribute("success", "Rejected. The parent was told.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/fee-requests";
    }
}
