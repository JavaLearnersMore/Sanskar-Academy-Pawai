package com.sanskaracademy.controller;

import java.security.Principal;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.OfflineExamService;

/** Shows one answer-sheet photo / PDF, only to the teacher, the admin, the student or the parent concerned. */
@Controller
@RequestMapping("/exams/sheet")
public class ExamSheetController {

    private final OfflineExamService offline;
    private final UserRepository users;

    public ExamSheetController(OfflineExamService offline, UserRepository users) {
        this.offline = offline;
        this.users = users;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> view(@PathVariable Long id, Principal principal) {
        try {
            User u = users.findByUsername(principal.getName()).orElseThrow();
            OfflineExamService.FileView f = offline.open(id, u);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(f.contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"answer-sheet-" + id + "\"")
                    .header("X-Content-Type-Options", "nosniff")
                    .cacheControl(CacheControl.noStore())
                    .body(new FileSystemResource(f.path));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
