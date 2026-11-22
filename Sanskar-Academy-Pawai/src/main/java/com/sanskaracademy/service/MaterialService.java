package com.sanskaracademy.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sanskaracademy.entity.StudyMaterial;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.StudyMaterialRepository;
import com.sanskaracademy.repository.SubjectRepository;

@Service
public class MaterialService {

    private final StudyMaterialRepository repo;
    private final SubjectRepository subjects;
    private final SchoolAccess access;
    private final FileStoreService files;

    public MaterialService(StudyMaterialRepository repo, SubjectRepository subjects, SchoolAccess access, FileStoreService files) {
        this.repo = repo;
        this.subjects = subjects;
        this.access = access;
        this.files = files;
    }

    @Transactional
    public StudyMaterial add(User teacher, Long subjectId, String chapter, String title, String description,
                             String link, MultipartFile file) throws IOException {
        Subject s = subjects.findById(subjectId).orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        if (s.getTeacher() == null || !s.getTeacher().getId().equals(teacher.getId()))
            throw new IllegalArgumentException("You can only add material for your own subjects");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Please write a title");
        boolean hasLink = link != null && !link.isBlank();
        if (hasLink) {
            String l = link.trim().toLowerCase();
            if (!(l.startsWith("https://") || l.startsWith("http://"))) throw new IllegalArgumentException("The link must start with https://");
        }
        FileStoreService.Stored st = files.save(file, "material");
        if (!hasLink && st == null) throw new IllegalArgumentException("Attach a file or paste a link");
        StudyMaterial m = new StudyMaterial();
        m.setSubject(s);
        m.setChapter(clip(chapter, 120));
        m.setTitle(clip(title, 200));
        m.setDescription(clip(description, 500));
        m.setLink(hasLink ? clip(link, 500) : null);
        if (st != null) {
            m.setFilePath(st.getPath());
            m.setFileName(st.getName());
        }
        m.setUploadedBy(teacher);
        m.setUploadedAt(LocalDateTime.now());
        repo.save(m);
        access.tell(access.studentsOfClass(s.getClassEntity().getId()),
                "New study material in " + s.getSubjectName() + ": " + m.getTitle());
        return m;
    }

    @Transactional(readOnly = true)
    public List<StudyMaterial> ofTeacher(Long teacherId) {
        return repo.findBySubjectTeacherIdOrderByUploadedAtDesc(teacherId);
    }

    @Transactional(readOnly = true)
    public List<StudyMaterial> forStudent(Long studentId) {
        Long classId = access.classOf(studentId);
        return classId == null ? new ArrayList<>()
                : repo.findBySubjectClassEntityIdOrderBySubjectSubjectNameAscChapterAscUploadedAtDesc(classId);
    }

    @Transactional
    public void delete(Long id, Long teacherId) {
        StudyMaterial m = find(id);
        if (m.getSubject().getTeacher() == null || !m.getSubject().getTeacher().getId().equals(teacherId))
            throw new IllegalArgumentException("This material belongs to another teacher");
        files.delete(m.getFilePath());
        repo.delete(m);
    }

    @Transactional(readOnly = true)
    public StudyMaterial find(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Material not found"));
    }

    private static String clip(String s, int n) {
        if (s == null) return null;
        s = s.trim();
        return s.length() > n ? s.substring(0, n) : s;
    }
}
