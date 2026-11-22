package com.sanskaracademy.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.Holiday;
import com.sanskaracademy.entity.LectureSchedule;
import com.sanskaracademy.entity.LectureSession;
import com.sanskaracademy.entity.LectureStatus;
import com.sanskaracademy.entity.StudentEnrollment;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.AttendanceLookupRepository;
import com.sanskaracademy.repository.HolidayRepository;
import com.sanskaracademy.repository.LectureScheduleRepository;
import com.sanskaracademy.repository.LectureSessionRepository;
import com.sanskaracademy.repository.StudentEnrollmentRepository;
import com.sanskaracademy.repository.StudentParentRepository;
import com.sanskaracademy.repository.SubjectRepository;
import com.sanskaracademy.repository.UserRepository;

/**
 * Month-wise timetable: dated lectures made from the weekly pattern, extra classes, cancel / move,
 * "lecture taken" (marked by the teacher or worked out from attendance) and the month report.
 */
@Service("timetableService")
public class TimetableService {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("hh:mm a");
    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December"};

    private final LectureSessionRepository repo;
    private final AttendanceLookupRepository attendance;
    private final LectureScheduleRepository weekly;
    private final SubjectRepository subjects;
    private final HolidayRepository holidays;
    private final UserRepository users;
    private final StudentEnrollmentRepository enrollments;
    private final StudentParentRepository parents;
    private final NotificationClient messages;

    public TimetableService(LectureSessionRepository repo, AttendanceLookupRepository attendance,
                            LectureScheduleRepository weekly, SubjectRepository subjects, HolidayRepository holidays,
                            UserRepository users, StudentEnrollmentRepository enrollments,
                            StudentParentRepository parents, NotificationClient messages) {
        this.repo = repo;
        this.attendance = attendance;
        this.weekly = weekly;
        this.subjects = subjects;
        this.holidays = holidays;
        this.users = users;
        this.enrollments = enrollments;
        this.parents = parents;
        this.messages = messages;
    }

    // ================================================================== filter (year / month / week)
    public static class WeekInfo {
        private final int number;
        private final LocalDate from, to;
        WeekInfo(int number, LocalDate from, LocalDate to) {
            this.number = number;
            this.from = from;
            this.to = to;
        }
        public int getNumber() { return number; }
        public LocalDate getFrom() { return from; }
        public LocalDate getTo() { return to; }
        public String getLabel() {
            return "Week " + number + " (" + from.getDayOfMonth() + (from.equals(to) ? "" : "-" + to.getDayOfMonth()) + ")";
        }
    }

    /** weeks run Monday to Sunday; week 1 starts on the 1st of the month */
    public static List<WeekInfo> weeksOf(int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
        List<WeekInfo> out = new ArrayList<>();
        LocalDate start = first;
        int n = 1;
        while (!start.isAfter(last)) {
            LocalDate end = start.plusDays(7 - start.getDayOfWeek().getValue());   // the Sunday of that week
            if (end.isAfter(last)) end = last;
            out.add(new WeekInfo(n++, start, end));
            start = end.plusDays(1);
        }
        return out;
    }

    public static class Filter {
        private final int year, month, week;
        private final Long classId, teacherId;
        private final LocalDate from, to;
        private final List<WeekInfo> weeks;
        Filter(int year, int month, int week, Long classId, Long teacherId, LocalDate from, LocalDate to, List<WeekInfo> weeks) {
            this.year = year;
            this.month = month;
            this.week = week;
            this.classId = classId;
            this.teacherId = teacherId;
            this.from = from;
            this.to = to;
            this.weeks = weeks;
        }
        public int getYear() { return year; }
        public int getMonth() { return month; }
        public int getWeek() { return week; }
        public Long getClassId() { return classId; }
        public Long getTeacherId() { return teacherId; }
        public LocalDate getFrom() { return from; }
        public LocalDate getTo() { return to; }
        public List<WeekInfo> getWeeks() { return weeks; }
        public String getMonthName() { return MONTHS[month - 1]; }
        public String getTitle() {
            return MONTHS[month - 1] + " " + year + (week > 0 ? " - Week " + week : "");
        }
        public List<Integer> getYears() {
            int y = LocalDate.now(IST).getYear();
            List<Integer> l = new ArrayList<>();
            for (int i = y - 3; i <= y + 2; i++) l.add(i);
            if (!l.contains(year)) l.add(year);
            l.sort(Comparator.naturalOrder());
            return l;
        }
        public String[] getMonthNames() { return MONTHS; }
    }

