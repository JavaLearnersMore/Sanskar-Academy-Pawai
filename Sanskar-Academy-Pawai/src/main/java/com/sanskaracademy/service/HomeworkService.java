package com.sanskaracademy.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sanskaracademy.entity.Homework;
import com.sanskaracademy.entity.HomeworkSubmission;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.HomeworkRepository;
import com.sanskaracademy.repository.HomeworkSubmissionRepository;
import com.sanskaracademy.repository.SubjectRepository;

@Service
public class HomeworkService {

    private final HomeworkRepository homeworks;
    private final HomeworkSubmissionRepository submissions;
    private final SubjectRepository subjects;
    private final SchoolAccess access;
    private final FileStoreService files;

    public HomeworkService(HomeworkRepository homeworks, HomeworkSubmissionRepository submissions,
                           SubjectRepository subjects, SchoolAccess access, FileStoreService files) {
        this.homeworks = homeworks;
        this.submissions = submissions;
        this.subjects = subjects;
        this.access = access;
        this.files = files;
    }

    /** one line of the student / parent list */
    public static class StudentRow {
        private final Homework homework;
        private final HomeworkSubmission submission;
        private final boolean overdue;
        StudentRow(Homework h, HomeworkSubmission s) {
            this.homework = h;
            this.submission = s;
            this.overdue = s == null && h.getDueDate().isBefore(LocalDate.now());
        }
        public Homework getHomework() { return homework; }
        public HomeworkSubmission getSubmission() { return submission; }
        public boolean isOverdue() { return overdue; }
        public boolean isSubmitted() { return submission != null; }
        public boolean isChecked() { return submission != null && submission.getCheckedAt() != null; }
        public String getStatus() {
            if (submission == null) return overdue ? "OVERDUE" : "PENDING";
            if (submission.getCheckedAt() != null) return "CHECKED";
            return submission.isLate() ? "LATE" : "SUBMITTED";
        }
        public String getBadge() {
            switch (getStatus()) {
                case "OVERDUE": return "bg-danger";
                case "PENDING": return "bg-warning text-dark";
                case "CHECKED": return "bg-success";
                case "LATE": return "bg-secondary";
                default: return "bg-primary";
            }
        }
    }

    // ------------------------------------------------------------------ teacher
    @Transactional
    public Homework create(User teacher, Long subjectId, String title, String description, LocalDate due,
                           Integer maxMarks, MultipartFile file) throws IOException {
        Subject s = subjects.findById(subjectId).orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        if (s.getTeacher() == null || !s.getTeacher().getId().equals(teacher.getId()))
            throw new IllegalArgumentException("You can only give homework for your own subjects");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Please write a title");
        if (due == null) throw new IllegalArgumentException("Please choose a due date");
        Homework h = new Homework();
        h.setSubject(s);
        h.setTitle(clip(title, 200));
        h.setDescription(clip(description, 2000));
        h.setDueDate(due);
        h.setMaxMarks(maxMarks == null || maxMarks <= 0 ? null : maxMarks);
        h.setCreatedBy(teacher);
        h.setCreatedAt(LocalDateTime.now());
        FileStoreService.Stored st = files.save(file, "homework");
        if (st != null) {
            h.setAttachmentPath(st.getPath());
            h.setAttachmentName(st.getName());
        }
        homeworks.save(h);
        List<User> kids = access.studentsOfClass(s.getClassEntity().getId());
        String msg = "New homework in " + s.getSubjectName() + ": " + h.getTitle() + " (due " + due + ")";
        access.tell(kids, msg);
        access.tell(access.parentsOf(kids), msg);
        return h;
    }

    @Transactional(readOnly = true)
    public List<Homework> ofTeacher(Long teacherId) {
        return homeworks.findBySubjectTeacherIdOrderByCreatedAtDesc(teacherId);
    }

