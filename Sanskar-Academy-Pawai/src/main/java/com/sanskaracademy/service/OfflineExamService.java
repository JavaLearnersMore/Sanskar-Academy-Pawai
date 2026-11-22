package com.sanskaracademy.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sanskaracademy.entity.AttemptStatus;
import com.sanskaracademy.entity.Exam;
import com.sanskaracademy.entity.ExamAttempt;
import com.sanskaracademy.entity.ExamStatus;
import com.sanskaracademy.entity.OfflineExam;
import com.sanskaracademy.entity.OfflineMark;
import com.sanskaracademy.entity.OfflineSheet;
import com.sanskaracademy.entity.StudentEnrollment;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.ExamAttemptRepository;
import com.sanskaracademy.repository.ExamRepository;
import com.sanskaracademy.repository.OfflineExamRepository;
import com.sanskaracademy.repository.OfflineMarkRepository;
import com.sanskaracademy.repository.OfflineSheetRepository;
import com.sanskaracademy.repository.StudentEnrollmentRepository;
import com.sanskaracademy.repository.StudentParentRepository;

/**
 * Paper (offline) exams: the teacher prints the question paper, checks the papers by hand,
 * types each student's marks, uploads photos / scans of the answer sheets, then publishes.
 * Before publishing nothing is visible to students or parents.
 */
@Service("offlineExamService")
public class OfflineExamService {

    private static final long MAX_FILE = 8L * 1024 * 1024;
    private static final int MAX_SHEETS_PER_STUDENT = 12;

    private final ExamService examService;
    private final ExamRepository examRepo;
    private final ExamAttemptRepository attemptRepo;
    private final OfflineExamRepository offlineRepo;
    private final OfflineMarkRepository markRepo;
    private final OfflineSheetRepository sheetRepo;
    private final StudentEnrollmentRepository enrollRepo;
    private final StudentParentRepository parentRepo;
    private final AcademicService academicService;
    private final NotificationService inApp;
    private final NotificationClient messages;
    private final Path root;

    public OfflineExamService(ExamService examService, ExamRepository examRepo, ExamAttemptRepository attemptRepo,
                              OfflineExamRepository offlineRepo, OfflineMarkRepository markRepo,
                              OfflineSheetRepository sheetRepo, StudentEnrollmentRepository enrollRepo,
                              StudentParentRepository parentRepo, AcademicService academicService,
                              NotificationService inApp, NotificationClient messages,
                              @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.examService = examService;
        this.examRepo = examRepo;
        this.attemptRepo = attemptRepo;
        this.offlineRepo = offlineRepo;
        this.markRepo = markRepo;
        this.sheetRepo = sheetRepo;
        this.enrollRepo = enrollRepo;
        this.parentRepo = parentRepo;
        this.academicService = academicService;
        this.inApp = inApp;
        this.messages = messages;
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("answer-sheets");
    }

    // ---------------------------------------------------------------- basics
    @Transactional(readOnly = true)
    public boolean isOffline(Long examId) {
        return examId != null && offlineRepo.existsByExamId(examId);
    }

    @Transactional(readOnly = true)
    public OfflineExam infoOf(Long examId) {
        return offlineRepo.findByExamId(examId).orElse(null);
    }

    @Transactional
    public Exam create(User teacher, Long subjectId, String title, String description,
                       int durationMinutes, LocalDate examDate) {
        Exam e = examService.createExam(teacher, subjectId, title, description, durationMinutes, null);
        OfflineExam o = new OfflineExam();
        o.setExam(e);
        o.setExamDate(examDate);
        o.setResultsPublished(false);
        offlineRepo.save(o);
        return e;
    }

    private OfflineExam requireOffline(Long examId, Long teacherId) {
        examService.getTeacherExam(examId, teacherId);          // also checks that the exam is this teacher's
        return offlineRepo.findByExamId(examId)
                .orElseThrow(() -> new IllegalArgumentException("This is not an offline (paper) exam"));
    }

    // ---------------------------------------------------------------- marks sheet
    /** one line of the marks page */
    public static class Row {
        private final User student;
        private final Integer marks;
        private final String remarks;
        private final List<OfflineSheet> sheets;
        Row(User student, Integer marks, String remarks, List<OfflineSheet> sheets) {
            this.student = student;
            this.marks = marks;
            this.remarks = remarks;
            this.sheets = sheets;
        }
        public User getStudent() { return student; }
        public Integer getMarks() { return marks; }
        public String getRemarks() { return remarks; }
        public List<OfflineSheet> getSheets() { return sheets; }
    }

