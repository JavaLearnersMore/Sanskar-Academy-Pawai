package com.sanskaracademy.controller;

import java.security.Principal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.service.NoticeService;
import com.sanskaracademy.service.SchoolAccess;

import jakarta.servlet.http.HttpServletRequest;

/** Notice board for admin, teacher, student and parent (same page, different rights). */
@Controller
public class NoticeController {

    private final NoticeService notices;
    private final SchoolAccess access;

    public NoticeController(NoticeService notices, SchoolAccess access) {
        this.notices = notices;
        this.access = access;
    }

    private static String base(HttpServletRequest req) {
        String u = req.getRequestURI();
        if (u.startsWith("/admin")) return "/admin";
        if (u.startsWith("/teacher")) return "/teacher";
        if (u.startsWith("/parent")) return "/parent";
        return "/student";
    }

    @GetMapping({ "/admin/notices", "/teacher/notices", "/student/notices", "/parent/notices" })
    public String board(Model m, Principal p, HttpServletRequest req) {
        User me = access.me(p);
        String base = base(req);
        boolean canPost = base.equals("/admin") || base.equals("/teacher");
        m.addAttribute("base", base);
        m.addAttribute("admin", base.equals("/admin"));
        m.addAttribute("canPost", canPost);
        m.addAttribute("rows", notices.boardFor(me, true));
        if (canPost) m.addAttribute("classes", notices.classesFor(me));
        m.addAttribute("audiences", NoticeService.AUDIENCES);
        return "notice/board";
    }

    @PostMapping({ "/admin/notices", "/teacher/notices" })
    public String post(@RequestParam String title, @RequestParam String body, @RequestParam(defaultValue = "CLASS") String audience,
                       @RequestParam(required = false) Long classId, @RequestParam(defaultValue = "false") boolean pinned,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresOn,
                       Principal p, RedirectAttributes ra, HttpServletRequest req) {
        String base = base(req);
        try {
            notices.post(access.me(p), title, body, audience, classId, pinned, expiresOn);
            ra.addFlashAttribute("success", "Notice posted. Everyone concerned was told.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + base + "/notices";
    }

    @PostMapping({ "/admin/notices/{id}/delete", "/teacher/notices/{id}/delete" })
    public String delete(@PathVariable Long id, Principal p, RedirectAttributes ra, HttpServletRequest req) {
        try {
            notices.delete(id, access.me(p));
            ra.addFlashAttribute("success", "Notice deleted.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + base(req) + "/notices";
    }

    @GetMapping("/admin/notices/{id}/unread")
    public String unread(@PathVariable Long id, Model m, RedirectAttributes ra) {
        try {
            m.addAttribute("people", notices.unreadBy(id));
            m.addAttribute("noticeId", id);
            return "notice/unread";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/notices";
        }
    }
}
