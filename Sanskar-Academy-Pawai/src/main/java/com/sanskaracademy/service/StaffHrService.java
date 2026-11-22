package com.sanskaracademy.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.Holiday;
import com.sanskaracademy.entity.PayrollStatus;
import com.sanskaracademy.entity.StaffAttendance;
import com.sanskaracademy.entity.StaffAttendanceStatus;
import com.sanskaracademy.entity.StaffLeave;
import com.sanskaracademy.entity.StaffLeaveStatus;
import com.sanskaracademy.entity.StaffPayroll;
import com.sanskaracademy.entity.StaffProfile;
import com.sanskaracademy.entity.StaffType;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.entity.WorkSetting;
import com.sanskaracademy.repository.HolidayRepository;
import com.sanskaracademy.repository.RoleRepository;
import com.sanskaracademy.repository.StaffAttendanceRepository;
import com.sanskaracademy.repository.StaffLeaveRepository;
import com.sanskaracademy.repository.StaffPayrollRepository;
import com.sanskaracademy.repository.StaffProfileRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.repository.WorkSettingRepository;

/** Staff HR: staff records, attendance, leave, holidays, payroll. */
@Service
public class StaffHrService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final StaffProfileRepository staffRepo;
    private final StaffAttendanceRepository attRepo;
    private final StaffLeaveRepository leaveRepo;
    private final HolidayRepository holidayRepo;
    private final StaffPayrollRepository payRepo;
    private final WorkSettingRepository settingRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final UserService userService;
    private final NotificationService inApp;
    private final NotificationClient messages;
    private final PasswordEncoder encoder;

    public StaffHrService(StaffProfileRepository staffRepo, StaffAttendanceRepository attRepo,
                          StaffLeaveRepository leaveRepo, HolidayRepository holidayRepo,
                          StaffPayrollRepository payRepo, WorkSettingRepository settingRepo,
                          UserRepository userRepo, RoleRepository roleRepo, UserService userService,
                          NotificationService inApp, NotificationClient messages, PasswordEncoder encoder) {
        this.staffRepo = staffRepo;
        this.attRepo = attRepo;
        this.leaveRepo = leaveRepo;
        this.holidayRepo = holidayRepo;
        this.payRepo = payRepo;
        this.settingRepo = settingRepo;
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.userService = userService;
        this.inApp = inApp;
        this.messages = messages;
        this.encoder = encoder;
    }

    // ================= settings =================
    @Transactional
    public WorkSetting getSetting() {
        List<WorkSetting> all = settingRepo.findAll();
        if (!all.isEmpty()) return all.get(0);
        WorkSetting s = new WorkSetting();
        s.setPaidLeavePerMonth(new BigDecimal("2.0"));
        s.setMonthDivisor(30);
        s.setWeeklyOffDays("SUNDAY");
        return settingRepo.save(s);
    }

    @Transactional
    public void saveSetting(BigDecimal paidLeave, int divisor, List<String> offDays) {
        if (paidLeave == null || paidLeave.signum() < 0 || paidLeave.compareTo(new BigDecimal("31")) > 0)
            throw new IllegalArgumentException("Paid leave per month must be between 0 and 31");
        if (divisor < 20 || divisor > 31) throw new IllegalArgumentException("Salary divisor must be between 20 and 31");
        WorkSetting s = getSetting();
        s.setPaidLeavePerMonth(paidLeave);
        s.setMonthDivisor(divisor);
        s.setWeeklyOffDays(joinDays(offDays));
        settingRepo.save(s);
    }

    public static String joinDays(List<String> days) {
        if (days == null) return "";
        Set<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
        for (String d : days) {
            try {
                set.add(DayOfWeek.valueOf(d.trim().toUpperCase()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Unknown day: " + d);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (DayOfWeek d : set) {
            if (sb.length() > 0) sb.append(',');
            sb.append(d.name());
        }
        return sb.toString();
    }

    static Set<DayOfWeek> parseDays(String csv) {
        Set<DayOfWeek> s = EnumSet.noneOf(DayOfWeek.class);
        if (csv != null) {
            for (String p : csv.split(",")) {
                if (!p.isBlank()) s.add(DayOfWeek.valueOf(p.trim()));
            }
        }
        return s;
    }

    /** a staff member's own weekly off days, or the academy setting when none were chosen */
    public Set<DayOfWeek> offDaysOf(StaffProfile st) {
        String own = st.getWeeklyOffDays();
        return parseDays(own == null || own.isBlank() ? getSetting().getWeeklyOffDays() : own);
    }

    // ================= staff records =================
    @Transactional(readOnly = true)
    public List<StaffProfile> allStaff() {
        return staffRepo.findAllByOrderByUserFullNameAsc();
    }

    @Transactional(readOnly = true)
    public StaffProfile staffOf(User user) {
        return staffRepo.findByUserId(user.getId()).orElse(null);
    }

    @Transactional(readOnly = true)
    public StaffProfile getStaff(Long id) {
        return staffRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Staff not found"));
    }

    /** teacher / staff users who are not in the staff list yet */
    @Transactional(readOnly = true)
    public List<User> usersAvailableForStaff() {
        List<User> out = new ArrayList<>();
        for (String r : List.of("TEACHER", "STAFF")) {
            roleRepo.findByName(r).ifPresent(role -> {
                for (User u : userRepo.findByRole(role)) {
                    if (!staffRepo.existsByUserId(u.getId())) out.add(u);
                }
            });
        }
        return out;
    }

    @Transactional
    public StaffProfile addStaff(Long existingUserId, String fullName, String username, String password,
                                 String email, String mobile, String staffType, String designation,
                                 BigDecimal salary, LocalDate joinDate, LocalDate birthDate, List<String> offDays) {
        validateStaffFields(salary, staffType);
        User user;
        if (existingUserId != null) {
            user = userRepo.findById(existingUserId).orElseThrow(() -> new IllegalArgumentException("User not found"));
            String role = user.getRole().getName();
            if (!role.equals("TEACHER") && !role.equals("STAFF"))
                throw new IllegalArgumentException("Only teacher or staff logins can be added here");
            if (staffRepo.existsByUserId(user.getId())) throw new IllegalArgumentException("Already in the staff list");
        } else {
            if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required");
            if (username == null || username.trim().length() < 4) throw new IllegalArgumentException("Username must be at least 4 characters");
            if (password == null || password.length() < 8) throw new IllegalArgumentException("Password must be at least 8 characters");
            if (userRepo.existsByUsername(username.trim())) throw new IllegalArgumentException("Username already exists");
            if (email != null && !email.isBlank() && userRepo.findByEmail(email.trim()).isPresent())
                throw new IllegalArgumentException("This e-mail is already registered");
            User u = new User();
            u.setFullName(fullName.trim());
            u.setUsername(username.trim());
            u.setPassword(password);                       // encoded inside createUser
            u.setEmail(email == null || email.isBlank() ? null : email.trim());
            u.setMobileNumber(mobile);
            u.setEnabled(true);
            u.setEmailVerified(true);
            u.setApprovedByAdmin(true);
            user = userService.createUser(u, "STAFF");
        }
        StaffProfile st = new StaffProfile();
        st.setUser(user);
        fill(st, staffType, designation, salary, joinDate, birthDate, offDays);
        st.setActive(true);
        return staffRepo.save(st);
    }

    @Transactional
    public void updateStaff(Long id, String staffType, String designation, BigDecimal salary,
                            LocalDate joinDate, LocalDate birthDate, List<String> offDays, boolean active) {
        validateStaffFields(salary, staffType);
        StaffProfile st = getStaff(id);
        fill(st, staffType, designation, salary, joinDate, birthDate, offDays);
        st.setActive(active);
        staffRepo.save(st);
    }

    private void validateStaffFields(BigDecimal salary, String staffType) {
        if (salary == null || salary.signum() < 0 || salary.compareTo(new BigDecimal("10000000")) > 0)
            throw new IllegalArgumentException("Enter a valid monthly salary");
        try {
            StaffType.valueOf(staffType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Choose Teaching or Non-teaching");
        }
    }

    private void fill(StaffProfile st, String staffType, String designation, BigDecimal salary,
                      LocalDate joinDate, LocalDate birthDate, List<String> offDays) {
        st.setStaffType(StaffType.valueOf(staffType));
        st.setDesignation(designation == null || designation.isBlank() ? null : designation.trim());
        st.setMonthlySalary(salary);
        st.setJoinDate(joinDate);
        st.setBirthDate(birthDate);
        st.setWeeklyOffDays(joinDays(offDays));      // empty = use the academy setting
    }

    // ================= holidays =================
    @Transactional(readOnly = true)
    public List<Holiday> allHolidays() {
        return holidayRepo.findAllByOrderByHolidayDateDesc();
    }

    @Transactional(readOnly = true)
    public List<Holiday> upcomingHolidays() {
        return holidayRepo.findByHolidayDateGreaterThanEqualOrderByHolidayDateAsc(LocalDate.now());
    }

    @Transactional
    public void addHoliday(LocalDate date, String name, boolean wishes) {
        if (date == null) throw new IllegalArgumentException("Choose a date");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Enter the holiday name");
        if (holidayRepo.existsByHolidayDate(date)) throw new IllegalArgumentException("That date is already a holiday");
        Holiday h = new Holiday();
        h.setHolidayDate(date);
        h.setName(name.trim());
        h.setSendWishes(wishes);
        holidayRepo.save(h);
    }

    @Transactional
    public void deleteHoliday(Long id) {
        holidayRepo.deleteById(id);
    }

    private Set<LocalDate> holidayDates(LocalDate from, LocalDate to) {
        Set<LocalDate> s = new HashSet<>();
        for (Holiday h : holidayRepo.findByHolidayDateBetween(from, to)) s.add(h.getHolidayDate());
        return s;
    }

    /** a working day = not this person's weekly off, not a holiday, and on/after the joining date */
    List<LocalDate> workingDays(StaffProfile st, LocalDate from, LocalDate to) {
        Set<DayOfWeek> off = offDaysOf(st);
        Set<LocalDate> hol = holidayDates(from, to);
        List<LocalDate> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (off.contains(d.getDayOfWeek()) || hol.contains(d)) continue;
            if (st.getJoinDate() != null && d.isBefore(st.getJoinDate())) continue;
            out.add(d);
        }
        return out;
    }

    // ================= attendance =================
    /** one row of the attendance sheet of a date */
    public static class DayRow {
        public StaffProfile staff;
        public String status;        // PRESENT / ABSENT / HALF_DAY / LEAVE or "" (not marked)
        public String remark;
        public String offReason;     // null = working day; else "Weekly off" / holiday name
        public StaffProfile getStaff() { return staff; }
        public String getStatus() { return status; }
        public String getRemark() { return remark; }
        public String getOffReason() { return offReason; }
    }

    @Transactional(readOnly = true)
    public List<DayRow> sheetFor(LocalDate date) {
        Map<Long, StaffAttendance> marked = new HashMap<>();
        for (StaffAttendance a : attRepo.findByAttDate(date)) marked.put(a.getStaff().getId(), a);
        Holiday hol = holidayRepo.findByHolidayDate(date).orElse(null);
        List<DayRow> rows = new ArrayList<>();
        for (StaffProfile st : staffRepo.findByActiveTrueOrderByUserFullNameAsc()) {
            if (st.getJoinDate() != null && date.isBefore(st.getJoinDate())) continue;
            DayRow r = new DayRow();
            r.staff = st;
            StaffAttendance a = marked.get(st.getId());
            r.status = a == null ? "" : a.getStatus().name();
            r.remark = a == null ? "" : a.getRemark();
            if (hol != null) r.offReason = hol.getName();
            else if (offDaysOf(st).contains(date.getDayOfWeek())) r.offReason = "Weekly off";
            rows.add(r);
        }
        return rows;
    }

    /** statuses: staffId -> PRESENT / ABSENT / HALF_DAY / LEAVE, or empty to clear. */
    @Transactional
    public int saveSheet(LocalDate date, Map<Long, String> statuses, Map<Long, String> remarks) {
        if (date == null) throw new IllegalArgumentException("Choose a date");
        if (date.isAfter(LocalDate.now())) throw new IllegalArgumentException("Attendance cannot be marked for a future date");
        int n = 0;
        for (DayRow row : sheetFor(date)) {
            if (row.offReason != null) continue;                      // off day: nothing to mark
            String s = statuses.get(row.staff.getId());
            if (s == null) continue;
            StaffAttendance a = attRepo.findByStaffIdAndAttDate(row.staff.getId(), date).orElse(null);
            if (s.isBlank()) {
                if (a != null) attRepo.delete(a);
                continue;
            }
            StaffAttendanceStatus st;
            try {
                st = StaffAttendanceStatus.valueOf(s);
            } catch (Exception e) {
                throw new IllegalArgumentException("Wrong status for " + row.staff.getUser().getFullName());
            }
            if (a == null) {
                a = new StaffAttendance();
                a.setStaff(row.staff);
                a.setAttDate(date);
            }
            a.setStatus(st);
            String rm = remarks.get(row.staff.getId());
            a.setRemark(rm == null || rm.isBlank() ? null : (rm.length() > 190 ? rm.substring(0, 190) : rm));
            attRepo.save(a);
            n++;
        }
        return n;
    }

    @Transactional(readOnly = true)
    public List<StaffAttendance> monthAttendance(StaffProfile st, int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        return attRepo.findByStaffIdAndAttDateBetweenOrderByAttDateAsc(st.getId(), ym.atDay(1), ym.atEndOfMonth());
    }

    // ================= leave =================
    @Transactional(readOnly = true)
    public List<StaffLeave> allLeaves() {
        return leaveRepo.findAllByOrderByAppliedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<StaffLeave> leavesOf(StaffProfile st) {
        return leaveRepo.findByStaffIdOrderByFromDateDesc(st.getId());
    }

    @Transactional(readOnly = true)
    public long pendingLeaveCount() {
        return leaveRepo.countByStatus(StaffLeaveStatus.PENDING);
    }

    @Transactional
    public StaffLeave applyLeave(StaffProfile st, LocalDate from, LocalDate to, String reason) {
        if (from == null || to == null) throw new IllegalArgumentException("Choose both dates");
        if (to.isBefore(from)) throw new IllegalArgumentException("'To' date is before 'From' date");
        if (from.isBefore(LocalDate.now().minusDays(31))) throw new IllegalArgumentException("Leave older than 31 days cannot be applied");
        if (to.isAfter(LocalDate.now().plusDays(180))) throw new IllegalArgumentException("Leave too far in the future");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Please write a reason");
        if (!st.isActive()) throw new IllegalArgumentException("Your staff record is not active");
        int days = workingDays(st, from, to).size();
        if (days == 0) throw new IllegalArgumentException("All those days are already weekly off or holiday");
        if (!leaveRepo.findOverlapping(st.getId(), List.of(StaffLeaveStatus.PENDING, StaffLeaveStatus.APPROVED), from, to).isEmpty())
            throw new IllegalArgumentException("You already have a leave request for these dates");
        StaffLeave l = new StaffLeave();
        l.setStaff(st);
        l.setFromDate(from);
        l.setToDate(to);
        l.setDays(days);
        l.setReason(reason.trim().length() > 480 ? reason.trim().substring(0, 480) : reason.trim());
        l.setStatus(StaffLeaveStatus.PENDING);
        l.setAppliedAt(LocalDateTime.now());
        l = leaveRepo.save(l);

        String msg = "New staff leave request from " + st.getUser().getFullName() + " (" + range(from, to) + ", " + days + " working day(s)).";
        roleRepo.findByName("ADMIN").ifPresent(r -> {
            for (User admin : userRepo.findByRole(r)) inApp.sendNotification(admin, msg);
        });
        return l;
    }

    @Transactional
    public void cancelOwnLeave(StaffProfile st, Long leaveId) {
        StaffLeave l = leaveRepo.findById(leaveId).orElseThrow(() -> new IllegalArgumentException("Leave not found"));
        if (!l.getStaff().getId().equals(st.getId())) throw new IllegalArgumentException("Leave not found");
        if (l.getStatus() != StaffLeaveStatus.PENDING) throw new IllegalArgumentException("Only a waiting request can be cancelled");
        l.setStatus(StaffLeaveStatus.CANCELLED);
        leaveRepo.save(l);
    }

    @Transactional
    public void decideLeave(Long leaveId, boolean approve, String note) {
        StaffLeave l = leaveRepo.findById(leaveId).orElseThrow(() -> new IllegalArgumentException("Leave not found"));
        if (l.getStatus() != StaffLeaveStatus.PENDING) throw new IllegalArgumentException("Already decided");
        l.setStatus(approve ? StaffLeaveStatus.APPROVED : StaffLeaveStatus.REJECTED);
        l.setAdminNote(note == null || note.isBlank() ? null : (note.length() > 290 ? note.substring(0, 290) : note.trim()));
        l.setDecidedAt(LocalDateTime.now());
        leaveRepo.save(l);
        if (approve) {
            for (LocalDate d : workingDays(l.getStaff(), l.getFromDate(), l.getToDate())) {
                StaffAttendance a = attRepo.findByStaffIdAndAttDate(l.getStaff().getId(), d).orElseGet(() -> {
                    StaffAttendance n = new StaffAttendance();
                    n.setStaff(l.getStaff());
                    n.setAttDate(d);
                    return n;
                });
                a.setStatus(StaffAttendanceStatus.LEAVE);
                a.setRemark("Approved leave");
                attRepo.save(a);
            }
        }
        User u = l.getStaff().getUser();
        String status = approve ? "approved" : "rejected";
        inApp.sendNotification(u, "Your leave (" + range(l.getFromDate(), l.getToDate()) + ") was " + status + "."
                + (l.getAdminNote() == null ? "" : " Note: " + l.getAdminNote()));
        Map<String, String> p = new HashMap<>();
        p.put("name", u.getFullName());
        p.put("dates", range(l.getFromDate(), l.getToDate()));
        p.put("status", status);
        messages.sendAsync(u, "STAFF_LEAVE", p, "LEAVE-" + l.getId() + "-" + status);
    }

    private static String range(LocalDate a, LocalDate b) {
        return a.equals(b) ? a.format(DAY) : a.format(DAY) + " to " + b.format(DAY);
    }

    // ================= payroll =================
    @Transactional(readOnly = true)
    public List<StaffPayroll> payrollOf(int month, int year) {
        return payRepo.findByPayMonthAndPayYearOrderByStaffUserFullNameAsc(month, year);
    }

    @Transactional(readOnly = true)
    public List<StaffPayroll> myPayroll(StaffProfile st) {
        return payRepo.findByStaffIdOrderByPayYearDescPayMonthDesc(st.getId());
    }

    @Transactional(readOnly = true)
    public StaffPayroll getPayroll(Long id) {
        return payRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Payroll not found"));
    }

    /** Calculates (or re-calculates) every active staff member for the month. Paid rows are never touched. */
    @Transactional
    public int generatePayroll(int month, int year) {
        YearMonth ym;
        try {
            ym = YearMonth.of(year, month);
        } catch (Exception e) {
            throw new IllegalArgumentException("Wrong month");
        }
        if (ym.isAfter(YearMonth.now())) throw new IllegalArgumentException("Payroll cannot be made for a future month");
        WorkSetting set = getSetting();
        int count = 0;
        for (StaffProfile st : staffRepo.findByActiveTrueOrderByUserFullNameAsc()) {
            if (st.getJoinDate() != null && st.getJoinDate().isAfter(ym.atEndOfMonth())) continue;
            StaffPayroll p = payRepo.findByStaffIdAndPayMonthAndPayYear(st.getId(), month, year).orElse(null);
            if (p != null && p.getStatus() == PayrollStatus.PAID) continue;
            if (p == null) {
                p = new StaffPayroll();
                p.setStaff(st);
                p.setPayMonth(month);
                p.setPayYear(year);
                p.setOtherDeduction(BigDecimal.ZERO);
                p.setBonus(BigDecimal.ZERO);
                p.setStatus(PayrollStatus.DRAFT);
            }
            List<LocalDate> work = workingDays(st, ym.atDay(1), ym.atEndOfMonth());
            Set<LocalDate> workSet = new HashSet<>(work);
            int absent = 0, leave = 0, half = 0, present = 0;
            for (StaffAttendance a : attRepo.findByStaffIdAndAttDateBetweenOrderByAttDateAsc(st.getId(), ym.atDay(1), ym.atEndOfMonth())) {
                if (!workSet.contains(a.getAttDate())) continue;
                switch (a.getStatus()) {
                    case ABSENT: absent++; break;
                    case LEAVE: leave++; break;
                    case HALF_DAY: half++; break;
                    default: present++;
                }
            }
            int unmarked = work.size() - absent - leave - half - present;
            p.setWorkingDays(work.size());
            p.setAbsentDays(absent);
            p.setLeaveDays(leave);
            p.setHalfDays(half);
            p.setUnmarkedDays(unmarked);
            // unmarked working days are treated as present (a warning is shown to the admin)
            p.setPresentDays(new BigDecimal(present + unmarked).add(new BigDecimal("0.5").multiply(BigDecimal.valueOf(half))));
            p.setPaidLeaveAllowed(set.getPaidLeavePerMonth());
            p.setGrossSalary(st.getMonthlySalary());
            recalc(p, set);
            payRepo.save(p);
            count++;
        }
        return count;
    }

    private void recalc(StaffPayroll p, WorkSetting set) {
        PayrollCalc.Result r = PayrollCalc.compute(p.getGrossSalary(), set.getMonthDivisor(), p.getPaidLeaveAllowed(),
                p.getAbsentDays(), p.getLeaveDays(), p.getHalfDays(), p.getOtherDeduction(), p.getBonus());
        p.setDeductDays(r.deductDays);
        p.setPerDay(r.perDay);
        p.setLeaveDeduction(r.leaveDeduction);
        p.setNetSalary(r.net);
    }

    @Transactional
    public void adjustPayroll(Long id, BigDecimal other, BigDecimal bonus, String remarks) {
        StaffPayroll p = getPayroll(id);
        if (p.getStatus() == PayrollStatus.PAID) throw new IllegalArgumentException("Already paid - cannot change");
        if (other == null || other.signum() < 0 || bonus == null || bonus.signum() < 0)
            throw new IllegalArgumentException("Amounts cannot be negative");
        p.setOtherDeduction(other);
        p.setBonus(bonus);
        p.setRemarks(remarks == null || remarks.isBlank() ? null : (remarks.length() > 290 ? remarks.substring(0, 290) : remarks.trim()));
        recalc(p, getSetting());
        payRepo.save(p);
    }

    @Transactional
    public void markPaid(Long id) {
        StaffPayroll p = getPayroll(id);
        if (p.getStatus() == PayrollStatus.PAID) throw new IllegalArgumentException("Already paid");
        p.setStatus(PayrollStatus.PAID);
        p.setPaidDate(LocalDate.now());
        payRepo.save(p);
        User u = p.getStaff().getUser();
        inApp.sendNotification(u, "Salary for " + p.getMonthName() + " paid: Rs. " + p.getNetSalary());
        Map<String, String> m = new HashMap<>();
        m.put("name", u.getFullName());
        m.put("month", p.getMonthName());
        m.put("net", p.getNetSalary().toPlainString());
        messages.sendAsync(u, "STAFF_SALARY", m, "SALARY-" + p.getId());
    }

    // ================= daily wishes (called by the scheduled job) =================
    @Transactional(readOnly = true)
    public int sendTodaysWishes(LocalDate today) {
        int n = 0;
        for (StaffProfile st : staffRepo.findByActiveTrueOrderByUserFullNameAsc()) {
            LocalDate b = st.getBirthDate();
            if (b != null && b.getMonthValue() == today.getMonthValue() && b.getDayOfMonth() == today.getDayOfMonth()) {
                Map<String, String> p = new HashMap<>();
                p.put("name", st.getUser().getFullName());
                messages.sendAsync(st.getUser(), "BIRTHDAY", p, "BDAY-" + st.getUser().getId() + "-" + today.getYear());
                n++;
            }
        }
        Holiday h = holidayRepo.findByHolidayDate(today).orElse(null);
        if (h != null && h.isSendWishes()) {
            Map<String, String> p = new HashMap<>();
            p.put("festival", h.getName());
            messages.broadcastAsync(List.of("TEACHER", "STAFF", "PARENT", "STUDENT"), "FESTIVAL", p, "FEST-" + today);
            n++;
        }
        return n;
    }
}
