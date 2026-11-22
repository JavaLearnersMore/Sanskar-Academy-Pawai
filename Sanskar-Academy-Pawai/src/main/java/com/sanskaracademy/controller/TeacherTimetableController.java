package com.sanskaracademy.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.TimetableService;
import com.sanskaracademy.service.TimetableService.Filter;
import com.sanskaracademy.service.TimetableService.Row;

/** Teacher: my lectures of a month / week, with "mark as taken". */
@Controller
@RequestMapping("/teacher/timetable")
public class TeacherTimetableController {

    private final TimetableService service;
    private final UserRepository users;

    public TeacherTimetableController(TimetableService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    @GetMapping
    public String page(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                       @RequestParam(required = false) Integer week, Principal principal, Model model) {
        User me = users.findByUsername(principal.getName()).orElseThrow();
        Filter f = service.filter(year, month, week, null, me.getId());
        List<Row> rows = service.rows(f.getFrom(), f.getTo(), null, me.getId());
        model.addAttribute("f", f);
        model.addAttribute("rows", rows);
        model.addAttribute("counts", service.count(rows));
        model.addAttribute("meId", me.getId());
        return "teacher/timetable";
    }

    @PostMapping("/{id}/taken")
    public String taken(@PathVariable Long id, @RequestParam(required = false) String topic,
                        @RequestParam(required = false) String note, @RequestParam(defaultValue = "false") boolean undo,
                        @RequestParam int year, @RequestParam int month, @RequestParam(required = false) Integer week,
                        Principal principal, RedirectAttributes ra) {
        try {
            User me = users.findByUsername(principal.getName()).orElseThrow();
            if (undo) service.unmarkTaken(id, me, false);
            else service.markTaken(id, me, false, topic, note);
            ra.addFlashAttribute("success", undo ? "Taken mark removed" : "Lecture marked as taken");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        StringBuilder sb = new StringBuilder("redirect:/teacher/timetable?year=" + year + "&month=" + month);
        if (week != null && week > 0) sb.append("&week=").append(week);
        return sb.toString();
    }
}