    public Filter filter(Integer year, Integer month, Integer week, Long classId, Long teacherId) {
        LocalDate today = LocalDate.now(IST);
        int y = year == null ? today.getYear() : Math.max(2000, Math.min(2100, year));
        int m = month == null ? today.getMonthValue() : Math.max(1, Math.min(12, month));
        List<WeekInfo> weeks = weeksOf(y, m);
        int w = week == null ? 0 : week;
        LocalDate from = LocalDate.of(y, m, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        if (w >= 1 && w <= weeks.size()) {
            from = weeks.get(w - 1).getFrom();
            to = weeks.get(w - 1).getTo();
        } else {
            w = 0;
        }
        return new Filter(y, m, w, classId, teacherId, from, to, weeks);
    }

    // ================================================================== rows
    public static class Row {
        private final LectureSession session;
        private final String className, subjectName, teacherName, plannedTeacherName, state;
        private final boolean substituted, byAttendance, dateReached;
        private final Long teacherId;
        Row(LectureSession session, String className, String subjectName, String teacherName,
            String plannedTeacherName, String state, boolean substituted, boolean byAttendance,
            boolean dateReached, Long teacherId) {
            this.dateReached = dateReached;
            this.teacherId = teacherId;
            this.session = session;
            this.className = className;
            this.subjectName = subjectName;
            this.teacherName = teacherName;
            this.plannedTeacherName = plannedTeacherName;
            this.state = state;
            this.substituted = substituted;
            this.byAttendance = byAttendance;
        }
        public LectureSession getSession() { return session; }
        public Long getId() { return session.getId(); }
        public String getClassName() { return className; }
        public String getSubjectName() { return subjectName; }
        public String getTeacherName() { return teacherName; }
        public String getPlannedTeacherName() { return plannedTeacherName; }
        /** TAKEN, CANCELLED, NOT_TAKEN or PLANNED */
        public String getState() { return state; }
        public boolean isSubstituted() { return substituted; }
        public boolean isByAttendance() { return byAttendance; }
        public String getStateLabel() {
            switch (state) {
                case "TAKEN": return byAttendance && session.getStatus() != LectureStatus.TAKEN ? "Taken (attendance)" : "Taken";
                case "CANCELLED": return "Cancelled";
                case "NOT_TAKEN": return "Not taken";
                default: return "Planned";
            }
        }
        public String getBadgeClass() {
            switch (state) {
                case "TAKEN": return "bg-success";
                case "CANCELLED": return "bg-secondary";
                case "NOT_TAKEN": return "bg-danger";
                default: return "bg-primary";
            }
        }
        public String getTimeText() {
            return session.getStartTime().format(HM) + " - " + session.getEndTime().format(HM);
        }
        public boolean isCancelled() { return "CANCELLED".equals(state); }
        public boolean isTaken() { return "TAKEN".equals(state); }
        public boolean isMarkedTaken() { return session.getStatus() == LectureStatus.TAKEN; }
        /** not cancelled, not yet marked, and its date is today or earlier */
        public boolean isMarkable() { return !isCancelled() && !isMarkedTaken() && dateReached; }
        /** the same, and the given user is the teacher who takes this lecture */
        public boolean markableBy(Long userId) { return isMarkable() && teacherId != null && teacherId.equals(userId); }
    }

    private static boolean sameTeacher(User a, Long id) {
        return a != null && a.getId().equals(id);
    }

    /** the teacher filter matches the planned teacher and the substitute */
    @Transactional(readOnly = true)
    public List<Row> rows(LocalDate from, LocalDate to, Long classId, Long teacherId) {
        LocalDate today = LocalDate.now(IST);
        Set<String> marked = new HashSet<>();
        for (Object[] o : attendance.markedSubjectDates(from, to)) {
            marked.add(o[0] + "|" + o[1]);
        }
        List<Row> out = new ArrayList<>();
        for (LectureSession s : repo.findRange(from, to)) {
            Subject sub = s.getSubject();
            if (classId != null && !sub.getClassEntity().getId().equals(classId)) continue;
            User planned = sub.getTeacher();
            User actual = s.getSubstituteTeacher() != null ? s.getSubstituteTeacher() : planned;
            if (teacherId != null && !sameTeacher(planned, teacherId) && !sameTeacher(actual, teacherId)) continue;
            boolean byAtt = marked.contains(sub.getId() + "|" + s.getLectureDate());
            String state;
            if (s.getStatus() == LectureStatus.CANCELLED) state = "CANCELLED";
            else if (s.getStatus() == LectureStatus.TAKEN || byAtt) state = "TAKEN";
            else if (s.getLectureDate().isBefore(today)) state = "NOT_TAKEN";
            else state = "PLANNED";
            out.add(new Row(s, sub.getClassEntity().getClassName(), sub.getSubjectName(),
                    actual == null ? "-" : actual.getFullName(), planned == null ? "-" : planned.getFullName(),
                    state, s.getSubstituteTeacher() != null && planned != null
                            && !s.getSubstituteTeacher().getId().equals(planned.getId()), byAtt,
                    !s.getLectureDate().isAfter(today), actual == null ? null : actual.getId()));
        }
        return out;
    }

    public static class Counts {
        private int planned, taken, notTaken, cancelled;
        public int getPlanned() { return planned; }
        public int getTaken() { return taken; }
        public int getNotTaken() { return notTaken; }
        public int getCancelled() { return cancelled; }
        public int getTotal() { return planned + taken + notTaken + cancelled; }
    }

    public Counts count(List<Row> rows) {
        Counts c = new Counts();
        for (Row r : rows) {
            switch (r.getState()) {
                case "TAKEN": c.taken++; break;
                case "CANCELLED": c.cancelled++; break;
                case "NOT_TAKEN": c.notTaken++; break;
                default: c.planned++;
            }
        }
        return c;
    }

    // ================================================================== generate / add / edit
    public static class GenResult {
        private final int created, existing, holidaySkipped;
        GenResult(int created, int existing, int holidaySkipped) {
            this.created = created;
            this.existing = existing;
            this.holidaySkipped = holidaySkipped;
        }
        public int getCreated() { return created; }
        public int getExisting() { return existing; }
        public int getHolidaySkipped() { return holidaySkipped; }
    }

    /** make dated lectures from the weekly pattern; Sundays are NOT skipped (only the weekly pattern decides) */
    @Transactional
    public GenResult generate(Filter f, Long classId, boolean includeHolidays) {
        Set<LocalDate> off = new HashSet<>();
        if (!includeHolidays) {
            for (Holiday h : holidays.findByHolidayDateBetween(f.getFrom(), f.getTo())) off.add(h.getHolidayDate());
        }
        List<LectureSchedule> pattern = weekly.findAll();
        int created = 0, existing = 0, skipped = 0;
        for (LocalDate d = f.getFrom(); !d.isAfter(f.getTo()); d = d.plusDays(1)) {
            for (LectureSchedule l : pattern) {
                if (l.getDayOfWeek() != d.getDayOfWeek()) continue;
                Subject sub = l.getSubject();
                if (classId != null && !sub.getClassEntity().getId().equals(classId)) continue;
                if (off.contains(d)) {
                    skipped++;
                    continue;
                }
                if (repo.existsBySubjectIdAndLectureDateAndStartTime(sub.getId(), d, l.getStartTime())) {
                    existing++;
                    continue;
                }
                LectureSession s = new LectureSession();
                s.setSubject(sub);
                s.setLectureDate(d);
                s.setStartTime(l.getStartTime());
                s.setEndTime(l.getEndTime());
                s.setRoom(l.getRoom());
                s.setStatus(LectureStatus.PLANNED);
                repo.save(s);
                created++;
            }
        }
        return new GenResult(created, existing, skipped);
    }

    private User substitute(Long id, Subject sub) {
        if (id == null) return null;
        User u = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        String role = u.getRole() == null ? "" : u.getRole().getName();
        if (!role.endsWith("TEACHER")) throw new IllegalArgumentException("The substitute must be a teacher");
        if (sub.getTeacher() != null && sub.getTeacher().getId().equals(u.getId())) return null;   // same as planned
        return u;
    }

    private void validate(LectureSession s, Long excludeId) {
        if (s.getLectureDate() == null || s.getStartTime() == null || s.getEndTime() == null)
            throw new IllegalArgumentException("Date, start and end time are required");
        if (!s.getEndTime().isAfter(s.getStartTime())) throw new IllegalArgumentException("End time must be after start time");
        if (s.getStartTime().isBefore(LocalTime.of(5, 0)))
            throw new IllegalArgumentException("Start time " + s.getStartTime() + " looks wrong (before 5 AM). Please check AM / PM.");
        if (s.getEndTime().isAfter(LocalTime.of(23, 30)))
            throw new IllegalArgumentException("End time looks wrong (after 11:30 PM). Please check AM / PM.");
        if (s.getStatus() == LectureStatus.CANCELLED) return;
        Subject sub = s.getSubject();
        User eff = s.getSubstituteTeacher() != null ? s.getSubstituteTeacher() : sub.getTeacher();
        long clashes = repo.countClashes(excludeId == null ? -1L : excludeId, s.getLectureDate(), s.getStartTime(),
                s.getEndTime(), sub.getClassEntity().getId(), eff == null ? -1L : eff.getId());
        if (clashes > 0) throw new IllegalArgumentException("Clash: this class or this teacher already has a lecture at that time on "
                + s.getLectureDate().format(DMY));
    }

    @Transactional
    public LectureSession add(Long subjectId, LocalDate date, LocalTime start, LocalTime end, String room,
                              Long substituteId, String note, boolean notify) {
        Subject sub = subjects.findById(subjectId).orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        LectureSession s = new LectureSession();
        s.setSubject(sub);
        s.setLectureDate(date);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setRoom(clean(room, 60));
        s.setSubstituteTeacher(substitute(substituteId, sub));
        s.setNote(clean(note, 250));
        s.setExtra(true);
        validate(s, null);
        if (repo.existsBySubjectIdAndLectureDateAndStartTime(sub.getId(), date, start))
            throw new IllegalArgumentException("This subject already has a lecture at that time on that date");
        repo.save(s);
        if (notify) tellClass(s, "ADD", date, start, end);
        return s;
    }

    @Transactional
    public LectureSession update(Long id, Long subjectId, LocalDate date, LocalTime start, LocalTime end, String room,
                                 Long substituteId, String note, boolean notify) {
        LectureSession s = load(id);
        Long oldSubjectId = s.getSubject().getId();
        LocalDate oldDate = s.getLectureDate();
        LocalTime oldStart = s.getStartTime();
        LocalTime oldEnd = s.getEndTime();
        Subject sub = subjectId == null ? s.getSubject()
                : subjects.findById(subjectId).orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        s.setSubject(sub);
        s.setLectureDate(date);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setRoom(clean(room, 60));
        s.setSubstituteTeacher(substitute(substituteId, sub));
        s.setNote(clean(note, 250));
        validate(s, id);
        boolean slotChanged = !oldDate.equals(date) || !oldStart.equals(start) || !oldSubjectId.equals(sub.getId());
        if (slotChanged && repo.existsBySubjectIdAndLectureDateAndStartTime(sub.getId(), date, start))
            throw new IllegalArgumentException("This subject already has a lecture at that time on that date");
        repo.save(s);
        boolean moved = !oldDate.equals(date) || !oldStart.equals(start) || !oldEnd.equals(end);
        if (notify && moved) {
            tellClass(s, "CANCEL", oldDate, oldStart, oldEnd);
            tellClass(s, "ADD", date, start, end);
        }
        return s;
    }

    @Transactional
    public void cancel(Long id, String reason, boolean notify) {
        LectureSession s = load(id);
        s.setStatus(LectureStatus.CANCELLED);
        String r = clean(reason, 200);
        if (r != null) s.setNote(r);
        repo.save(s);
        if (notify) tellClass(s, "CANCEL", s.getLectureDate(), s.getStartTime(), s.getEndTime());
    }

    @Transactional
    public void restore(Long id) {
        LectureSession s = load(id);
        s.setStatus(LectureStatus.PLANNED);
        validate(s, id);
        repo.save(s);
    }

    @Transactional
    public void delete(Long id) {
        repo.delete(load(id));
    }

    // ================================================================== taken
    private LectureSession load(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Lecture not found"));
    }

    private static boolean mayMark(LectureSession s, User actor, boolean admin) {
        if (admin) return true;
        User eff = s.getSubstituteTeacher() != null ? s.getSubstituteTeacher() : s.getSubject().getTeacher();
        return eff != null && eff.getId().equals(actor.getId());
    }

    @Transactional
    public void markTaken(Long id, User actor, boolean admin, String topic, String note) {
        LectureSession s = load(id);
        if (!mayMark(s, actor, admin)) throw new IllegalArgumentException("This lecture is not yours");
        if (s.getStatus() == LectureStatus.CANCELLED) throw new IllegalArgumentException("A cancelled lecture cannot be marked as taken");
        if (s.getLectureDate().isAfter(LocalDate.now(IST))) throw new IllegalArgumentException("You can mark a lecture only on or after its date");
        s.setStatus(LectureStatus.TAKEN);
        s.setTopic(clean(topic, 250));
        if (note != null && !note.isBlank()) s.setNote(clean(note, 250));
        s.setMarkedBy(actor);
        s.setMarkedAt(LocalDateTime.now(IST));
        repo.save(s);
    }

    @Transactional
    public void unmarkTaken(Long id, User actor, boolean admin) {
        LectureSession s = load(id);
        if (!mayMark(s, actor, admin)) throw new IllegalArgumentException("This lecture is not yours");
        if (s.getStatus() == LectureStatus.TAKEN) {
            s.setStatus(LectureStatus.PLANNED);
            s.setMarkedBy(null);
            s.setMarkedAt(null);
            repo.save(s);
        }
    }

    // ================================================================== report
    public static class ReportLine {
        private final String teacher, subject, className;
        private int planned, taken, notTaken, cancelled, upcoming, substituteWork;
        ReportLine(String teacher, String subject, String className) {
            this.teacher = teacher;
            this.subject = subject;
            this.className = className;
        }
        public String getTeacher() { return teacher; }
        public String getSubject() { return subject; }
        public String getClassName() { return className; }
        /** lectures that were not cancelled */
        public int getPlanned() { return planned; }
        public int getTaken() { return taken; }
        public int getNotTaken() { return notTaken; }
        public int getCancelled() { return cancelled; }
        public int getUpcoming() { return upcoming; }
        /** lectures this teacher took in place of another teacher */
        public int getSubstituteWork() { return substituteWork; }
        /** taken out of the lectures whose date has passed */
        public int getPercent() {
            int past = taken + notTaken;
            return past == 0 ? 0 : (int) Math.round(taken * 100.0 / past);
        }
    }

    @Transactional(readOnly = true)
    public List<ReportLine> report(LocalDate from, LocalDate to, Long classId, Long teacherId) {
        Map<String, ReportLine> map = new LinkedHashMap<>();
        for (Row r : rows(from, to, classId, teacherId)) {
            String key = r.getTeacherName() + "|" + r.getSubjectName() + "|" + r.getClassName();
            ReportLine l = map.computeIfAbsent(key, k -> new ReportLine(r.getTeacherName(), r.getSubjectName(), r.getClassName()));
            switch (r.getState()) {
                case "TAKEN": l.taken++; l.planned++; if (r.isSubstituted()) l.substituteWork++; break;
                case "CANCELLED": l.cancelled++; break;
                case "NOT_TAKEN": l.notTaken++; l.planned++; break;
                default: l.upcoming++; l.planned++;
            }
        }
        List<ReportLine> out = new ArrayList<>(map.values());
        out.sort(Comparator.comparing(ReportLine::getTeacher).thenComparing(ReportLine::getClassName).thenComparing(ReportLine::getSubject));
        return out;
    }

    // ================================================================== students / parents view
    @Transactional(readOnly = true)
    public Long classOfStudent(Long studentId) {
        return enrollments.findByStudentId(studentId).map(e -> e.getClassEntity().getId()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<User> childrenOf(Long parentId) {
        return parents.findStudentsByParent(parentId);
    }

    // ================================================================== messages
    private void tellClass(LectureSession s, String action, LocalDate date, LocalTime start, LocalTime end) {
        if (!messages.isEnabled() || date.isBefore(LocalDate.now(IST))) return;
        Subject sub = s.getSubject();
        Map<String, String> p = new LinkedHashMap<>();
        p.put("subject", sub.getSubjectName());
        p.put("date", date.format(DMY));
        p.put("time", start.format(HM));
        String code = "ADD".equals(action) ? "EXTRA_CLASS" : "EXTRA_CLASS_CANCEL";
        if ("ADD".equals(action)) {
            User t = s.getSubstituteTeacher() != null ? s.getSubstituteTeacher() : sub.getTeacher();
            p.put("teacher", t == null ? "-" : t.getFullName());
        }
        Set<Long> done = new LinkedHashSet<>();
        for (StudentEnrollment en : enrollments.findByClassEntityId(sub.getClassEntity().getId())) {
            List<User> to = new ArrayList<>();
            to.add(en.getStudent());
            to.addAll(parents.findParentsByStudent(en.getStudent().getId()));
            for (User u : to) {
                if (!done.add(u.getId())) continue;
                messages.sendAsync(u, code, p, "TT-" + s.getId() + "-" + action + "-" + date + "-" + start + "-" + u.getId());
            }
        }
    }

    private static String clean(String s, int max) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
