package com.sanskaracademy.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.dto.FeeSummaryDto;
import com.sanskaracademy.entity.EnquiryStatus;
import com.sanskaracademy.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Numbers for the owner's one-page dashboard. */
@Service
public class OwnerDashboardService {

    @PersistenceContext
    private EntityManager em;

    private final UserRepository users;
    private final FeeService fees;
    private final TimetableService timetable;
    private final FeeRequestService feeRequests;
    private final EnquiryService enquiries;

    public OwnerDashboardService(UserRepository users, FeeService fees, TimetableService timetable,
                                 FeeRequestService feeRequests, EnquiryService enquiries) {
        this.users = users;
        this.fees = fees;
        this.timetable = timetable;
        this.feeRequests = feeRequests;
        this.enquiries = enquiries;
    }

    public static class Dash {
        public long students, teachers;
        public BigDecimal payable = BigDecimal.ZERO, paid = BigDecimal.ZERO, pending = BigDecimal.ZERO;
        public int overdueStudents;
        public int collectedPercent;
        public BigDecimal monthCollected = BigDecimal.ZERO, monthExpenses = BigDecimal.ZERO, monthSalary = BigDecimal.ZERO;
        public int attendanceTodayPercent = -1;
        public long attendanceTodayMarked;
        public int lecturesTakenPercent;
        public int lecturesTaken, lecturesNotTaken;
        public long feeRequestsPending, leavesPending, usersPending, followUpsDue;
        public long enquiriesNew, enquiriesMonth, enquiriesAdmitted;
        public List<String> labels = new ArrayList<>();
        public List<BigDecimal> collected = new ArrayList<>(), expenses = new ArrayList<>(), salaries = new ArrayList<>();
        public List<FeeSummaryDto> topPending = new ArrayList<>();

        public long getStudents() { return students; }
        public long getTeachers() { return teachers; }
        public BigDecimal getPayable() { return payable; }
        public BigDecimal getPaid() { return paid; }
        public BigDecimal getPending() { return pending; }
        public int getOverdueStudents() { return overdueStudents; }
        public int getCollectedPercent() { return collectedPercent; }
        public BigDecimal getMonthCollected() { return monthCollected; }
        public BigDecimal getMonthExpenses() { return monthExpenses; }
        public BigDecimal getMonthSalary() { return monthSalary; }
        public BigDecimal getMonthProfit() { return monthCollected.subtract(monthExpenses).subtract(monthSalary); }
        public int getAttendanceTodayPercent() { return attendanceTodayPercent; }
        public long getAttendanceTodayMarked() { return attendanceTodayMarked; }
        public int getLecturesTakenPercent() { return lecturesTakenPercent; }
        public int getLecturesTaken() { return lecturesTaken; }
        public int getLecturesNotTaken() { return lecturesNotTaken; }
        public long getFeeRequestsPending() { return feeRequestsPending; }
        public long getLeavesPending() { return leavesPending; }
        public long getUsersPending() { return usersPending; }
        public long getFollowUpsDue() { return followUpsDue; }
        public long getEnquiriesNew() { return enquiriesNew; }
        public long getEnquiriesMonth() { return enquiriesMonth; }
        public long getEnquiriesAdmitted() { return enquiriesAdmitted; }
        public List<String> getLabels() { return labels; }
        public List<BigDecimal> getCollected() { return collected; }
        public List<BigDecimal> getExpenses() { return expenses; }
        public List<BigDecimal> getSalaries() { return salaries; }
        public List<FeeSummaryDto> getTopPending() { return topPending; }
    }

    private BigDecimal sum(String jpql, Object... params) {
        var q = em.createQuery(jpql, BigDecimal.class);
        for (int i = 0; i < params.length; i++) q.setParameter(i + 1, params[i]);
        BigDecimal r = q.getSingleResult();
        return r == null ? BigDecimal.ZERO : r;
    }

    private long count(String jpql, Object... params) {
        var q = em.createQuery(jpql, Long.class);
        for (int i = 0; i < params.length; i++) q.setParameter(i + 1, params[i]);
        return q.getSingleResult();
    }

    @Transactional(readOnly = true)
    public Dash build() {
        Dash d = new Dash();
        LocalDate today = LocalDate.now();
        YearMonth now = YearMonth.from(today);
        d.students = users.countByRoleName("STUDENT");
        d.teachers = users.countByRoleName("TEACHER");

        List<FeeSummaryDto> all = fees.getAllFeeSummaries();
        for (FeeSummaryDto f : all) {
            if (f.getPayable() != null) d.payable = d.payable.add(f.getPayable());
            if (f.getPaid() != null) d.paid = d.paid.add(f.getPaid());
            if (f.getPending() != null && f.getPending().signum() > 0) {
                d.pending = d.pending.add(f.getPending());
                d.topPending.add(f);
            }
            if (f.isOverdue()) d.overdueStudents++;
        }
        d.collectedPercent = d.payable.signum() == 0 ? 0 : d.paid.multiply(BigDecimal.valueOf(100)).divide(d.payable, 0, java.math.RoundingMode.DOWN).intValue();
        d.topPending.sort((a, b) -> b.getPending().compareTo(a.getPending()));
        if (d.topPending.size() > 8) d.topPending = new ArrayList<>(d.topPending.subList(0, 8));

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            LocalDate from = ym.atDay(1), to = ym.atEndOfMonth();
            d.labels.add(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + String.valueOf(ym.getYear()).substring(2));
            d.collected.add(sum("SELECT COALESCE(SUM(f.amountPaid),0) FROM StudentFee f WHERE f.paymentDate BETWEEN ?1 AND ?2", from, to));
            d.expenses.add(sum("SELECT COALESCE(SUM(e.amount),0) FROM Expense e WHERE e.expenseDate BETWEEN ?1 AND ?2", from, to));
            d.salaries.add(sum("SELECT COALESCE(SUM(s.netAmount),0) FROM TeacherSalary s WHERE s.salaryYear = ?1 AND s.salaryMonth = ?2", ym.getYear(), ym.getMonthValue()));
        }
        d.monthCollected = d.collected.get(5);
        d.monthExpenses = d.expenses.get(5);
        d.monthSalary = d.salaries.get(5);

        long marked = count("SELECT COUNT(a) FROM Attendance a WHERE a.attendanceDate = ?1", today);
        long present = count("SELECT COUNT(a) FROM Attendance a WHERE a.attendanceDate = ?1 AND a.status = com.sanskaracademy.entity.AttendanceStatus.PRESENT", today);
        d.attendanceTodayMarked = marked;
        d.attendanceTodayPercent = marked == 0 ? -1 : (int) (present * 100 / marked);

        var rows = timetable.rows(now.atDay(1), now.atEndOfMonth(), null, null);
        var c = timetable.count(rows);
        d.lecturesTaken = c.getTaken();
        d.lecturesNotTaken = c.getNotTaken();
        int due = c.getTaken() + c.getNotTaken();
        d.lecturesTakenPercent = due == 0 ? 0 : c.getTaken() * 100 / due;

        d.feeRequestsPending = feeRequests.pendingCount();
        d.leavesPending = count("SELECT COUNT(l) FROM LeaveApplication l WHERE l.status = com.sanskaracademy.entity.LeaveStatus.PENDING");
        d.usersPending = users.findPendingUsers().size();
        d.followUpsDue = enquiries.due().size();
        d.enquiriesNew = enquiries.count(EnquiryStatus.NEW);
        d.enquiriesMonth = enquiries.lastDays(30);
        d.enquiriesAdmitted = enquiries.count(EnquiryStatus.ADMITTED);
        return d;
    }
}
