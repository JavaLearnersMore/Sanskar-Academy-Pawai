package com.sanskaracademy.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.Enquiry;
import com.sanskaracademy.entity.EnquiryStatus;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.EnquiryRepository;
import com.sanskaracademy.repository.UserRepository;

@Service
public class EnquiryService {

    private static final EnumSet<EnquiryStatus> OPEN = EnumSet.of(EnquiryStatus.NEW, EnquiryStatus.CONTACTED, EnquiryStatus.VISITED);

    private final EnquiryRepository repo;
    private final UserRepository users;
    private final SchoolAccess access;

    public EnquiryService(EnquiryRepository repo, UserRepository users, SchoolAccess access) {
        this.repo = repo;
        this.users = users;
        this.access = access;
    }

    /** used by the office (admin) and by the public form (createdBy = null) */
    @Transactional
    public Enquiry add(User by, String studentName, String parentName, String phone, String email, String cls,
                       String source, String message) {
        if (studentName == null || studentName.isBlank()) throw new IllegalArgumentException("Please enter the student's name");
        String ph = phone == null ? "" : phone.replaceAll("[\\s-]", "");
        if (!ph.matches("\\+?[0-9]{10,15}")) throw new IllegalArgumentException("Please enter a valid mobile number");
        String mail = email == null || email.isBlank() ? null : email.trim();
        if (mail != null && !mail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new IllegalArgumentException("Please enter a valid email or leave it empty");
        Enquiry e = new Enquiry();
        e.setStudentName(clip(studentName, 120));
        e.setParentName(clip(parentName, 120));
        e.setPhone(ph);
        e.setEmail(clip(mail, 120));
        e.setInterestedClass(clip(cls, 60));
        e.setSource(clip(source == null || source.isBlank() ? (by == null ? "Website form" : "Walk-in") : source, 60));
        e.setMessage(clip(message, 500));
        e.setStatus(EnquiryStatus.NEW);
        e.setNextFollowUp(LocalDate.now().plusDays(1));
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        e.setCreatedBy(by);
        repo.save(e);
        if (by == null) {
            for (User a : users.findAll()) if (SchoolAccess.role(a).equals("ADMIN"))
                access.tell(a, "New admission enquiry: " + e.getStudentName() + " (" + e.getPhone() + ")");
        }
        return e;
    }

    @Transactional(readOnly = true)
    public List<Enquiry> list(EnquiryStatus status) {
        return status == null ? repo.findAllByOrderByCreatedAtDesc() : repo.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public List<Enquiry> due() {
        return repo.findDueFollowUps(LocalDate.now(), OPEN);
    }

    @Transactional(readOnly = true)
    public long count(EnquiryStatus s) {
        return repo.countByStatus(s);
    }

    @Transactional(readOnly = true)
    public long lastDays(int days) {
        return repo.countByCreatedAtAfter(LocalDateTime.now().minusDays(days));
    }

    @Transactional
    public void update(Long id, User by, EnquiryStatus status, LocalDate nextFollowUp, String note) {
        Enquiry e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Enquiry not found"));
        if (status != null) e.setStatus(status);
        e.setNextFollowUp(OPEN.contains(e.getStatus()) ? nextFollowUp : null);
        if (note != null && !note.isBlank()) {
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.ENGLISH));
            String line = "[" + stamp + " " + by.getFullName() + "] " + note.trim().replace('\n', ' ');
            String all = e.getNotes() == null ? line : e.getNotes() + "\n" + line;
            if (all.length() > 3000) all = all.substring(all.length() - 3000);
            e.setNotes(all);
        }
        e.setUpdatedAt(LocalDateTime.now());
        repo.save(e);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    /** a line for the daily reminder to the admins */
    @Transactional(readOnly = true)
    public int notifyAdminsOfDue() {
        List<Enquiry> due = due();
        if (due.isEmpty()) return 0;
        String msg = due.size() + " admission enquiry follow-up(s) are due today or overdue. Open Admission Enquiries to call them.";
        for (User a : users.findAll()) if (SchoolAccess.role(a).equals("ADMIN")) access.tell(a, msg);
        return due.size();
    }

    private static String clip(String s, int n) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        return s.length() > n ? s.substring(0, n) : s;
    }
}
