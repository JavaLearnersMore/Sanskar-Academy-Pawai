package com.sanskaracademy.controller;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.ExamGuardService;

/** Small JSON calls made by the exam page while the student is writing. */
@Controller
@RequestMapping("/student/exams/attempt/{id}")
public class StudentExamGuardController {

    private final ExamGuardService guard;
    private final UserRepository users;

    public StudentExamGuardController(ExamGuardService guard, UserRepository users) {
        this.guard = guard;
        this.users = users;
    }

    @PostMapping("/event")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> event(@PathVariable Long id, @RequestParam String type,
                                                     @RequestParam(required = false) String detail, Principal principal) {
        try {
            User u = users.findByUsername(principal.getName()).orElseThrow();
            ExamGuardService.Outcome o = guard.record(id, u.getId(), type, detail);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("count", o.getCount());
            m.put("max", o.getMax());
            m.put("autoSubmit", o.isAutoSubmit());
            return ResponseEntity.ok(m);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/next")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> next(@PathVariable Long id, Principal principal) {
        try {
            User u = users.findByUsername(principal.getName()).orElseThrow();
            ExamGuardService.QState s = guard.advance(id, u.getId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", s.getIndex());
            m.put("remaining", s.getRemaining());
            m.put("finished", s.isFinished());
            return ResponseEntity.ok(m);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
