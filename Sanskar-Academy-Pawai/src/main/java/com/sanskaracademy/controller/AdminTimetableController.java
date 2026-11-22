package com.sanskaracademy.controller;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.SubjectRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AcademicService;
import com.sanskaracademy.service.TimetableService;
import com.sanskaracademy.service.TimetableService.Filter;
import com.sanskaracademy.service.TimetableService.Row;

/** Admin: month-wise timetable (plan, generate, edit, cancel, mark taken) and the "who took what" report. */
@Controller
@RequestMapping("/admin/timetable")
public class AdminTimetableController {

    private final TimetableService service;
    private final AcademicService academic;
    private final UserRepository users;
    private final SubjectRepository subjects;

    public AdminTimetableController(TimetableService service, AcademicService academic,
                                    UserRepository users, SubjectRepository subjects) {
        this.service = service;
        this.academic = academic;
        this.users = users;
        this.subjects = subjects;
    }

    private void common(Model model, Filter f) {
        model.addAttribute("f", f);
        model.addAttribute("classes", academic.getAllClasses());
        model.addAttribute("teachers", users.findAllTeachers());
    }

    private String back(String page, Integer year, Integer month, Integer week, Long classId, Long teacherId) {
        StringBuilder sb = new StringBuilder("redirect:/admin/timetable").append(page).append("?");
        if (year != null) sb.append("year=").append(year).append('&');
        if (month != null) sb.append("month=").append(month).append('&');
        if (week != null && week > 0) sb.append("week=").append(week).append('&');
        if (classId != null) sb.append("classId=").append(classId).append('&');
        if (teacherId != null) sb.append("teacherId=").append(teacherId).append('&');
        return sb.toString();
    }

