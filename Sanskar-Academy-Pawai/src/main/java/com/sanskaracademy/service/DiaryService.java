package com.sanskaracademy.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.DiaryCategory;
import com.sanskaracademy.entity.DiaryEntry;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.DiaryEntryRepository;
import com.sanskaracademy.repository.UserRepository;

@Service
public class DiaryService {

    private final DiaryEntryRepository repo;
    private final UserRepository users;
    private final SchoolAccess access;

    public DiaryService(DiaryEntryRepository repo, UserRepository users, SchoolAccess access) {
        this.repo = repo;
        this.users = users;
        this.access = access;
    }

    @Transactional
    public int add(User teacher, List<Long> studentIds, DiaryCategory cat, String text, boolean visible) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Please write the remark");
        if (studentIds == null || studentIds.isEmpty()) throw new IllegalArgumentException("Choose at least one student");
        String t = text.trim().length() > 500 ? text.trim().substring(0, 500) : text.trim();
        int n = 0;
        for (Long id : studentIds) {
            User st = users.findById(id).orElse(null);
            if (st == null || !access.teacherTeachesClass(teacher.getId(), access.classOf(id))) continue;
            DiaryEntry e = new DiaryEntry();
            e.setStudent(st);
            e.setTeacher(teacher);
            e.setCategory(cat == null ? DiaryCategory.NOTE : cat);
            e.setText(t);
            e.setCreatedAt(LocalDateTime.now());
            e.setVisibleToParent(visible);
            repo.save(e);
            n++;
            String msg = "Class diary from " + teacher.getFullName() + ": " + t;
            access.tell(st, msg);
            if (visible) access.tell(access.parentsOf(List.of(st)), "About " + st.getFullName() + " - " + msg);
        }
        return n;
    }

    @Transactional(readOnly = true)
    public List<DiaryEntry> recentOfTeacher(Long teacherId) {
        return repo.findTop100ByTeacherIdOrderByCreatedAtDesc(teacherId);
    }

    @Transactional(readOnly = true)
    public List<DiaryEntry> forStudent(Long studentId) {
        return repo.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    /** parents only see entries the teacher made visible */
    @Transactional(readOnly = true)
    public List<DiaryEntry> forParentView(Long studentId) {
        List<DiaryEntry> out = new ArrayList<>();
        for (DiaryEntry e : repo.findByStudentIdOrderByCreatedAtDesc(studentId)) if (e.isVisibleToParent()) out.add(e);
        return out;
    }

    @Transactional
    public void delete(Long id, Long teacherId) {
        DiaryEntry e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Entry not found"));
        if (!e.getTeacher().getId().equals(teacherId)) throw new IllegalArgumentException("This entry is by another teacher");
        repo.delete(e);
    }
}
