package com.sanskaracademy.service;

import java.time.LocalDate;
import java.util.List;

import com.sanskaracademy.entity.LeaveApplication;
import com.sanskaracademy.entity.User;

public interface LeaveService {

    /** applicant is the student himself OR a parent applying for the child. */
    LeaveApplication applyForStudent(User applicant, Long studentId,
                                     LocalDate from, LocalDate to, String reason);

    LeaveApplication applyForTeacher(User teacher, LocalDate from, LocalDate to, String reason);

    List<LeaveApplication> getAll();

    List<LeaveApplication> getByApplicant(Long applicantId);

    List<LeaveApplication> getForStudent(Long studentId);

    List<LeaveApplication> getStudentLeavesForTeacher(Long teacherId);

    long countPending();

    void review(Long leaveId, User reviewer, boolean approve, String remarks);
}