    @GetMapping
    public String plan(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                       @RequestParam(required = false) Integer week, @RequestParam(required = false) Long classId,
                       @RequestParam(required = false) Long teacherId, Model model) {
        Filter f = service.filter(year, month, week, classId, teacherId);
        List<Row> rows = service.rows(f.getFrom(), f.getTo(), classId, teacherId);
        common(model, f);
        model.addAttribute("rows", rows);
        model.addAttribute("counts", service.count(rows));
        model.addAttribute("subjects", academic.getAllSubjects());
        model.addAttribute("today", LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")));
        return "admin/timetable";
    }

    @GetMapping("/report")
    public String report(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                         @RequestParam(required = false) Integer week, @RequestParam(required = false) Long classId,
                         @RequestParam(required = false) Long teacherId, Model model) {
        Filter f = service.filter(year, month, week, classId, teacherId);
        List<Row> rows = service.rows(f.getFrom(), f.getTo(), classId, teacherId);
        common(model, f);
        model.addAttribute("lines", service.report(f.getFrom(), f.getTo(), classId, teacherId));
        model.addAttribute("rows", rows);
        model.addAttribute("counts", service.count(rows));
        return "admin/timetable-report";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                                         @RequestParam(required = false) Integer week, @RequestParam(required = false) Long classId,
                                         @RequestParam(required = false) Long teacherId) {
        Filter f = service.filter(year, month, week, classId, teacherId);
        DateTimeFormatter date = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("Date,Day,Time,Class,Subject,Planned teacher,Took / will take,Status,Topic,Note\n");
        for (Row r : service.rows(f.getFrom(), f.getTo(), classId, teacherId)) {
            sb.append(csv(r.getSession().getLectureDate().format(date))).append(',')
              .append(csv(r.getSession().getLectureDate().getDayOfWeek().toString())).append(',')
              .append(csv(r.getTimeText())).append(',')
              .append(csv(r.getClassName())).append(',')
              .append(csv(r.getSubjectName())).append(',')
              .append(csv(r.getPlannedTeacherName())).append(',')
              .append(csv(r.getTeacherName())).append(',')
              .append(csv(r.getStateLabel())).append(',')
              .append(csv(r.getSession().getTopic())).append(',')
              .append(csv(r.getSession().getNote())).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"timetable-" + f.getYear() + "-"
                        + String.format("%02d", f.getMonth()) + (f.getWeek() > 0 ? "-week" + f.getWeek() : "") + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String csv(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        // keep spreadsheet programs from running text that starts like a formula
        if (!t.isEmpty() && "=+-@".indexOf(t.charAt(0)) >= 0) t = "'" + t;
        return "\"" + t + "\"";
    }

    // ------------------------------------------------------------------ actions
    @PostMapping("/generate")
    public String generate(@RequestParam int year, @RequestParam int month, @RequestParam(required = false) Integer week,
                           @RequestParam(required = false) Long classId, @RequestParam(required = false) Long genClassId,
                           @RequestParam(required = false) Long teacherId,
                           @RequestParam(defaultValue = "false") boolean includeHolidays, RedirectAttributes ra) {
        try {
            Filter f = service.filter(year, month, week, classId, teacherId);
            TimetableService.GenResult r = service.generate(f, genClassId, includeHolidays);
            ra.addFlashAttribute("success", r.getCreated() + " lectures created for " + f.getTitle()
                    + (r.getExisting() > 0 ? " (" + r.getExisting() + " already existed)" : "")
                    + (r.getHolidaySkipped() > 0 ? " (" + r.getHolidaySkipped() + " skipped because of holidays)" : "") + ".");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return back("", year, month, week, classId, teacherId);
    }

    @PostMapping("/add")
    public String add(@RequestParam Long subjectId, @RequestParam LocalDate date, @RequestParam LocalTime start,
                      @RequestParam LocalTime end, @RequestParam(required = false) String room,
                      @RequestParam(required = false) Long substituteId, @RequestParam(required = false) String note,
                      @RequestParam(defaultValue = "false") boolean notify,
                      @RequestParam int year, @RequestParam int month, @RequestParam(required = false) Integer week,
                      @RequestParam(required = false) Long classId, @RequestParam(required = false) Long teacherId,
                      RedirectAttributes ra) {
        try {
            service.add(subjectId, date, start, end, room, substituteId, note, notify);
            ra.addFlashAttribute("success", "Lecture added on " + date);
            return back("", date.getYear(), date.getMonthValue(), null, classId, teacherId);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return back("", year, month, week, classId, teacherId);
        }
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @RequestParam Long subjectId, @RequestParam LocalDate date,
                       @RequestParam LocalTime start, @RequestParam LocalTime end, @RequestParam(required = false) String room,
                       @RequestParam(required = false) Long substituteId, @RequestParam(required = false) String note,
                       @RequestParam(defaultValue = "false") boolean notify,
                       @RequestParam int year, @RequestParam int month, @RequestParam(required = false) Integer week,
                       @RequestParam(required = false) Long classId, @RequestParam(required = false) Long teacherId,
                       RedirectAttributes ra) {
        try {
            service.update(id, subjectId, date, start, end, room, substituteId, note, notify);
            ra.addFlashAttribute("success", "Lecture updated");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return back("", year, month, week, classId, teacherId);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam(required = false) String reason,
                         @RequestParam(defaultValue = "false") boolean notify,
                         @RequestParam int year, @RequestParam int month, @RequestParam(required = false) Integer week,
                         @RequestParam(required = false) Long classId, @RequestParam(required = false) Long teacherId,
                         RedirectAttributes ra) {
        try {
            service.cancel(id, reason, notify);
            ra.addFlashAttribute("success", "Lecture cancelled");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return back("", year, month, week, classId, teacherId);
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, @RequestParam int year, @RequestParam int month,
                          @RequestParam(required = false) Integer week, @RequestParam(required = false) Long classId,
                          @RequestParam(required = false) Long teacherId, RedirectAttributes ra) {
        try {
            service.restore(id);
            ra.addFlashAttribute("success", "Lecture restored");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return back("", year, month, week, classId, teacherId);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @RequestParam int year, @RequestParam int month,
                         @RequestParam(required = false) Integer week, @RequestParam(required = false) Long classId,
                         @RequestParam(required = false) Long teacherId, RedirectAttributes ra) {
        try {
            service.delete(id);
            ra.addFlashAttribute("success", "Lecture deleted");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return back("", year, month, week, classId, teacherId);
    }

    @PostMapping("/{id}/taken")
    public String taken(@PathVariable Long id, @RequestParam(required = false) String topic,
                        @RequestParam(required = false) String note, @RequestParam(defaultValue = "false") boolean undo,
                        @RequestParam int year, @RequestParam int month, @RequestParam(required = false) Integer week,
                        @RequestParam(required = false) Long classId, @RequestParam(required = false) Long teacherId,
                        Principal principal, RedirectAttributes ra) {
        try {
            User me = users.findByUsername(principal.getName()).orElseThrow();
            if (undo) service.unmarkTaken(id, me, true);
            else service.markTaken(id, me, true, topic, note);
            ra.addFlashAttribute("success", undo ? "Taken mark removed" : "Marked as taken");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return back("", year, month, week, classId, teacherId);
    }
}
