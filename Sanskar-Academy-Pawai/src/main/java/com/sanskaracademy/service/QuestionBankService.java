package com.sanskaracademy.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.BankQuestion;
import com.sanskaracademy.entity.Exam;
import com.sanskaracademy.entity.ExamQuestion;
import com.sanskaracademy.entity.QuestionType;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.BankQuestionRepository;
import com.sanskaracademy.repository.SubjectRepository;

@Service
public class QuestionBankService {

    private final BankQuestionRepository repo;
    private final SubjectRepository subjects;
    private final ExamService exams;

    public QuestionBankService(BankQuestionRepository repo, SubjectRepository subjects, ExamService exams) {
        this.repo = repo;
        this.subjects = subjects;
        this.exams = exams;
    }

    @Transactional(readOnly = true)
    public List<BankQuestion> ofTeacher(Long teacherId) {
        return repo.findByOwnerIdOrderByCreatedAtDesc(teacherId);
    }

    @Transactional
    public BankQuestion add(User teacher, Long subjectId, String chapter, QuestionType type, String text,
                            String a, String b, String c, String d, String correct, String model,
                            int marks, String difficulty) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Please write the question");
        BankQuestion q = new BankQuestion();
        q.setOwner(teacher);
        if (subjectId != null) {
            Subject s = subjects.findById(subjectId).orElse(null);
            if (s != null && s.getTeacher() != null && s.getTeacher().getId().equals(teacher.getId())) q.setSubject(s);
        }
        q.setChapter(clip(chapter, 120));
        q.setType(type == null ? QuestionType.MCQ : type);
        q.setQuestionText(clip(text, 1000));
        if (q.getType() == QuestionType.MCQ) {
            if (blank(a) || blank(b)) throw new IllegalArgumentException("Give at least options A and B");
            String cr = correct == null ? "" : correct.trim().toUpperCase();
            if (!cr.matches("[A-D]")) throw new IllegalArgumentException("Choose the correct option (A-D)");
            q.setOptionA(clip(a, 300));
            q.setOptionB(clip(b, 300));
            q.setOptionC(clip(c, 300));
            q.setOptionD(clip(d, 300));
            q.setCorrectOption(cr);
        } else {
            q.setModelAnswer(clip(model, 1000));
        }
        q.setMarks(marks <= 0 ? 1 : Math.min(marks, 100));
        q.setDifficulty(blank(difficulty) ? "MEDIUM" : clip(difficulty, 20));
        q.setCreatedAt(LocalDateTime.now());
        return repo.save(q);
    }

    @Transactional
    public void delete(Long id, Long teacherId) {
        BankQuestion q = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Question not found"));
        if (!q.getOwner().getId().equals(teacherId)) throw new IllegalArgumentException("This question belongs to another teacher");
        repo.delete(q);
    }

    /** copy bank questions into a draft exam through the normal ExamService (same rules as typing them) */
    @Transactional
    public int addToExam(Long examId, User teacher, List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("Tick at least one question");
        int n = 0;
        for (Long id : ids) {
            BankQuestion q = repo.findById(id).orElse(null);
            if (q == null || !q.getOwner().getId().equals(teacher.getId())) continue;
            exams.addQuestion(examId, teacher.getId(), q.getType(), q.getQuestionText(), q.getOptionA(), q.getOptionB(),
                    q.getOptionC(), q.getOptionD(), q.getCorrectOption(), q.getModelAnswer(), q.getMarks());
            q.setTimesUsed(q.getTimesUsed() + 1);
            repo.save(q);
            n++;
        }
        return n;
    }

    /** save every question of an existing exam into the bank (skips ones already saved with the same text) */
    @Transactional
    public int importFromExam(Long examId, User teacher) {
        Exam ex = exams.getTeacherExam(examId, teacher.getId());
        int n = 0;
        for (ExamQuestion eq : ex.getQuestions()) {
            boolean dup = false;
            for (BankQuestion b : repo.findByOwnerIdOrderByCreatedAtDesc(teacher.getId()))
                if (b.getQuestionText().equals(eq.getQuestionText())) { dup = true; break; }
            if (dup) continue;
            BankQuestion q = new BankQuestion();
            q.setOwner(teacher);
            q.setSubject(ex.getSubject());
            q.setChapter(null);
            q.setType(eq.getType());
            q.setQuestionText(eq.getQuestionText());
            q.setOptionA(eq.getOptionA());
            q.setOptionB(eq.getOptionB());
            q.setOptionC(eq.getOptionC());
            q.setOptionD(eq.getOptionD());
            q.setCorrectOption(eq.getCorrectOption());
            q.setModelAnswer(eq.getModelAnswer());
            q.setMarks(eq.getMarks());
            q.setDifficulty("MEDIUM");
            q.setCreatedAt(LocalDateTime.now());
            repo.save(q);
            n++;
        }
        return n;
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private static String clip(String s, int n) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        return s.length() > n ? s.substring(0, n) : s;
    }
}
