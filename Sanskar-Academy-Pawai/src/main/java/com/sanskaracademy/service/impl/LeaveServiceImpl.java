package com.sanskaracademy.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.LeaveApplication;
import com.sanskaracademy.entity.LeaveStatus;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.LeaveApplicationRepository;
import com.sanskaracademy.repository.RoleRepository;
import com.sanskaracademy.repository.StudentEnrollmentRepository;
import com.sanskaracademy.repository.StudentParentRepository;
import com.sanskaracademy.repository.SubjectRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.LeaveService;
import com.sanskaracademy.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveApplicationRepository leaveRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;
    private final StudentParentRepository studentParentRepository;
    private final NotificationService notificationService;

    // ================= APPLY =================
    @Override
    @Transactional
    public LeaveApplication applyForStudent(User applicant, Long studentId,
                                            LocalDate from, LocalDate to, String reason) {

        validate(from, to, reason);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        String role = applicant.getRole().getName();
        if ("STUDENT".equals(role)) {
            if (!applicant.getId().equals(studentId)) {
                throw new IllegalArgumentException("You can apply leave only for yourself");
            }
        } else if ("PARENT".equals(role)) {
            boolean linked = false;
            for (User child : studentParentRepository.findStudentsByParent(applicant.getId())) {
                if (child.getId().equals(studentId)) linked = true;
            }
            if (!linked) {
                throw new IllegalArgumentException("This student is not linked to your account");
            }
        } else {
            throw new IllegalArgumentException("You are not allowed to apply for student leave");
        }

        if (leaveRepository.countStudentOverlap(studentId, LeaveStatus.REJECTED, from, to) > 0) {
            throw new IllegalArgumentException("A leave request already exists for these dates");
        }

        LeaveApplication l = new LeaveApplication();
        l.setApplicant(applicant);
        l.setApplicantRole(role);
        l.setStudent(student);
        l.setFromDate(from);
        l.setToDate(to);
        l.setReason(reason.trim());
        l.setStatus(LeaveStatus.PENDING);
        l.setAppliedAt(LocalDateTime.now());
        l = leaveRepository.save(l);

        // tell the teachers of the student's class
        String msg = "New leave request for " + student.getFullName() + " (" + from + " to " + to + ").";
        Set<Long> notified = new HashSet<>();
        var enrollment = enrollmentRepository.findByStudentId(studentId);
        if (enrollment.isPresent()) {
            Long classId = enrollment.get().getClassEntity().getId();
            for (Subject s : subjectRepository.findByClassEntityId(classId)) {
                User t = s.getTeacher();
                if (notified.add(t.getId())) {
                    notificationService.sendNotification(t, msg);
                }
            }
        }
        return l;
    }

    @Override
    @Transactional
    public LeaveApplication applyForTeacher(User teacher, LocalDate from, LocalDate to, String reason) {

        validate(from, to, reason);

        if (!"TEACHER".equals(teacher.getRole().getName())) {
            throw new IllegalArgumentException("Only teachers can use this form");
        }
        if (leaveRepository.countTeacherOverlap(teacher.getId(), LeaveStatus.REJECTED, from, to) > 0) {
            throw new IllegalArgumentException("A leave request already exists for these dates");
        }

        LeaveApplication l = new LeaveApplication();
        l.setApplicant(teacher);
        l.setApplicantRole("TEACHER");
        l.setStudent(null);
        l.setFromDate(from);
        l.setToDate(to);
        l.setReason(reason.trim());
        l.setStatus(LeaveStatus.PENDING);
        l.setAppliedAt(LocalDateTime.now());
        l = leaveRepository.save(l);

        String msg = "New leave request from teacher " + teacher.getFullName()
                + " (" + from + " to " + to + ").";
        roleRepository.findByName("ADMIN").ifPresent(r -> {
            for (User admin : userRepository.findByRole(r)) {
                notificationService.sendNotification(admin, msg);
            }
        });
        return l;
    }

    // ================= READ =================
    @Override
    @Transactional(readOnly = true)
    public List<LeaveApplication> getAll() {
        return leaveRepository.findAllByOrderByAppliedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveApplication> getByApplicant(Long applicantId) {
        return leaveRepository.findByApplicantIdOrderByAppliedAtDesc(applicantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveApplication> getForStudent(Long studentId) {
        return leaveRepository.findByStudentIdOrderByAppliedAtDesc(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveApplication> getStudentLeavesForTeacher(Long teacherId) {
        return leaveRepository.findStudentLeavesForTeacher(teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return leaveRepository.countByStatus(LeaveStatus.PENDING);
    }

    // ================= REVIEW =================
    @Override
    @Transactional
    public void review(Long leaveId, User reviewer, boolean approve, String remarks) {

        LeaveApplication l = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        if (l.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException("This leave request is already " + l.getStatus());
        }

        String role = reviewer.getRole().getName();
        if ("TEACHER".equals(role)) {
            if (l.getStudent() == null) {
                throw new IllegalArgumentException("Teacher leave can be reviewed only by the admin");
            }
            boolean mine = false;
            for (LeaveApplication x : leaveRepository.findStudentLeavesForTeacher(reviewer.getId())) {
                if (x.getId().equals(leaveId)) mine = true;
            }
            if (!mine) {
                throw new IllegalArgumentException("This student is not in your classes");
            }
        } else if (!"ADMIN".equals(role)) {
            throw new IllegalArgumentException("You are not allowed to review leave requests");
        }

        l.setStatus(approve ? LeaveStatus.APPROVED : LeaveStatus.REJECTED);
        l.setReviewedBy(reviewer);
        l.setReviewedAt(LocalDateTime.now());
        l.setReviewRemarks(remarks == null || remarks.isBlank() ? null : remarks.trim());
        leaveRepository.save(l);

        String msg = "Leave for " + l.getPersonName() + " (" + l.getFromDate() + " to " + l.getToDate()
                + ") was " + l.getStatus() + " by " + reviewer.getFullName()
                + (l.getReviewRemarks() != null ? ". Remarks: " + l.getReviewRemarks() : ".");

        notificationService.sendNotification(l.getApplicant(), msg);
        if (l.getStudent() != null && !l.getStudent().getId().equals(l.getApplicant().getId())) {
            notificationService.sendNotification(l.getStudent(), msg);
        }
    }

    private void validate(LocalDate from, LocalDate to, String reason) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("From date and to date are required");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("To date cannot be before from date");
        }
        if (from.isBefore(LocalDate.now().minusDays(7))) {
            throw new IllegalArgumentException("Leave cannot start more than 7 days in the past");
        }
        if (ChronoUnit.DAYS.between(from, to) > 60) {
            throw new IllegalArgumentException("Leave cannot be longer than 60 days");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason is required");
        }
        if (reason.trim().length() > 500) {
            throw new IllegalArgumentException("Reason is too long (max 500 characters)");
        }
    }
}
