package com.sanskaracademy.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AcademicService;
import com.sanskaracademy.service.FeeService;

import lombok.RequiredArgsConstructor;

/**
 * Parent pays the fee of his/her child.
 * NOTE: this records the payment directly (demo, no real gateway). For Razorpay/PayU etc.
 * call collectPayment() from the gateway's success callback instead.
 */
@Controller
@RequestMapping("/parent/fees")
@RequiredArgsConstructor
public class ParentFeeController {

    private final FeeService feeService;
    private final AcademicService academicService;
    private final UserRepository userRepository;

    @GetMapping
    public String page(Principal principal, Model model) {

        User parent = userRepository.findByUsername(principal.getName()).orElseThrow();

        List<FeeSummaryDto> fees = new ArrayList<>();
        Map<Long, List<StudentFee>> payments = new HashMap<>();
        for (User child : academicService.getChildren(parent.getId())) {
            fees.add(feeService.getSummary(child.getId()));
            payments.put(child.getId(), feeService.getPayments(child.getId()));
        }

        model.addAttribute("fees", fees);
        model.addAttribute("payments", payments);
        model.addAttribute("onlineModes", new PaymentMode[] {
                PaymentMode.UPI, PaymentMode.CARD, PaymentMode.BANK_TRANSFER });
        return "parent/fees";
    }

    @PostMapping("/pay")
    public String pay(@RequestParam Long studentId,
                      @RequestParam BigDecimal amount,
                      @RequestParam PaymentMode paymentMode,
                      Principal principal, RedirectAttributes ra) {
        try {
            User parent = userRepository.findByUsername(principal.getName()).orElseThrow();

            if (!academicService.isParentOf(parent.getId(), studentId)) {
                throw new IllegalArgumentException("This student is not linked to your account");
            }
            if (paymentMode == PaymentMode.CASH) {
                throw new IllegalArgumentException("Choose an online payment mode");
            }

            StudentFee fee = feeService.collectPayment(studentId, amount, paymentMode, LocalDate.now(),
                    "Paid online by parent " + parent.getFullName());
            return "redirect:/parent/fees/receipt/" + fee.getId();

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/parent/fees";
        }
    }

    @GetMapping("/receipt/{id}")
    public String receipt(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        try {
            User parent = userRepository.findByUsername(principal.getName()).orElseThrow();
            StudentFee payment = feeService.getPayment(id);

            if (!academicService.isParentOf(parent.getId(), payment.getStudent().getId())) {
                throw new IllegalArgumentException("You cannot view this receipt");
            }
            model.addAttribute("payment", payment);
            model.addAttribute("summary", feeService.getSummary(payment.getStudent().getId()));
            return "parent/fee-receipt";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/parent/fees";
        }
    }
}