    @Transactional(readOnly = true)
    public Map<Long, long[]> countsFor(List<Homework> list) {
        Map<Long, long[]> out = new HashMap<>();
        for (Homework h : list) {
            int total = access.studentsOfClass(h.getSubject().getClassEntity().getId()).size();
            out.put(h.getId(), new long[] { submissions.countByHomeworkId(h.getId()), total });
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Homework ownedBy(Long id, Long teacherId) {
        Homework h = homeworks.findById(id).orElseThrow(() -> new IllegalArgumentException("Homework not found"));
        if (h.getSubject().getTeacher() == null || !h.getSubject().getTeacher().getId().equals(teacherId))
            throw new IllegalArgumentException("This homework belongs to another teacher");
        return h;
    }

    /** one row per student of the class: who submitted, who did not */
    public static class ClassRow {
        private final User student;
        private final HomeworkSubmission submission;
        ClassRow(User u, HomeworkSubmission s) {
            this.student = u;
            this.submission = s;
        }
        public User getStudent() { return student; }
        public HomeworkSubmission getSubmission() { return submission; }
    }

    @Transactional(readOnly = true)
    public List<ClassRow> classRows(Homework h) {
        Map<Long, HomeworkSubmission> by = new HashMap<>();
        for (HomeworkSubmission s : submissions.findByHomeworkId(h.getId())) by.put(s.getStudent().getId(), s);
        List<ClassRow> out = new ArrayList<>();
        for (User u : access.studentsOfClass(h.getSubject().getClassEntity().getId()))
            out.add(new ClassRow(u, by.get(u.getId())));
        return out;
    }

    @Transactional
    public void check(Long submissionId, Long teacherId, Integer marks, String remark) {
        HomeworkSubmission s = submissions.findById(submissionId).orElseThrow(() -> new IllegalArgumentException("Submission not found"));
        Homework h = ownedBy(s.getHomework().getId(), teacherId);
        if (marks != null) {
            if (marks < 0) throw new IllegalArgumentException("Marks cannot be negative");
            if (h.getMaxMarks() != null && marks > h.getMaxMarks()) throw new IllegalArgumentException("Marks are more than the maximum (" + h.getMaxMarks() + ")");
        }
        s.setMarks(marks);
        s.setRemark(clip(remark, 250));
        s.setCheckedAt(LocalDateTime.now());
        submissions.save(s);
        access.tell(s.getStudent(), "Your homework \"" + h.getTitle() + "\" was checked"
                + (marks != null ? ": " + marks + (h.getMaxMarks() != null ? "/" + h.getMaxMarks() : "") : "."));
    }

    @Transactional
    public void delete(Long id, Long teacherId) {
        Homework h = ownedBy(id, teacherId);
        List<HomeworkSubmission> subs = submissions.findByHomeworkId(id);
        for (HomeworkSubmission s : subs) files.delete(s.getFilePath());
        submissions.deleteAll(subs);
        files.delete(h.getAttachmentPath());
        homeworks.delete(h);
    }

    // ------------------------------------------------------------------ student / parent
    @Transactional(readOnly = true)
    public List<StudentRow> forStudent(Long studentId) {
        Long classId = access.classOf(studentId);
        List<StudentRow> out = new ArrayList<>();
        if (classId == null) return out;
        List<Long> subjectIds = new ArrayList<>();
        for (Subject s : access.subjectsOfClass(classId)) subjectIds.add(s.getId());
        if (subjectIds.isEmpty()) return out;
        Map<Long, HomeworkSubmission> mine = new HashMap<>();
        for (HomeworkSubmission s : submissions.findByStudentId(studentId)) mine.put(s.getHomework().getId(), s);
        for (Homework h : homeworks.findBySubjectIdInOrderByDueDateDescIdDesc(subjectIds))
            out.add(new StudentRow(h, mine.get(h.getId())));
        return out;
    }

    @Transactional
    public void submit(User student, Long homeworkId, String text, MultipartFile file) throws IOException {
        Homework h = homeworks.findById(homeworkId).orElseThrow(() -> new IllegalArgumentException("Homework not found"));
        Long classId = access.classOf(student.getId());
        if (classId == null || !classId.equals(h.getSubject().getClassEntity().getId()))
            throw new IllegalArgumentException("This homework is not for your class");
        HomeworkSubmission s = submissions.findByHomeworkIdAndStudentId(homeworkId, student.getId()).orElse(null);
        if (s != null && s.getCheckedAt() != null) throw new IllegalArgumentException("Your teacher already checked this homework");
        boolean noText = text == null || text.isBlank();
        boolean noFile = file == null || file.isEmpty();
        if (noText && noFile && (s == null || (s.getFilePath() == null && s.getAnswerText() == null)))
            throw new IllegalArgumentException("Write an answer or attach a file");
        if (s == null) {
            s = new HomeworkSubmission();
            s.setHomework(h);
            s.setStudent(student);
        }
        FileStoreService.Stored st = files.save(file, "submissions");
        if (st != null) {
            files.delete(s.getFilePath());
            s.setFilePath(st.getPath());
            s.setFileName(st.getName());
        }
        if (!noText) s.setAnswerText(clip(text, 2000));
        s.setSubmittedAt(LocalDateTime.now());
        s.setLate(LocalDate.now().isAfter(h.getDueDate()));
        submissions.save(s);
        access.tell(h.getCreatedBy(), student.getFullName() + " submitted homework: " + h.getTitle());
    }

    /** for downloads */
    @Transactional(readOnly = true)
    public Homework find(Long id) {
        return homeworks.findById(id).orElseThrow(() -> new IllegalArgumentException("Homework not found"));
    }

    @Transactional(readOnly = true)
    public HomeworkSubmission findSubmission(Long id) {
        return submissions.findById(id).orElseThrow(() -> new IllegalArgumentException("Submission not found"));
    }

    private static String clip(String s, int n) {
        if (s == null) return null;
        s = s.trim();
        return s.length() > n ? s.substring(0, n) : s;
    }
}
