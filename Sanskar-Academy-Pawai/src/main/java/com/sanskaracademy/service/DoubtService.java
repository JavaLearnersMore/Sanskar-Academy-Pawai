package com.sanskaracademy.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.Doubt;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.DoubtRepository;
import com.sanskaracademy.repository.SubjectRepository;

@Service
public class DoubtService {

    private final DoubtRepository repo;
    private final SubjectRepository subjects;
    private final SchoolAccess access;
    private final AiTutorService ai;

    public DoubtService(DoubtRepository repo, SubjectRepository subjects, SchoolAccess access, AiTutorService ai) {
        this.repo = repo;
        this.subjects = subjects;
        this.access = access;
        this.ai = ai;
    }

    @Transactional
    public Doubt ask(User student, Long subjectId, String question) {
        Subject s = subjects.findById(subjectId).orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        Long classId = access.classOf(student.getId());
        if (classId == null || !classId.equals(s.getClassEntity().getId()))
            throw new IllegalArgumentException("This subject is not in your class");
        if (question == null || question.trim().length() < 5) throw new IllegalArgumentException("Please write your doubt (at least 5 letters)");
        Doubt d = new Doubt();
        d.setStudent(student);
        d.setSubject(s);
        d.setQuestion(question.trim().length() > 1500 ? question.trim().substring(0, 1500) : question.trim());
        d.setAskedAt(LocalDateTime.now());
        repo.save(d);
        access.tell(s.getTeacher(), student.getFullName() + " asked a doubt in " + s.getSubjectName());
        return d;
    }

    @Transactional(readOnly = true)
    public List<Doubt> ofStudent(Long studentId) {
        return repo.findByStudentIdOrderByAskedAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public List<Doubt> ofTeacher(Long teacherId) {
        return repo.findBySubjectTeacherIdOrderByAskedAtDesc(teacherId);
    }

    @Transactional(readOnly = true)
    public long openCount(Long teacherId) {
        return repo.countBySubjectTeacherIdAndAnswerIsNull(teacherId);
    }

    private Doubt ownedBy(Long id, Long teacherId) {
        Doubt d = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Doubt not found"));
        if (d.getSubject().getTeacher() == null || !d.getSubject().getTeacher().getId().equals(teacherId))
            throw new IllegalArgumentException("This doubt is for another teacher");
        return d;
    }

    @Transactional
    public void answer(Long id, User teacher, String answer, boolean aiAssisted) {
        Doubt d = ownedBy(id, teacher.getId());
        if (answer == null || answer.isBlank()) throw new IllegalArgumentException("Please write the answer");
        d.setAnswer(answer.trim().length() > 3000 ? answer.trim().substring(0, 3000) : answer.trim());
        d.setAnsweredBy(teacher);
        d.setAnsweredAt(LocalDateTime.now());
        d.setAiAssisted(aiAssisted);
        repo.save(d);
        access.tell(d.getStudent(), "Your doubt in " + d.getSubject().getSubjectName() + " was answered by " + teacher.getFullName());
    }

    /** A draft the teacher can edit before sending. Returns null when AI is off or fails. */
    @Transactional(readOnly = true)
    public String aiDraft(Long id, User teacher) {
        Doubt d = ownedBy(id, teacher.getId());
        if (!ai.isEnabled()) return null;
        try {
            return ai.answerDoubt(teacher.getId(), d.getSubject().getClassEntity().getClassName(),
                    d.getSubject().getSubjectName(), null, null, d.getQuestion());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
