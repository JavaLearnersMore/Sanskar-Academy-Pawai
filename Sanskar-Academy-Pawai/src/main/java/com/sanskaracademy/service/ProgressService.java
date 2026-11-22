package com.sanskaracademy.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.Attendance;
import com.sanskaracademy.entity.AttendanceStatus;
import com.sanskaracademy.entity.AttemptStatus;
import com.sanskaracademy.entity.ExamAttempt;
import com.sanskaracademy.entity.LeaveApplication;
import com.sanskaracademy.entity.TeacherSalary;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.AttendanceRepository;
import com.sanskaracademy.repository.ExamAttemptRepository;
import com.sanskaracademy.repository.LeaveApplicationRepository;
import com.sanskaracademy.repository.TeacherSalaryRepository;

/** "My progress" for a student/parent and "My month" for a teacher. */
@Service
public class ProgressService {

    private final AttendanceRepository attendance;
    private final ExamAttemptRepository attempts;
    private final LeaveApplicationRepository leaves;
    private final TeacherSalaryRepository salaries;
    private final HomeworkService homework;
    private final TimetableService timetable;

    public ProgressService(AttendanceRepository attendance, ExamAttemptRepository attempts,
                           LeaveApplicationRepository leaves, TeacherSalaryRepository salaries,
                           HomeworkService homework, TimetableService timetable) {
        this.attendance = attendance;
        this.attempts = attempts;
        this.leaves = leaves;
        this.salaries = salaries;
        this.homework = homework;
        this.timetable = timetable;
    }

    public static class SubjectLine {
        private final String subject;
        private int present, total;
        private int score, outOf;
        SubjectLine(String s) { this.subject = s; }
        public String getSubject() { return subject; }
        public int getPresent() { return present; }
        public int getTotal() { return total; }
        public int getAttendancePercent() { return total == 0 ? 0 : present * 100 / total; }
        public int getScore() { return score; }
        public int getOutOf() { return outOf; }
        public int getMarksPercent() { return outOf == 0 ? 0 : score * 100 / outOf; }
        public boolean isHasExam() { return outOf > 0; }
    }

    public static class ExamPoint {
        private final String title, subject, date;
        private final int percent;
        ExamPoint(String t, String s, String d, int p) { title = t; subject = s; date = d; percent = p; }
        public String getTitle() { return title; }
        public String getSubject() { return subject; }
        public String getDate() { return date; }
        public int getPercent() { return percent; }
    }

    public static class Progress {
        private final List<SubjectLine> subjects = new ArrayList<>();
        private final List<ExamPoint> exams = new ArrayList<>();
        private int presentDays, totalDays;
        private int hwTotal, hwDone, hwOverdue;
        private int examAvg;
        public List<SubjectLine> getSubjects() { return subjects; }
        public List<ExamPoint> getExams() { return exams; }
        public int getAttendancePercent() { return totalDays == 0 ? 0 : presentDays * 100 / totalDays; }
        public int getPresentDays() { return presentDays; }
        public int getTotalDays() { return totalDays; }
        public int getHwTotal() { return hwTotal; }
        public int getHwDone() { return hwDone; }
        public int getHwOverdue() { return hwOverdue; }
        public int getExamAvg() { return examAvg; }
        public boolean isHasExams() { return !exams.isEmpty(); }
        public String getWeakest() {
            SubjectLine w = null;
            for (SubjectLine s : subjects) if (s.isHasExam() && (w == null || s.getMarksPercent() < w.getMarksPercent())) w = s;
            return w == null ? null : w.getSubject();
        }
        public String getStrongest() {
            SubjectLine w = null;
            for (SubjectLine s : subjects) if (s.isHasExam() && (w == null || s.getMarksPercent() > w.getMarksPercent())) w = s;
            return w == null ? null : w.getSubject();
        }
    }

