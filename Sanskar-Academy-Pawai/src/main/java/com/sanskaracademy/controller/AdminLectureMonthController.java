package com.sanskaracademy.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
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

import com.sanskaracademy.entity.LectureSchedule;
import com.sanskaracademy.service.AcademicService;

/**
 * "Timetable (Lectures)" with a Year and Month selector: the weekly pattern is shown as real dates of the chosen month.
 * Add / Edit / Delete here change the weekly pattern (same as the old tab), so they apply to every such day.
 */
@Controller
@RequestMapping("/admin/lectures")
public class AdminLectureMonthController {

    private static final String[] MONTHS = { "January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December" };

    private final AcademicService academic;

    public AdminLectureMonthController(AcademicService academic) {
        this.academic = academic;
    }

    /** one line of the table: a real date + the weekly slot that falls on it */
    public static class DatedRow {
        private final LocalDate date;
        private final LectureSchedule slot;
        DatedRow(LocalDate d, LectureSchedule s) { date = d; slot = s; }
        public LocalDate getDate() { return date; }
        public LectureSchedule getSlot() { return slot; }
    }

    private static int cleanMonth(Integer m) {
        return m == null || m < 1 || m > 12 ? LocalDate.now().getMonthValue() : m;
    }

    private static int cleanYear(Integer y) {
        int now = LocalDate.now().getYear();
        return y == null || y < now - 5 || y > now + 5 ? now : y;
    }

    @GetMapping
    public String page(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                       @RequestParam(required = false) Long classId, Model m) {
        int y = cleanYear(year), mo = cleanMonth(month);
        YearMonth ym = YearMonth.of(y, mo);
        List<DatedRow> rows = new ArrayList<>();
        List<LectureSchedule> all = academic.getAllLectures();
        for (LectureSchedule l : all) {
            if (classId != null && !classId.equals(l.getSubject().getClassEntity().getId())) continue;
            for (int d = 1; d <= ym.lengthOfMonth(); d++) {
                LocalDate date = ym.atDay(d);
                if (date.getDayOfWeek() == l.getDayOfWeek()) rows.add(new DatedRow(date, l));
            }
        }
        rows.sort(Comparator.comparing(DatedRow::getDate)
                .thenComparing(r -> r.getSlot().getStartTime())
                .thenComparing(r -> r.getSlot().getSubject().getClassEntity().getClassName()));
        List<Integer> years = new ArrayList<>();
        int now = LocalDate.now().getYear();
        for (int i = now - 2; i <= now + 2; i++) years.add(i);
        m.addAttribute("rows", rows);
        m.addAttribute("year", y);
        m.addAttribute("month", mo);
        m.addAttribute("monthName", MONTHS[mo - 1]);
        m.addAttribute("monthNames", MONTHS);
        m.addAttribute("years", years);
        m.addAttribute("classId", classId);
        m.addAttribute("classes", academic.getAllClasses());
        m.addAttribute("subjects", academic.getAllSubjects());
        m.addAttribute("days", DayOfWeek.values());
        return "admin/lectures";
    }

    private static String back(Integer y, Integer mo, Long classId) {
        return "redirect:/admin/lectures?year=" + cleanYear(y) + "&month=" + cleanMonth(mo)
                + (classId == null ? "" : "&classId=" + classId);
    }

    @PostMapping("/add")
    public String add(@RequestParam Long subjectId, @RequestParam DayOfWeek dayOfWeek,
                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
                      @RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                      @RequestParam(required = false) Long classId, RedirectAttributes ra) {
        try {
            academic.addLecture(subjectId, dayOfWeek, startTime, endTime, null);
            ra.addFlashAttribute("success", "Lecture added. It repeats every " + dayOfWeek + ".");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return back(year, month, classId);
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @RequestParam Long subjectId, @RequestParam DayOfWeek dayOfWeek,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
                       @RequestParam(required = false) String room,
                       @RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                       @RequestParam(required = false) Long classId, RedirectAttributes ra) {
        try {
            academic.updateLecture(id, subjectId, dayOfWeek, startTime, endTime, room);
            ra.addFlashAttribute("success", "Lecture updated for every " + dayOfWeek + ".");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return back(year, month, classId);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @RequestParam(required = false) Integer year,
                         @RequestParam(required = false) Integer month, @RequestParam(required = false) Long classId,
                         RedirectAttributes ra) {
        academic.deleteLecture(id);
        ra.addFlashAttribute("success", "Lecture removed from the weekly pattern.");
        return back(year, month, classId);
    }
}