    @Transactional(readOnly = true)
    public List<Row> rows(Long examId, Long teacherId) {
        Exam exam = examService.getTeacherExam(examId, teacherId);
        requireOffline(examId, teacherId);
        Map<Long, OfflineMark> marks = new HashMap<>();
        for (OfflineMark m : markRepo.findByExamId(examId)) marks.put(m.getStudent().getId(), m);
        Map<Long, List<OfflineSheet>> sheets = new HashMap<>();
        for (OfflineSheet s : sheetRepo.findByMarkExamId(examId)) {
            sheets.computeIfAbsent(s.getMark().getStudent().getId(), k -> new ArrayList<>()).add(s);
        }
        List<Row> out = new ArrayList<>();
        for (StudentEnrollment en : enrollRepo.findByClassEntityId(exam.getSubject().getClassEntity().getId())) {
            User st = en.getStudent();
            OfflineMark m = marks.get(st.getId());
            out.add(new Row(st, m == null ? null : m.getMarks(), m == null ? null : m.getRemarks(),
                    sheets.getOrDefault(st.getId(), List.of())));
        }
        out.sort(Comparator.comparing(r -> r.getStudent().getFullName() == null ? "" : r.getStudent().getFullName().toLowerCase(Locale.ROOT)));
        return out;
    }

    private void requireClassStudent(Exam exam, Long studentId) {
        StudentEnrollment en = enrollRepo.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found in a class"));
        if (!en.getClassEntity().getId().equals(exam.getSubject().getClassEntity().getId()))
            throw new IllegalArgumentException("This student is not in the class of this exam");
    }

    private OfflineMark markOf(Exam exam, Long studentId) {
        return markRepo.findByExamIdAndStudentId(exam.getId(), studentId).orElseGet(() -> {
            OfflineMark m = new OfflineMark();
            m.setExam(exam);
            m.setStudent(enrollRepo.findByStudentId(studentId).orElseThrow().getStudent());
            return m;
        });
    }

