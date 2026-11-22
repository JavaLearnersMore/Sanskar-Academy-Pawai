package com.sanskaracademy.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sanskaracademy.service.EnquiryService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Public "Admission enquiry" form at /enquiry (no login). It only works for visitors after you add "/enquiry"
 * to the permitAll() list in SecurityConfig (see the README). Until then visitors are sent to the login page.
 * Protection: hidden trap field, at most 5 submissions per hour per address, and the normal CSRF token.
 */
@Controller
public class PublicEnquiryController {

    private static final int MAX_PER_HOUR = 5;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final EnquiryService enquiries;

    public PublicEnquiryController(EnquiryService enquiries) {
        this.enquiries = enquiries;
    }

    @GetMapping("/enquiry")
    public String form() {
        return "public/enquiry";
    }

    @PostMapping("/enquiry")
    public String submit(@RequestParam String studentName, @RequestParam(required = false) String parentName,
                         @RequestParam String phone, @RequestParam(required = false) String email,
                         @RequestParam(required = false) String interestedClass, @RequestParam(required = false) String message,
                         @RequestParam(required = false) String website, HttpServletRequest req, Model m) {
        if (website != null && !website.isBlank()) {
            m.addAttribute("done", true);   // a robot filled the hidden field: pretend it worked, save nothing
            return "public/enquiry";
        }
        String ip = req.getRemoteAddr();
        Deque<Instant> q = hits.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (q) {
            Instant cutoff = Instant.now().minus(Duration.ofHours(1));
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
            if (q.size() >= MAX_PER_HOUR) {
                m.addAttribute("error", "Too many requests. Please try again after some time or call the office.");
                return "public/enquiry";
            }
            q.addLast(Instant.now());
        }
        if (hits.size() > 5000) hits.clear();
        try {
            enquiries.add(null, studentName, parentName, phone, email, interestedClass, null, message);
            m.addAttribute("done", true);
        } catch (IllegalArgumentException e) {
            m.addAttribute("error", e.getMessage());
        }
        return "public/enquiry";
    }
}
