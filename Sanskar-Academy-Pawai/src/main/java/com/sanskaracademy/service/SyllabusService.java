package com.sanskaracademy.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.Chapter;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.ChapterRepository;
import com.sanskaracademy.repository.SubjectRepository;

@Service
public class SyllabusService {

    private final ChapterRepository repo;
    private final SubjectRepository subjects;
    private final SchoolAccess access;

    public SyllabusService(ChapterRepository repo, SubjectRepository subjects, SchoolAccess access) {
        this.repo = repo;
        this.subjects = subjects;
        this.access = access;
    }

    /** one subject with its chapters and a percent */
    public static class SubjectProgress {
        private final Subject subject;
        private final List<Chapter> chapters;
        private final int done;
        SubjectProgress(Subject s, List<Chapter> c) {
            this.subject = s;
            this.chapters = c;
            int d = 0;
            for (Chapter x : c) if (x.getCompletedOn() != null) d++;
            this.done = d;
        }
        public Subject getSubject() { return subject; }
        public List<Chapter> getChapters() { return chapters; }
        public int getDone() { return done; }
        public int getTotal() { return chapters.size(); }
        public int getPercent() { return chapters.isEmpty() ? 0 : done * 100 / chapters.size(); }
        public int getBehind() {
            int n = 0;
            for (Chapter x : chapters) if (x.getCompletedOn() == null && x.getPlannedDate() != null && x.getPlannedDate().isBefore(LocalDate.now())) n++;
            return n;
        }
    }

    @Transactional(readOnly = true)
    public List<SubjectProgress> forTeacher(Long teacherId) {
        List<SubjectProgress> out = new ArrayList<>();
        for (Subject s : access.subjectsOfTeacher(teacherId)) out.add(new SubjectProgress(s, repo.findBySubjectIdOrderByOrderNoAscIdAsc(s.getId())));
        return out;
    }

    @Transactional(readOnly = true)
    public List<SubjectProgress> forClass(Long classId) {
        List<SubjectProgress> out = new ArrayList<>();
        if (classId == null) return out;
        for (Subject s : access.subjectsOfClass(classId)) out.add(new SubjectProgress(s, repo.findBySubjectIdOrderByOrderNoAscIdAsc(s.getId())));
        return out;
    }

    @Transactional(readOnly = true)
    public List<SubjectProgress> all() {
        List<SubjectProgress> out = new ArrayList<>();
        for (Subject s : subjects.findAll()) out.add(new SubjectProgress(s, repo.findBySubjectIdOrderByOrderNoAscIdAsc(s.getId())));
        return out;
    }

    private Subject mine(Long subjectId, Long teacherId) {
        Subject s = subjects.findById(subjectId).orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        if (s.getTeacher() == null || !s.getTeacher().getId().equals(teacherId))
            throw new IllegalArgumentException("This is not your subject");
        return s;
    }

    private Chapter mineChapter(Long id, Long teacherId) {
        Chapter c = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Chapter not found"));
        mine(c.getSubject().getId(), teacherId);
        return c;
    }

    /** chapter titles, one per line */
    @Transactional
    public int addChapters(Long subjectId, Long teacherId, String lines) {
        Subject s = mine(subjectId, teacherId);
        int next = repo.countBySubjectId(subjectId) + 1;
        int added = 0;
        if (lines == null) return 0;
        for (String line : lines.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            Chapter c = new Chapter();
            c.setSubject(s);
            c.setTitle(t.length() > 200 ? t.substring(0, 200) : t);
            c.setOrderNo(next++);
            repo.save(c);
            added++;
        }
        return added;
    }

    @Transactional
    public void plan(Long chapterId, Long teacherId, LocalDate planned) {
        Chapter c = mineChapter(chapterId, teacherId);
        c.setPlannedDate(planned);
        repo.save(c);
    }

    @Transactional
    public void complete(Long chapterId, User teacher, String note) {
        Chapter c = mineChapter(chapterId, teacher.getId());
        c.setCompletedOn(LocalDate.now());
        c.setCompletedBy(teacher);
        c.setNote(note == null || note.isBlank() ? null : (note.trim().length() > 250 ? note.trim().substring(0, 250) : note.trim()));
        repo.save(c);
    }

    @Transactional
    public void reopen(Long chapterId, Long teacherId) {
        Chapter c = mineChapter(chapterId, teacherId);
        c.setCompletedOn(null);
        c.setCompletedBy(null);
        repo.save(c);
    }

    @Transactional
    public void delete(Long chapterId, Long teacherId) {
        repo.delete(mineChapter(chapterId, teacherId));
    }
}
