package com.sanskaracademy.controller;

import java.math.BigDecimal;
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

import com.sanskaracademy.dto.FeeSummaryDto;
import com.sanskaracademy.entity.PaymentMode;
import com.sanskaracademy.entity.StudentFee;
import com.sanskaracademy.service.FeeService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/fees")
@RequiredArgsConstructor
public class AdminFeeController {

    private final FeeService feeService;

    // ---------- main page ----------
    @GetMapping
    public String feeList(Model model) {

        List<FeeSummaryDto> summaries = feeService.getAllFeeSummaries();

        BigDecimal totalPayable = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;
        long studentsWithDues = 0;

        for (FeeSummaryDto s : summaries) {
            totalPayable = totalPayable.add(s.getPayable());
            totalPaid = totalPaid.add(s.getPaid());
            totalPending = totalPending.add(s.getPending());
            if (s.getPending().signum() > 0) studentsWithDues++;
        }

        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        String defaultYear = startYear + "-" + String.valueOf(startYear + 1).substring(2);

        model.addAttribute("summaries", summaries);
        model.addAttribute("totalPayable", totalPayable);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("studentsWithDues", studentsWithDues);
        model.addAttribute("paymentModes", PaymentMode.values());
        model.addAttribute("today", today);
        model.addAttribute("defaultYear", defaultYear);
        return "admin/fees";
    }

    // ---------- set / update fee of a student ----------
    @PostMapping("/plan")
    public String savePlan(@RequestParam Long studentId,
                           @RequestParam BigDecimal totalFee,
                           @RequestParam(required = false) BigDecimal discount,
                           @RequestParam(required = false) String academicYear,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                           RedirectAttributes ra) {
        try {
            feeService.setFeePlan(studentId, totalFee, discount, academicYear, dueDate);
            ra.addFlashAttribute("success", "Fee saved successfully");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/fees";
    }

    // ---------- collect payment ----------
    @PostMapping("/pay")
    public String pay(@RequestParam Long studentId,
                      @RequestParam BigDecimal amount,
                      @RequestParam PaymentMode paymentMode,
                      @RequestParam(required = false)
                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
                      @RequestParam(required = false) String remarks,
                      RedirectAttributes ra) {
        try {
            StudentFee fee = feeService.collectPayment(studentId, amount, paymentMode, paymentDate, remarks);
            return "redirect:/admin/fees/receipt/" + fee.getId();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/fees";
        }
    }

    // ---------- payment history of one student ----------
    @GetMapping("/student/{id}")
    public String history(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("summary", feeService.getSummary(id));
            model.addAttribute("payments", feeService.getPayments(id));
            return "admin/fee-history";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/fees";
        }
    }

    // ---------- printable receipt ----------
    @GetMapping("/receipt/{id}")
    public String receipt(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            StudentFee payment = feeService.getPayment(id);
            model.addAttribute("payment", payment);
            model.addAttribute("summary", feeService.getSummary(payment.getStudent().getId()));
            return "admin/fee-receipt";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/fees";
        }
    }
}
