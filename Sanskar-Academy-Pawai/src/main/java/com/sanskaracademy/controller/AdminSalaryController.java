package com.sanskaracademy.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.PaymentMode;
import com.sanskaracademy.entity.TeacherSalary;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.SalaryService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/salaries")
@RequiredArgsConstructor
public class AdminSalaryController {

    private final SalaryService salaryService;
    private final UserRepository userRepository;

    @GetMapping
    public String page(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("teachers", userRepository.findAllTeachers());
        model.addAttribute("salaries", salaryService.getAll());
        model.addAttribute("lastBase", salaryService.getLastBaseAmounts());
        model.addAttribute("paidThisMonth",
                salaryService.getPaidTeacherIds(today.getMonthValue(), today.getYear()));
        model.addAttribute("month", today.getMonthValue());
        model.addAttribute("year", today.getYear());
        model.addAttribute("today", today);
        model.addAttribute("paymentModes", PaymentMode.values());
        return "admin/salaries";
    }

    @PostMapping("/pay")
    public String pay(@RequestParam Long teacherId,
                      @RequestParam int month,
                      @RequestParam int year,
                      @RequestParam BigDecimal baseAmount,
                      @RequestParam(required = false) BigDecimal bonus,
                      @RequestParam(required = false) BigDecimal deduction,
                      @RequestParam PaymentMode paymentMode,
                      @RequestParam(required = false)
                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
                      @RequestParam(required = false) String remarks,
                      Principal principal,
                      RedirectAttributes ra) {
        try {
            User admin = userRepository.findByUsername(principal.getName()).orElseThrow();
            TeacherSalary s = salaryService.paySalary(teacherId, month, year, baseAmount, bonus,
                    deduction, paymentMode, paymentDate, remarks, admin);
            return "redirect:/admin/salaries/slip/" + s.getId();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/salaries";
        }
    }

    @GetMapping("/slip/{id}")
    public String slip(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("salary", salaryService.get(id));
            return "admin/salary-slip";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/salaries";
        }
    }
}