    /** Saves marks + remarks of one student and adds any uploaded answer-sheet files. */
    @Transactional
    public void saveStudent(Long examId, Long teacherId, Long studentId, Integer marks, String remarks,
                            List<MultipartFile> files) throws IOException {
        Exam exam = examService.getTeacherExam(examId, teacherId);
        OfflineExam info = requireOffline(examId, teacherId);
        requireClassStudent(exam, studentId);
        int total = exam.getTotalMarks();
        if (marks != null) {
            if (total <= 0) throw new IllegalArgumentException("Add the questions first - the total marks come from the paper");
            if (marks < 0 || marks > total) throw new IllegalArgumentException("Marks must be between 0 and " + total);
        }
        OfflineMark m = markOf(exam, studentId);
        m.setMarks(marks);
        m.setRemarks(remarks == null || remarks.isBlank() ? null : (remarks.trim().length() > 290 ? remarks.trim().substring(0, 290) : remarks.trim()));
        m.setUpdatedAt(LocalDateTime.now());
        m = markRepo.save(m);

        if (files != null) {
            int have = sheetRepo.findByMarkIdOrderByIdAsc(m.getId()).size();
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                if (have >= MAX_SHEETS_PER_STUDENT) throw new IllegalArgumentException("At most " + MAX_SHEETS_PER_STUDENT + " files per student");
                storeFile(exam, m, f);
                have++;
            }
        }
        if (info.isResultsPublished()) syncAttempt(m, exam);       // changed after publishing: keep results in step
    }

    private void storeFile(Exam exam, OfflineMark m, MultipartFile f) throws IOException {
        if (f.getSize() > MAX_FILE) throw new IllegalArgumentException("Each file must be 8 MB or smaller");
        byte[] head;
        try (InputStream in = f.getInputStream()) {
            head = in.readNBytes(12);
        }
        String ext;
        String type;
        if (head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            ext = "jpg"; type = "image/jpeg";
        } else if (head.length >= 4 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            ext = "png"; type = "image/png";
        } else if (head.length >= 4 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F') {
            ext = "pdf"; type = "application/pdf";
        } else if (head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            ext = "webp"; type = "image/webp";
        } else {
            throw new IllegalArgumentException("Only JPG, PNG, WEBP photos or PDF files are allowed");
        }
        Path dir = root.resolve(String.valueOf(exam.getId())).resolve(String.valueOf(m.getStudent().getId())).normalize();
        if (!dir.startsWith(root)) throw new IllegalArgumentException("Bad path");
        Files.createDirectories(dir);
        String stored = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try (InputStream in = f.getInputStream()) {
            Files.copy(in, dir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        }
        OfflineSheet s = new OfflineSheet();
        s.setMark(m);
        s.setStoredName(stored);
        String on = f.getOriginalFilename();
        if (on != null) on = on.replaceAll("[^A-Za-z0-9._ -]", "_");
        s.setOriginalName(on == null ? null : (on.length() > 140 ? on.substring(on.length() - 140) : on));
        s.setContentType(type);
        s.setUploadedAt(LocalDateTime.now());
        sheetRepo.save(s);
    }

    @Transactional
    public void deleteSheet(Long sheetId, Long teacherId) {
        OfflineSheet s = sheetRepo.findById(sheetId).orElseThrow(() -> new IllegalArgumentException("File not found"));
        examService.getTeacherExam(s.getMark().getExam().getId(), teacherId);
        try {
            Files.deleteIfExists(pathOf(s));
        } catch (IOException ignore) {
            // the database row is removed anyway
        }
        sheetRepo.delete(s);
    }

    private Path pathOf(OfflineSheet s) {
        Path p = root.resolve(String.valueOf(s.getMark().getExam().getId()))
                .resolve(String.valueOf(s.getMark().getStudent().getId()))
                .resolve(s.getStoredName()).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("Bad path");
        return p;
    }

    // ---------------------------------------------------------------- publish
    @Transactional
    public int publish(Long examId, Long teacherId) {
        Exam exam = examService.getTeacherExam(examId, teacherId);
        OfflineExam info = requireOffline(examId, teacherId);
        List<OfflineMark> marks = new ArrayList<>();
        for (OfflineMark m : markRepo.findByExamId(examId)) if (m.getMarks() != null) marks.add(m);
        if (marks.isEmpty()) throw new IllegalArgumentException("Enter marks for at least one student first");
        boolean first = !info.isResultsPublished();
        info.setResultsPublished(true);
        info.setPublishedAt(LocalDateTime.now());
        offlineRepo.save(info);
        exam.setStatus(ExamStatus.CLOSED);
        examRepo.save(exam);
        int n = 0;
        for (OfflineMark m : marks) {
            ExamAttempt a = syncAttempt(m, exam);
            if (first) {
                tell(m.getStudent(), a, exam);
                n++;
            }
        }
        return n;
    }

    /** creates / updates the normal result record, so every existing result page works unchanged */
    private ExamAttempt syncAttempt(OfflineMark m, Exam exam) {
        ExamAttempt a = attemptRepo.findByExamIdAndStudentId(exam.getId(), m.getStudent().getId()).orElse(null);
        if (m.getMarks() == null) {
            if (a != null) attemptRepo.delete(a);
            return null;
        }
        if (a == null) {
            a = new ExamAttempt();
            a.setExam(exam);
            a.setStudent(m.getStudent());
            a.setStartedAt(LocalDateTime.now());
        }
        a.setScore(m.getMarks());
        a.setTotalMarks(exam.getTotalMarks());
        a.setStatus(AttemptStatus.SUBMITTED);
        a.setSubmittedAt(LocalDateTime.now());
        return attemptRepo.save(a);
    }

    private void tell(User student, ExamAttempt a, Exam exam) {
        String marks = a.getScore() + "/" + a.getTotalMarks();
        String text = "Result of " + exam.getTitle() + " (" + exam.getSubject().getSubjectName() + "): "
                + student.getFullName() + " scored " + marks + ".";
        inApp.sendNotification(student, text);
        Map<String, String> p = new HashMap<>();
        p.put("student", student.getFullName());
        p.put("exam", exam.getTitle());
        p.put("marks", marks);
        messages.sendAsync(student, "RESULT_READY", p, "RESULT-" + exam.getId() + "-" + student.getId());
        for (User parent : parentRepo.findParentsByStudent(student.getId())) {
            inApp.sendNotification(parent, text);
            messages.sendAsync(parent, "RESULT_READY", p, "RESULT-" + exam.getId() + "-" + student.getId());
        }
    }

    // ---------------------------------------------------------------- viewing
    /** sheets of one student for one exam, for the result pages (only once results are published) */
    @Transactional(readOnly = true)
    public List<OfflineSheet> sheetsFor(Long examId, Long studentId) {
        OfflineExam info = offlineRepo.findByExamId(examId).orElse(null);
        if (info == null || !info.isResultsPublished()) return List.of();
        return sheetRepo.findByMarkExamIdAndMarkStudentIdOrderByIdAsc(examId, studentId);
    }

    @Transactional(readOnly = true)
    public String remarksFor(Long examId, Long studentId) {
        return markRepo.findByExamIdAndStudentId(examId, studentId).map(OfflineMark::getRemarks).orElse(null);
    }

    public static class FileView {
        public final Path path;
        public final String contentType;
        public final String name;
        FileView(Path path, String contentType, String name) {
            this.path = path;
            this.contentType = contentType;
            this.name = name;
        }
    }

    /** Opens one sheet if (and only if) the viewer may see it. */
    @Transactional(readOnly = true)
    public FileView open(Long sheetId, User viewer) {
        OfflineSheet s = sheetRepo.findById(sheetId).orElseThrow(() -> new IllegalArgumentException("File not found"));
        Exam exam = s.getMark().getExam();
        Long studentId = s.getMark().getStudent().getId();
        String role = viewer.getRole().getName();
        boolean published = offlineRepo.findByExamId(exam.getId()).map(OfflineExam::isResultsPublished).orElse(false);
        boolean ok;
        switch (role) {
            case "ADMIN": ok = true; break;
            case "TEACHER": ok = exam.getSubject().getTeacher().getId().equals(viewer.getId()); break;
            case "STUDENT": ok = published && studentId.equals(viewer.getId()); break;
            case "PARENT": ok = published && academicService.isParentOf(viewer.getId(), studentId); break;
            default: ok = false;
        }
        if (!ok) throw new IllegalArgumentException("File not found");
        Path p = pathOf(s);
        if (!Files.isRegularFile(p)) throw new IllegalArgumentException("File not found");
        return new FileView(p, s.getContentType(), s.getOriginalName() == null ? s.getStoredName() : s.getOriginalName());
    }
}