    @Transactional(readOnly = true)
    public Progress progressOf(Long studentId) {
        Progress p = new Progress();
        Map<String, SubjectLine> by = new LinkedHashMap<>();
        for (Attendance a : attendance.findWithSubjectByStudent(studentId)) {
            SubjectLine l = by.computeIfAbsent(a.getSubject().getSubjectName(), SubjectLine::new);
            l.total++;
            p.totalDays++;
            if (a.getStatus() == AttendanceStatus.PRESENT) { l.present++; p.presentDays++; }
        }
        int sum = 0, n = 0;
        List<ExamAttempt> list = attempts.findByStudentIdOrderByStartedAtDesc(studentId);
        for (int i = list.size() - 1; i >= 0; i--) {
            ExamAttempt a = list.get(i);
            if (a.getStatus() != AttemptStatus.SUBMITTED || a.getTotalMarks() <= 0) continue;
            String sub = a.getExam().getSubject().getSubjectName();
            SubjectLine l = by.computeIfAbsent(sub, SubjectLine::new);
            l.score += a.getScore();
            l.outOf += a.getTotalMarks();
            int pc = a.getScore() * 100 / a.getTotalMarks();
            sum += pc;
            n++;
            LocalDate d = a.getSubmittedAt() == null ? a.getStartedAt().toLocalDate() : a.getSubmittedAt().toLocalDate();
            p.exams.add(new ExamPoint(a.getExam().getTitle(), sub, d.toString(), pc));
        }
        p.examAvg = n == 0 ? 0 : sum / n;
        p.subjects.addAll(by.values());
        for (HomeworkService.StudentRow r : homework.forStudent(studentId)) {
            p.hwTotal++;
            if (r.isSubmitted()) p.hwDone++;
            if (r.isOverdue()) p.hwOverdue++;
        }
        return p;
    }

    // ------------------------------------------------------------------ teacher: my month
    public static class MyMonth {
        private final int year, month;
        private final List<TimetableService.Row> rows;
        private final TimetableService.Counts counts;
        private final int substituteWork;
        private final List<LeaveApplication> leaves;
        private final int leaveDays;
        private final TeacherSalary salary;
        MyMonth(int y, int m, List<TimetableService.Row> r, TimetableService.Counts c, int sub,
                List<LeaveApplication> l, int ld, TeacherSalary s) {
            year = y; month = m; rows = r; counts = c; substituteWork = sub; leaves = l; leaveDays = ld; salary = s;
        }
        public int getYear() { return year; }
        public int getMonth() { return month; }
        public List<TimetableService.Row> getRows() { return rows; }
        public TimetableService.Counts getCounts() { return counts; }
        public int getSubstituteWork() { return substituteWork; }
        public List<LeaveApplication> getLeaves() { return leaves; }
        public int getLeaveDays() { return leaveDays; }
        public TeacherSalary getSalary() { return salary; }
        public int getPercent() {
            int due = counts.getTaken() + counts.getNotTaken();
            return due == 0 ? 0 : counts.getTaken() * 100 / due;
        }
    }

    @Transactional(readOnly = true)
    public MyMonth myMonth(User teacher, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1), to = ym.atEndOfMonth();
        List<TimetableService.Row> rows = timetable.rows(from, to, null, teacher.getId());
        int sub = 0;
        for (TimetableService.Row r : rows) if (r.isTaken() && r.isSubstituted()) sub++;
        List<LeaveApplication> ls = new ArrayList<>();
        int days = 0;
        for (LeaveApplication l : leaves.findByApplicantIdOrderByAppliedAtDesc(teacher.getId())) {
            if (l.getFromDate().isAfter(to) || l.getToDate().isBefore(from)) continue;
            ls.add(l);
            if (l.getStatus() == com.sanskaracademy.entity.LeaveStatus.APPROVED) {
                LocalDate a = l.getFromDate().isBefore(from) ? from : l.getFromDate();
                LocalDate b = l.getToDate().isAfter(to) ? to : l.getToDate();
                days += (int) (b.toEpochDay() - a.toEpochDay()) + 1;
            }
        }
        TeacherSalary sal = null;
        for (TeacherSalary s : salaries.findByTeacherIdOrderBySalaryYearDescSalaryMonthDesc(teacher.getId()))
            if (s.getSalaryYear() == year && s.getSalaryMonth() == month) { sal = s; break; }
        return new MyMonth(year, month, rows, timetable.count(rows), sub, ls, days, sal);
    }
}
