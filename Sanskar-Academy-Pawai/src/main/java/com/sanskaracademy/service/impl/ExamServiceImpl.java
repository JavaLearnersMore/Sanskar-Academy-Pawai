package com.sanskaracademy.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.dto.AiGrade;
import com.sanskaracademy.dto.AiQuestion;
import com.sanskaracademy.dto.ExamStatsDto;
import com.sanskaracademy.dto.StudentExamDto;
import com.sanskaracademy.dto.StudentLevelDto;
import com.sanskaracademy.entity.AttemptStatus;
import com.sanskaracademy.entity.Exam;
import com.sanskaracademy.entity.ExamAnswer;
import com.sanskaracademy.entity.ExamAttempt;
import com.sanskaracademy.entity.ExamQuestion;
import com.sanskaracademy.entity.ExamStatus;
import com.sanskaracademy.entity.QuestionType;
import com.sanskaracademy.entity.StudentEnrollment;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.ExamAttemptRepository;
import com.sanskaracademy.repository.ExamRepository;
import com.sanskaracademy.repository.StudentEnrollmentRepository;
import com.sanskaracademy.repository.StudentParentRepository;
import com.sanskaracademy.repository.SubjectRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AiTutorService;
import com.sanskaracademy.service.ExamService;
import com.sanskaracademy.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    /** extra minutes allowed after the timer ends (slow network) */
    private static final int GRACE_MINUTES = 2;

    private final ExamRepository examRepository;
    private final ExamAttemptRepository attemptRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentParentRepository studentParentRepository;
    private final NotificationService notificationService;
    private final AiTutorService aiTutorService;
    private final com.sanskaracademy.service.ExamGuardService guardService;

    // =====================================================================
    // TEACHER - build exam
    // =====================================================================
    @Override
    @Transactional
    public Exam createExam(User teacher, Long subjectId, String title, String description,
                           int durationMinutes, LocalDateTime dueDate) {

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        if (!subject.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("This subject is not assigned to you");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Exam title is required");
        }
        if (title.trim().length() > 150) {
            throw new IllegalArgumentException("Exam title is too long");
        }
        if (durationMinutes < 5 || durationMinutes > 240) {
            throw new IllegalArgumentException("Duration must be between 5 and 240 minutes");
        }
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Last date to start cannot be in the past");
        }

        Exam e = new Exam();
        e.setTitle(title.trim());
        e.setDescription(description == null || description.isBlank() ? null : cut(description.trim(), 500));
        e.setSubject(subject);
        e.setCreatedBy(teacher);
        e.setDurationMinutes(durationMinutes);
        e.setDueDate(dueDate);
        e.setStatus(ExamStatus.DRAFT);
        return examRepository.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Exam> getTeacherExams(Long teacherId) {
        return examRepository.findBySubjectTeacherIdOrderByCreatedAtDesc(teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public Exam getTeacherExam(Long examId, Long teacherId) {
        Exam e = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        if (!e.getSubject().getTeacher().getId().equals(teacherId)) {
            throw new IllegalArgumentException("This exam does not belong to you");
        }
        return e;
    }

    @Override
    @Transactional
    public void addQuestion(Long examId, Long teacherId, QuestionType type, String text,
                            String a, String b, String c, String d, String correct,
                            String modelAnswer, int marks) {

        Exam exam = editableExam(examId, teacherId);

        if (text == null || text.isBlank()) throw new IllegalArgumentException("Question text is required");
        if (text.trim().length() > 1000) throw new IllegalArgumentException("Question is too long");
        if (marks < 1 || marks > 100) throw new IllegalArgumentException("Marks must be between 1 and 100");

        ExamQuestion q = new ExamQuestion();
        q.setExam(exam);
        q.setQuestionText(text.trim());
        q.setType(type);
        q.setMarks(marks);

        if (type == QuestionType.MCQ) {
            if (blank(a) || blank(b) || blank(c) || blank(d)) {
                throw new IllegalArgumentException("Fill all four options");
            }
            if (correct == null || !correct.matches("[ABCD]")) {
                throw new IllegalArgumentException("Choose the correct option");
            }
            q.setOptionA(cut(a.trim(), 300));
            q.setOptionB(cut(b.trim(), 300));
            q.setOptionC(cut(c.trim(), 300));
            q.setOptionD(cut(d.trim(), 300));
            q.setCorrectOption(correct);
        } else {
            if (blank(modelAnswer)) {
                throw new IllegalArgumentException("Write the model answer (used for marking)");
            }
            q.setModelAnswer(cut(modelAnswer.trim(), 1000));
        }
        exam.getQuestions().add(q);
        examRepository.save(exam);
    }

    @Override
    @Transactional
    public int addAiQuestions(Long examId, Long teacherId, List<AiQuestion> questions, int marksEach) {

        Exam exam = editableExam(examId, teacherId);
        int marks = Math.max(1, Math.min(marksEach, 100));
        String letters = "ABCD";
        int added = 0;

        for (AiQuestion aq : questions) {
            if (aq.options() == null || aq.options().size() < 4
                    || aq.correctIndex() < 0 || aq.correctIndex() > 3) continue;

            ExamQuestion q = new ExamQuestion();
            q.setExam(exam);
            q.setQuestionText(cut(aq.question().trim(), 1000));
            q.setType(QuestionType.MCQ);
            q.setMarks(marks);
            q.setOptionA(cut(aq.options().get(0), 300));
            q.setOptionB(cut(aq.options().get(1), 300));
            q.setOptionC(cut(aq.options().get(2), 300));
            q.setOptionD(cut(aq.options().get(3), 300));
            q.setCorrectOption(String.valueOf(letters.charAt(aq.correctIndex())));
            exam.getQuestions().add(q);
            added++;
        }
        examRepository.save(exam);
        return added;
    }

    @Override
    @Transactional
    public void deleteQuestion(Long examId, Long questionId, Long teacherId) {
        Exam exam = editableExam(examId, teacherId);
        exam.getQuestions().removeIf(q -> q.getId().equals(questionId));
        examRepository.save(exam);
    }

    @Override
    @Transactional
    public void publish(Long examId, Long teacherId) {
        Exam exam = editableExam(examId, teacherId);
        if (exam.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("Add at least one question before publishing");
        }
        exam.setStatus(ExamStatus.PUBLISHED);
        examRepository.save(exam);

        Long classId = exam.getSubject().getClassEntity().getId();
        String msg = "New exam: " + exam.getTitle() + " (" + exam.getSubject().getSubjectName() + ") - "
                + exam.getTotalMarks() + " marks, " + exam.getDurationMinutes() + " minutes."
                + (exam.getDueDate() != null ? " Start before " + exam.getDueDate().toLocalDate() + "." : "");
        for (StudentEnrollment en : enrollmentRepository.findByClassEntityId(classId)) {
            notificationService.sendNotification(en.getStudent(), msg);
        }
    }

    @Override
    @Transactional
    public void close(Long examId, Long teacherId) {
        Exam exam = getTeacherExam(examId, teacherId);
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only a published exam can be closed");
        }
        exam.setStatus(ExamStatus.CLOSED);
        examRepository.save(exam);
    }

    // =====================================================================
    // TEACHER - results
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<ExamAttempt> getSubmittedAttempts(Long examId, Long teacherId) {
        getTeacherExam(examId, teacherId);
        List<ExamAttempt> list = new ArrayList<>(
                attemptRepository.findByExamIdAndStatusNotOrderBySubmittedAtDesc(examId, AttemptStatus.IN_PROGRESS));
        list.sort(Comparator.comparingInt(ExamAttempt::getPercentage).reversed());
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamAttempt getAttemptForTeacher(Long attemptId, Long teacherId) {
        ExamAttempt a = getAttempt(attemptId);
        if (!a.getExam().getSubject().getTeacher().getId().equals(teacherId)) {
            throw new IllegalArgumentException("This result does not belong to your exam");
        }
        return a;
    }

    @Override
    @Transactional
    public void overrideMarks(Long attemptId, Long teacherId, Map<Long, Integer> marks, Map<Long, String> feedback) {

        ExamAttempt attempt = getAttemptForTeacher(attemptId, teacherId);
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("The student has not submitted this exam yet");
        }

        for (ExamAnswer a : attempt.getAnswers()) {
            if (a.getQuestion().getType() != QuestionType.SHORT) continue;

            Integer m = marks.get(a.getId());
            if (m != null) {
                a.setMarksAwarded(Math.max(0, Math.min(m, a.getQuestion().getMarks())));
                a.setTeacherGraded(true);
                a.setCorrect(a.getMarksAwarded() == a.getQuestion().getMarks());
            }
            String fb = feedback.get(a.getId());
            if (fb != null && !fb.isBlank()) {
                a.setFeedback(cut(fb.trim(), 1000));
            }
        }

        recalc(attempt);
        attemptRepository.save(attempt);

        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            String msg = "Result updated: " + attempt.getExam().getTitle() + " - "
                    + attempt.getScore() + "/" + attempt.getTotalMarks()
                    + " (" + attempt.getPercentage() + "%).";
            notificationService.sendNotification(attempt.getStudent(), msg);
            for (User p : studentParentRepository.findParentsByStudent(attempt.getStudent().getId())) {
                notificationService.sendNotification(p,
                        attempt.getStudent().getFullName() + " - " + msg);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.sanskaracademy.dto.MonitorRowDto> getMonitor(Long examId, Long teacherId) {
        Exam exam = getTeacherExam(examId, teacherId);
        Map<Long, ExamAttempt> byStudent = new HashMap<>();
        for (ExamAttempt a : attemptRepository.findByExamId(examId)) {
            byStudent.put(a.getStudent().getId(), a);
        }
        List<com.sanskaracademy.dto.MonitorRowDto> rows = new ArrayList<>();
        for (StudentEnrollment en : enrollmentRepository.findByClassEntityId(exam.getSubject().getClassEntity().getId())) {
            User st = en.getStudent();
            ExamAttempt a = byStudent.get(st.getId());
            String state;
            if (a == null) state = "NOT_STARTED";
            else if (a.getStatus() == AttemptStatus.IN_PROGRESS) state = "IN_PROGRESS";
            else if (a.isPending()) state = "REVIEW";
            else state = "SUBMITTED";
            rows.add(new com.sanskaracademy.dto.MonitorRowDto(st, a, state));
        }
        rows.sort(Comparator.comparing((com.sanskaracademy.dto.MonitorRowDto r) -> r.getStudent().getFullName(),
                String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamStatsDto getStats(Long examId, Long teacherId) {

        Exam exam = getTeacherExam(examId, teacherId);
        List<ExamAttempt> attempts = attemptRepository
                .findByExamIdAndStatusNotOrderBySubmittedAtDesc(examId, AttemptStatus.IN_PROGRESS);

        int enrolled = enrollmentRepository.findByClassEntityId(exam.getSubject().getClassEntity().getId()).size();

        int sum = 0, high = 0, low = 100, passed = 0, counted = 0;
        for (ExamAttempt a : attempts) {
            if (a.isPending()) continue;
            int p = a.getPercentage();
            sum += p;
            high = Math.max(high, p);
            low = Math.min(low, p);
            if (a.isPassed()) passed++;
            counted++;
        }

        // per-question accuracy
        Map<Long, int[]> acc = new HashMap<>();      // qid -> {obtained, available, answered}
        for (ExamAttempt a : attempts) {
            for (ExamAnswer ans : a.getAnswers()) {
                if (ans.getMarksAwarded() == null) continue;
                int[] x = acc.computeIfAbsent(ans.getQuestion().getId(), k -> new int[3]);
                x[0] += ans.getMarksAwarded();
                x[1] += ans.getQuestion().getMarks();
                x[2]++;
            }
        }
        List<ExamStatsDto.QuestionStat> qs = new ArrayList<>();
        int no = 1;
        for (ExamQuestion q : exam.getQuestions()) {
            int[] x = acc.get(q.getId());
            int percent = (x == null || x[1] == 0) ? 0 : (int) Math.round(x[0] * 100.0 / x[1]);
            qs.add(new ExamStatsDto.QuestionStat(q.getId(), no++, q.getQuestionText(), percent, x == null ? 0 : x[2]));
        }

        return new ExamStatsDto(enrolled, attempts.size(), counted == 0 ? 0 : sum / counted,
                counted == 0 ? 0 : high, counted == 0 ? 0 : low, passed, qs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentLevelDto> getStudentLevels(Long teacherId) {

        // classes this teacher teaches
        Map<Long, String> classNames = new LinkedHashMap<>();
        for (Subject s : subjectRepository.findSubjectsByTeacherId(teacherId)) {
            classNames.put(s.getClassEntity().getId(), s.getClassEntity().getClassName());
        }

        // finished, marked attempts on this teacher's exams, grouped by student
        Map<Long, List<ExamAttempt>> byStudent = new HashMap<>();
        for (ExamAttempt a : attemptRepository.findByExamSubjectTeacherIdAndStatusNot(teacherId, AttemptStatus.IN_PROGRESS)) {
            if (a.isPending()) continue;
            byStudent.computeIfAbsent(a.getStudent().getId(), k -> new ArrayList<>()).add(a);
        }

        List<StudentLevelDto> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (Map.Entry<Long, String> c : classNames.entrySet()) {
            for (StudentEnrollment en : enrollmentRepository.findByClassEntityId(c.getKey())) {
                User st = en.getStudent();
                if (!seen.add(st.getId())) continue;

                List<ExamAttempt> list = byStudent.getOrDefault(st.getId(), List.of());
                if (list.isEmpty()) {
                    result.add(new StudentLevelDto(st.getId(), st.getFullName(), c.getValue(), 0, 0, 0, "NONE"));
                    continue;
                }
                List<ExamAttempt> sorted = new ArrayList<>(list);
                sorted.sort(Comparator.comparing(ExamAttempt::getSubmittedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())));

                int total = 0;
                for (ExamAttempt a : sorted) total += a.getPercentage();
                int avg = total / sorted.size();
                int last = sorted.get(sorted.size() - 1).getPercentage();

                String trend;
                if (sorted.size() == 1) {
                    trend = "NEW";
                } else {
                    int before = (total - last) / (sorted.size() - 1);
                    trend = last >= before + 5 ? "UP" : (last <= before - 5 ? "DOWN" : "STEADY");
                }
                result.add(new StudentLevelDto(st.getId(), st.getFullName(), c.getValue(),
                        sorted.size(), avg, last, trend));
            }
        }
        // weakest students first
        result.sort(Comparator
                .comparing((StudentLevelDto d) -> d.getExamsTaken() == 0)
                .thenComparingInt(StudentLevelDto::getAveragePercent));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingReview(Long teacherId) {
        return attemptRepository.countByExamSubjectTeacherIdAndStatus(teacherId, AttemptStatus.PENDING_REVIEW);
    }

    // =====================================================================
    // STUDENT
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveExam(Long studentId) {
        for (ExamAttempt a : attemptRepository.findByStudentIdAndStatus(studentId, AttemptStatus.IN_PROGRESS)) {
            if (!a.isExpired()) return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentExamDto> getStudentExams(Long studentId) {

        Optional<StudentEnrollment> en = enrollmentRepository.findByStudentId(studentId);
        if (en.isEmpty()) return List.of();

        Map<Long, ExamAttempt> attempts = new HashMap<>();
        for (ExamAttempt a : attemptRepository.findByStudentIdOrderByStartedAtDesc(studentId)) {
            attempts.put(a.getExam().getId(), a);
        }

        List<StudentExamDto> list = new ArrayList<>();
        for (Exam e : examRepository.findBySubjectClassEntityIdAndStatusInOrderByCreatedAtDesc(
                en.get().getClassEntity().getId(), List.of(ExamStatus.PUBLISHED, ExamStatus.CLOSED))) {

            ExamAttempt a = attempts.get(e.getId());
            String state;
            if (a != null && a.getStatus() != AttemptStatus.IN_PROGRESS) {
                state = "DONE";
            } else if (a != null) {
                state = a.isExpired() ? "MISSED" : "IN_PROGRESS";
            } else {
                state = e.isOpenForStart() ? "AVAILABLE" : "MISSED";
            }
            list.add(new StudentExamDto(e, a, state));
        }
        return list;
    }

    @Override
    @Transactional
    public ExamAttempt startAttempt(Long studentId, Long examId) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        Long myClass = enrollmentRepository.findByStudentId(studentId)
                .map(en -> en.getClassEntity().getId()).orElse(null);
        if (myClass == null || !myClass.equals(exam.getSubject().getClassEntity().getId())
                || exam.getStatus() == ExamStatus.DRAFT) {
            throw new IllegalArgumentException("This exam is not available for you");
        }

        Optional<ExamAttempt> existing = attemptRepository.findByExamIdAndStudentId(examId, studentId);
        if (existing.isPresent()) {
            ExamAttempt a = existing.get();
            if (a.getStatus() != AttemptStatus.IN_PROGRESS) {
                throw new IllegalArgumentException("You have already submitted this exam");
            }
            if (a.getRemainingSeconds() == 0) {
                // time ran out while the student was away - record it with no answers
                gradeAndSave(a, Map.of());
                throw new IllegalArgumentException("Your time for this exam was over. It was submitted automatically.");
            }
            return a;
        }

        if (!exam.isOpenForStart()) {
            throw new IllegalArgumentException("This exam is closed");
        }

        ExamAttempt a = new ExamAttempt();
        a.setExam(exam);
        a.setStudent(student);
        a.setStartedAt(LocalDateTime.now());
        a.setStatus(AttemptStatus.IN_PROGRESS);
        a.setTotalMarks(exam.getTotalMarks());
        a = attemptRepository.save(a);
        guardService.getOrAssign(a);      // personal paper (order / pool / options)
        return a;
    }

    @Override
    @Transactional
    public ExamAttempt submitAttempt(Long studentId, Long attemptId, Map<Long, String> answers) {

        ExamAttempt a = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        if (!a.getStudent().getId().equals(studentId)) {
            throw new IllegalArgumentException("This is not your exam");
        }
        if (a.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("You have already submitted this exam");
        }

        // after the timer + grace period answers are not accepted
        boolean late = LocalDateTime.now().isAfter(
                a.getStartedAt().plusMinutes(a.getExam().getDurationMinutes() + GRACE_MINUTES));

        return gradeAndSave(a, late ? Map.of() : answers);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamAttempt getStudentAttempt(Long attemptId, Long studentId) {
        ExamAttempt a = getAttempt(attemptId);
        if (!a.getStudent().getId().equals(studentId)) {
            throw new IllegalArgumentException("This result is not yours");
        }
        return a;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamAttempt> getStudentResults(Long studentId) {
        List<ExamAttempt> list = new ArrayList<>();
        for (ExamAttempt a : attemptRepository.findByStudentIdOrderByStartedAtDesc(studentId)) {
            if (a.getStatus() != AttemptStatus.IN_PROGRESS) list.add(a);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamAttempt getAttempt(Long attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Result not found"));
    }

    // =====================================================================
    // grading
    // =====================================================================
    private ExamAttempt gradeAndSave(ExamAttempt attempt, Map<Long, String> given) {

        attempt.getAnswers().clear();

        for (ExamQuestion q : guardService.questionsFor(attempt)) {

            ExamAnswer ans = new ExamAnswer();
            ans.setAttempt(attempt);
            ans.setQuestion(q);
            String value = given.get(q.getId());

            if (q.getType() == QuestionType.MCQ) {
                String sel = value != null && value.matches("[ABCD]") ? value : null;
                ans.setSelectedOption(sel);
                boolean ok = sel != null && sel.equals(q.getCorrectOption());
                ans.setCorrect(ok);
                ans.setMarksAwarded(ok ? q.getMarks() : 0);

            } else {
                String text = value == null ? "" : cut(value.trim(), 2000);
                ans.setAnswerText(text.isEmpty() ? null : text);

                if (text.isEmpty()) {
                    ans.setMarksAwarded(0);
                    ans.setCorrect(false);
                    ans.setFeedback("No answer written.");
                } else {
                    AiGrade g = aiTutorService.gradeShortAnswer(
                            q.getQuestionText(), q.getModelAnswer(), text, q.getMarks());
                    if (g != null) {
                        ans.setMarksAwarded(g.marks());
                        ans.setCorrect(g.marks() == q.getMarks());
                        ans.setFeedback(g.feedback());
                    }
                    // g == null  ->  marks stay null: waits for the teacher
                }
            }
            attempt.getAnswers().add(ans);
        }

        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setTotalMarks(guardService.paperTotal(attempt));
        recalc(attempt);
        attempt = attemptRepository.save(attempt);
        guardService.markFinal(attempt.getId());

        notifyAfterSubmit(attempt);
        return attempt;
    }

    /** score = sum of awarded marks; status depends on whether any answer still waits for the teacher */
    private void recalc(ExamAttempt attempt) {
        int score = 0;
        boolean pending = false;
        for (ExamAnswer a : attempt.getAnswers()) {
            if (a.getMarksAwarded() == null) pending = true;
            else score += a.getMarksAwarded();
        }
        attempt.setScore(score);
        attempt.setStatus(pending ? AttemptStatus.PENDING_REVIEW : AttemptStatus.SUBMITTED);
    }

    private void notifyAfterSubmit(ExamAttempt a) {
        String exam = a.getExam().getTitle() + " (" + a.getExam().getSubject().getSubjectName() + ")";
        User student = a.getStudent();

        String result = a.isPending()
                ? "submitted. Written answers are waiting for teacher marking."
                : "scored " + a.getScore() + "/" + a.getTotalMarks() + " (" + a.getPercentage() + "%, grade " + a.getGrade() + ").";

        notificationService.sendNotification(student, "You " + result.replace("submitted.", "submitted " + exam + ".")
                .replace("scored", "scored in " + exam + ":"));

        for (User p : studentParentRepository.findParentsByStudent(student.getId())) {
            notificationService.sendNotification(p,
                    "Your child " + student.getFullName() + " " + result.replace("submitted.", "submitted " + exam + ".")
                            .replace("scored", "scored in " + exam + ":"));
        }
        notificationService.sendNotification(a.getExam().getCreatedBy(),
                student.getFullName() + " submitted " + exam
                        + (a.isPending() ? " - please review the written answers." : " - " + a.getPercentage() + "%."));
    }

    // =====================================================================
    // helpers
    // =====================================================================
    private Exam editableExam(Long examId, Long teacherId) {
        Exam e = getTeacherExam(examId, teacherId);
        if (e.getStatus() != ExamStatus.DRAFT) {
            throw new IllegalArgumentException("A published exam cannot be changed");
        }
        return e;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String cut(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
