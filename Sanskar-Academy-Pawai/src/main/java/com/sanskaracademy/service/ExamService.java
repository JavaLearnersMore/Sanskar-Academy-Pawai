package com.sanskaracademy.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.sanskaracademy.dto.AiQuestion;
import com.sanskaracademy.dto.ExamStatsDto;
import com.sanskaracademy.dto.StudentExamDto;
import com.sanskaracademy.dto.StudentLevelDto;
import com.sanskaracademy.entity.Exam;
import com.sanskaracademy.entity.ExamAttempt;
import com.sanskaracademy.entity.QuestionType;
import com.sanskaracademy.entity.User;

public interface ExamService {

    // ---------------- teacher ----------------
    Exam createExam(User teacher, Long subjectId, String title, String description,
                    int durationMinutes, LocalDateTime dueDate);

    List<Exam> getTeacherExams(Long teacherId);

    Exam getTeacherExam(Long examId, Long teacherId);

    void addQuestion(Long examId, Long teacherId, QuestionType type, String text,
                     String a, String b, String c, String d, String correct,
                     String modelAnswer, int marks);

    int addAiQuestions(Long examId, Long teacherId, List<AiQuestion> questions, int marksEach);

    void deleteQuestion(Long examId, Long questionId, Long teacherId);

    void publish(Long examId, Long teacherId);

    void close(Long examId, Long teacherId);

    List<ExamAttempt> getSubmittedAttempts(Long examId, Long teacherId);

    ExamAttempt getAttemptForTeacher(Long attemptId, Long teacherId);

    void overrideMarks(Long attemptId, Long teacherId, Map<Long, Integer> marks, Map<Long, String> feedback);

    ExamStatsDto getStats(Long examId, Long teacherId);

    List<StudentLevelDto> getStudentLevels(Long teacherId);

    long countPendingReview(Long teacherId);

    /** every student of the exam's class with live status (not started / writing / submitted) */
    java.util.List<com.sanskaracademy.dto.MonitorRowDto> getMonitor(Long examId, Long teacherId);

    // ---------------- student ----------------
    /** true while the student has an exam started and its timer has not ended (AI help is locked) */
    boolean hasActiveExam(Long studentId);

    List<StudentExamDto> getStudentExams(Long studentId);

    ExamAttempt startAttempt(Long studentId, Long examId);

    ExamAttempt submitAttempt(Long studentId, Long attemptId, Map<Long, String> answers);

    ExamAttempt getStudentAttempt(Long attemptId, Long studentId);

    /** finished attempts (submitted or waiting for review), newest first */
    List<ExamAttempt> getStudentResults(Long studentId);

    // ---------------- generic (caller checks who may see it) ----------------
    ExamAttempt getAttempt(Long attemptId);
}
