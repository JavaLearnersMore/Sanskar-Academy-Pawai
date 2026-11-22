package com.sanskaracademy.service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.StudentEnrollment;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.StudentEnrollmentRepository;
import com.sanskaracademy.repository.StudentParentRepository;
import com.sanskaracademy.repository.SubjectRepository;
import com.sanskaracademy.repository.UserRepository;

/** Small helper used by the new modules: who is who, which class, which parent, in-app messages. */
@Service
public class SchoolAccess {

    private final UserRepository users;
    private final StudentEnrollmentRepository enrollments;
    private final SubjectRepository subjects;
    private final StudentParentRepository parents;
    private final NotificationService notifier;

    public SchoolAccess(UserRepository users, StudentEnrollmentRepository enrollments, SubjectRepository subjects,
                        StudentParentRepository parents, NotificationService notifier) {
        this.users = users;
        this.enrollments = enrollments;
        this.subjects = subjects;
        this.parents = parents;
        this.notifier = notifier;
    }

    public User me(Principal p) {
        return users.findByUsername(p.getName()).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public static String role(User u) {
        return u.getRole() == null || u.getRole().getName() == null ? "" : u.getRole().getName().replace("ROLE_", "");
    }

    @Transactional(readOnly = true)
    public Long classOf(Long studentId) {
        return enrollments.findByStudentId(studentId).map(e -> e.getClassEntity().getId()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<User> studentsOfClass(Long classId) {
        List<User> out = new ArrayList<>();
        for (StudentEnrollment e : enrollments.findByClassEntityId(classId)) out.add(e.getStudent());
        out.sort((a, b) -> String.valueOf(a.getFullName()).compareToIgnoreCase(String.valueOf(b.getFullName())));
        return out;
    }

    @Transactional(readOnly = true)
    public List<User> parentsOf(Collection<User> students) {
        Map<Long, User> out = new LinkedHashMap<>();
        for (User s : students) for (User p : parents.findParentsByStudent(s.getId())) out.putIfAbsent(p.getId(), p);
        return new ArrayList<>(out.values());
    }

    @Transactional(readOnly = true)
    public List<Subject> subjectsOfTeacher(Long teacherId) {
        return subjects.findSubjectsByTeacherId(teacherId);
    }

    @Transactional(readOnly = true)
    public List<Subject> subjectsOfClass(Long classId) {
        return subjects.findByClassEntityId(classId);
    }

    @Transactional(readOnly = true)
    public boolean teacherTeachesClass(Long teacherId, Long classId) {
        if (classId == null) return false;
        for (Subject s : subjects.findByClassEntityId(classId)) {
            if (s.getTeacher() != null && s.getTeacher().getId().equals(teacherId)) return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean isParentOf(Long parentId, Long studentId) {
        for (User p : parents.findParentsByStudent(studentId)) if (p.getId().equals(parentId)) return true;
        return false;
    }

    @Transactional(readOnly = true)
    public List<User> childrenOf(Long parentId) {
        return parents.findStudentsByParent(parentId);
    }

    /** the child chosen on the page, but only if it really is the parent's child; else the first child (or null) */
    @Transactional(readOnly = true)
    public User pickChild(Long parentId, Long wanted) {
        List<User> kids = childrenOf(parentId);
        for (User k : kids) if (wanted != null && k.getId().equals(wanted)) return k;
        return kids.isEmpty() ? null : kids.get(0);
    }

    @Transactional(readOnly = true)
    public boolean mayViewStudent(User viewer, Long studentId) {
        String r = role(viewer);
        if (r.equals("ADMIN")) return true;
        if (r.equals("STUDENT")) return viewer.getId().equals(studentId);
        if (r.equals("PARENT")) return isParentOf(viewer.getId(), studentId);
        if (r.equals("TEACHER")) return teacherTeachesClass(viewer.getId(), classOf(studentId));
        return false;
    }

    /** in-app message; one failure never stops the real work */
    public void tell(Collection<User> to, String message) {
        for (User u : to) {
            try {
                notifier.sendNotification(u, message);
            } catch (RuntimeException ignored) {
                // best effort
            }
        }
    }

    public void tell(User to, String message) {
        if (to != null) tell(List.of(to), message);
    }
}
