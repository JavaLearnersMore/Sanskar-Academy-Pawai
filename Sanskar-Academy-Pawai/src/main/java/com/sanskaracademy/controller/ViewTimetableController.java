package com.sanskaracademy.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.TimetableService;
import com.sanskaracademy.service.TimetableService.Filter;
import com.sanskaracademy.service.TimetableService.Row;

/** Student and parent: read-only month / week timetable of the class. */
@Controller
public class ViewTimetableController {

    private final TimetableService service;
    private final UserRepository users;

    public ViewTimetableController(TimetableService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    private String render(Long studentId, Integer year, Integer month, Integer week, boolean parent,
                          List<User> children, Model model) {
        Long classId = studentId == null ? null : service.classOfStudent(studentId);
        Filter f = service.filter(year, month, week, classId, null);
        List<Row> rows = classId == null ? new ArrayList<>() : service.rows(f.getFrom(), f.getTo(), classId, null);
        model.addAttribute("f", f);
        model.addAttribute("rows", rows);
        model.addAttribute("noClass", classId == null);
        model.addAttribute("parent", parent);
        model.addAttribute("children", children);
        model.addAttribute("studentId", studentId);
        model.addAttribute("action", parent ? "/parent/timetable" : "/student/timetable");
        return "timetable-view";
    }

    @GetMapping("/student/timetable")
    public String student(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                          @RequestParam(required = false) Integer week, Principal principal, Model model) {
        User me = users.findByUsername(principal.getName()).orElseThrow();
        return render(me.getId(), year, month, week, false, new ArrayList<>(), model);
    }

    @GetMapping("/parent/timetable")
    public String parent(@RequestParam(required = false) Long studentId, @RequestParam(required = false) Integer year,
                         @RequestParam(required = false) Integer month, @RequestParam(required = false) Integer week,
                         Principal principal, Model model) {
        User me = users.findByUsername(principal.getName()).orElseThrow();
        List<User> kids = service.childrenOf(me.getId());
        Long sid = null;
        for (User k : kids) {
            if (studentId != null && k.getId().equals(studentId)) sid = k.getId();   // only own children
        }
        if (sid == null && !kids.isEmpty()) sid = kids.get(0).getId();
        return render(sid, year, month, week, true, kids, model);
    }
}
