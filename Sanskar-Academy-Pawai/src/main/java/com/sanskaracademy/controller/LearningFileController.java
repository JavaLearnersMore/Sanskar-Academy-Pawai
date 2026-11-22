package com.sanskaracademy.controller;

import java.security.Principal;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.sanskaracademy.entity.Homework;
import com.sanskaracademy.entity.HomeworkSubmission;
import com.sanskaracademy.entity.StudyMaterial;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.service.FileStoreService;
import com.sanskaracademy.service.HomeworkService;
import com.sanskaracademy.service.MaterialService;
import com.sanskaracademy.service.SchoolAccess;

/**
 * Downloads for homework files, submitted files and study material.
 * Every download checks that the logged-in user may see that file. Files are always sent as downloads.
 */
@Controller
public class LearningFileController {

    private final SchoolAccess access;
    private final FileStoreService files;
    private final HomeworkService homework;
    private final MaterialService materials;

    public LearningFileController(SchoolAccess access, FileStoreService files, HomeworkService homework, MaterialService materials) {
        this.access = access;
        this.files = files;
        this.homework = homework;
        this.materials = materials;
    }

    private boolean mayViewClassFile(User u, Subject s) {
        String r = SchoolAccess.role(u);
        if (r.equals("ADMIN")) return true;
        if (r.equals("TEACHER")) return s.getTeacher() != null && s.getTeacher().getId().equals(u.getId());
        Long classId = s.getClassEntity().getId();
        if (r.equals("STUDENT")) return classId.equals(access.classOf(u.getId()));
        if (r.equals("PARENT")) {
            for (User k : access.childrenOf(u.getId())) if (classId.equals(access.classOf(k.getId()))) return true;
        }
        return false;
    }

    private ResponseEntity<Resource> send(String path, String name) {
        Resource r = files.load(path);
        String ext = path.substring(path.lastIndexOf('.') + 1);
        String fileName = name == null || name.isBlank() ? "file." + ext : name;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(r);
    }

    private static ResponseEntity<Resource> denied() {
        return ResponseEntity.status(403).build();
    }

    @GetMapping("/files/homework/{id}")
    public ResponseEntity<Resource> homeworkFile(@PathVariable Long id, Principal p) {
        User u = access.me(p);
        Homework h = homework.find(id);
        if (h.getAttachmentPath() == null || !mayViewClassFile(u, h.getSubject())) return denied();
        return send(h.getAttachmentPath(), h.getAttachmentName());
    }

    @GetMapping("/files/submission/{id}")
    public ResponseEntity<Resource> submissionFile(@PathVariable Long id, Principal p) {
        User u = access.me(p);
        HomeworkSubmission s = homework.findSubmission(id);
        if (s.getFilePath() == null) return denied();
        String r = SchoolAccess.role(u);
        boolean ok = r.equals("ADMIN")
                || (r.equals("STUDENT") && s.getStudent().getId().equals(u.getId()))
                || (r.equals("PARENT") && access.isParentOf(u.getId(), s.getStudent().getId()))
                || (r.equals("TEACHER") && s.getHomework().getSubject().getTeacher() != null
                        && s.getHomework().getSubject().getTeacher().getId().equals(u.getId()));
        if (!ok) return denied();
        return send(s.getFilePath(), s.getFileName());
    }

    @GetMapping("/files/material/{id}")
    public ResponseEntity<Resource> materialFile(@PathVariable Long id, Principal p) {
        User u = access.me(p);
        StudyMaterial m = materials.find(id);
        if (m.getFilePath() == null || !mayViewClassFile(u, m.getSubject())) return denied();
        return send(m.getFilePath(), m.getFileName());
    }
}
