package com.sanskaracademy.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.ClassEntity;
import com.sanskaracademy.entity.Notice;
import com.sanskaracademy.entity.NoticeRead;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.ClassRepository;
import com.sanskaracademy.repository.NoticeReadRepository;
import com.sanskaracademy.repository.NoticeRepository;
import com.sanskaracademy.repository.UserRepository;

/** Notice board with "who has read it" tracking. */
@Service
public class NoticeService {

    public static final List<String> AUDIENCES = List.of("ALL", "STUDENTS", "PARENTS", "TEACHERS", "CLASS");

    private final NoticeRepository notices;
    private final NoticeReadRepository reads;
    private final UserRepository users;
    private final ClassRepository classes;
    private final SchoolAccess access;

    public NoticeService(NoticeRepository notices, NoticeReadRepository reads, UserRepository users,
                         ClassRepository classes, SchoolAccess access) {
        this.notices = notices;
        this.reads = reads;
        this.users = users;
        this.classes = classes;
        this.access = access;
    }

    public static class Row {
        private final Notice notice;
        private final boolean read;
        private final long readCount, audienceSize;
        Row(Notice n, boolean read, long rc, long as) {
            notice = n; this.read = read; readCount = rc; audienceSize = as;
        }
        public Notice getNotice() { return notice; }
        public boolean isRead() { return read; }
        public long getReadCount() { return readCount; }
        public long getAudienceSize() { return audienceSize; }
        public String getAudienceLabel() {
            if ("CLASS".equals(notice.getAudience()) && notice.getClassEntity() != null)
                return "Class " + notice.getClassEntity().getClassName();
            switch (notice.getAudience()) {
                case "STUDENTS": return "All students";
                case "PARENTS": return "All parents";
                case "TEACHERS": return "All teachers & staff";
                default: return "Everyone";
            }
        }
    }

    // ------------------------------------------------------------------ who should see a notice
    @Transactional(readOnly = true)
    public boolean isFor(Notice n, User u) {
        String r = SchoolAccess.role(u);
        if (r.equals("ADMIN")) return true;
        switch (n.getAudience()) {
            case "ALL": return true;
            case "STUDENTS": return r.equals("STUDENT");
            case "PARENTS": return r.equals("PARENT");
            case "TEACHERS": return r.equals("TEACHER") || r.equals("STAFF");
            case "CLASS": {
                if (n.getClassEntity() == null) return false;
                Long cid = n.getClassEntity().getId();
                if (r.equals("STUDENT")) return cid.equals(access.classOf(u.getId()));
                if (r.equals("TEACHER")) return access.teacherTeachesClass(u.getId(), cid);
                if (r.equals("PARENT")) {
                    for (User k : access.childrenOf(u.getId())) if (cid.equals(access.classOf(k.getId()))) return true;
                }
                return false;
            }
            default: return false;
        }
    }

    @Transactional(readOnly = true)
    public List<User> recipients(Notice n) {
        Map<Long, User> out = new LinkedHashMap<>();
        for (User u : users.findAll()) {
            if (!u.isEnabled()) continue;
            String r = SchoolAccess.role(u);
            if (r.equals("ADMIN") || r.isEmpty()) continue;
            if (isFor(n, u)) out.put(u.getId(), u);
        }
        return new ArrayList<>(out.values());
    }

    // ------------------------------------------------------------------ posting
    @Transactional
    public Notice post(User poster, String title, String body, String audience, Long classId, boolean pinned, LocalDate expiresOn) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Please write a title");
        if (body == null || body.isBlank()) throw new IllegalArgumentException("Please write the notice");
        String aud = audience == null ? "ALL" : audience.trim().toUpperCase();
        if (!AUDIENCES.contains(aud)) throw new IllegalArgumentException("Choose who should receive the notice");
        boolean admin = SchoolAccess.role(poster).equals("ADMIN");
        ClassEntity cls = null;
        if (aud.equals("CLASS")) {
            if (classId == null) throw new IllegalArgumentException("Choose the class");
            cls = classes.findById(classId).orElseThrow(() -> new IllegalArgumentException("Class not found"));
            if (!admin && !access.teacherTeachesClass(poster.getId(), classId))
                throw new IllegalArgumentException("You can only post to classes you teach");
        } else if (!admin) {
            throw new IllegalArgumentException("Teachers can post only to their own class");
        }
        if (expiresOn != null && expiresOn.isBefore(LocalDate.now())) throw new IllegalArgumentException("The expiry date is in the past");
        Notice n = new Notice();
        n.setTitle(title.trim().length() > 200 ? title.trim().substring(0, 200) : title.trim());
        n.setBody(body.trim().length() > 3000 ? body.trim().substring(0, 3000) : body.trim());
        n.setAudience(aud);
        n.setClassEntity(cls);
        n.setPostedBy(poster);
        n.setPostedAt(LocalDateTime.now());
        n.setPinned(admin && pinned);
        n.setExpiresOn(expiresOn);
        notices.save(n);
        List<User> to = recipients(n);
        to.removeIf(u -> u.getId().equals(poster.getId()));
        access.tell(to, "New notice: " + n.getTitle());
        return n;
    }

    @Transactional
    public void delete(Long id, User actor) {
        Notice n = notices.findById(id).orElseThrow(() -> new IllegalArgumentException("Notice not found"));
        boolean admin = SchoolAccess.role(actor).equals("ADMIN");
        if (!admin && !n.getPostedBy().getId().equals(actor.getId())) throw new IllegalArgumentException("You can delete only your own notices");
        reads.deleteByNoticeId(id);
        notices.delete(n);
    }

    // ------------------------------------------------------------------ reading
    /** notices this user may see, with read flags; then everything shown is marked as read */
    @Transactional
    public List<Row> boardFor(User u, boolean markRead) {
        boolean admin = SchoolAccess.role(u).equals("ADMIN");
        Set<Long> mine = new HashSet<>();
        for (NoticeRead r : reads.findByUserId(u.getId())) mine.add(r.getNotice().getId());
        List<Row> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Notice n : notices.findAllByOrderByPinnedDescPostedAtDesc()) {
            if (!admin && n.getExpiresOn() != null && n.getExpiresOn().isBefore(today)) continue;
            if (!isFor(n, u)) continue;
            boolean isRead = mine.contains(n.getId()) || n.getPostedBy().getId().equals(u.getId());
            long rc = 0, size = 0;
            if (admin || n.getPostedBy().getId().equals(u.getId())) {
                rc = reads.countByNoticeId(n.getId());
                size = recipients(n).size();
            }
            out.add(new Row(n, isRead, rc, size));
            if (markRead && !isRead) {
                NoticeRead nr = new NoticeRead();
                nr.setNotice(n);
                nr.setUser(u);
                nr.setReadAt(LocalDateTime.now());
                reads.save(nr);
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public int unreadCount(User u) {
        int c = 0;
        Set<Long> mine = new HashSet<>();
        for (NoticeRead r : reads.findByUserId(u.getId())) mine.add(r.getNotice().getId());
        LocalDate today = LocalDate.now();
        for (Notice n : notices.findAllByOrderByPinnedDescPostedAtDesc()) {
            if (n.getExpiresOn() != null && n.getExpiresOn().isBefore(today)) continue;
            if (!mine.contains(n.getId()) && !n.getPostedBy().getId().equals(u.getId()) && isFor(n, u)) c++;
        }
        return c;
    }

    /** names of people who have NOT read the notice (admin view) */
    @Transactional(readOnly = true)
    public List<User> unreadBy(Long noticeId) {
        Notice n = notices.findById(noticeId).orElseThrow(() -> new IllegalArgumentException("Notice not found"));
        Set<Long> done = new HashSet<>();
        for (NoticeRead r : reads.findAll()) if (r.getNotice().getId().equals(noticeId)) done.add(r.getUser().getId());
        List<User> out = new ArrayList<>();
        for (User u : recipients(n)) if (!done.contains(u.getId())) out.add(u);
        return out;
    }

    @Transactional(readOnly = true)
    public List<ClassEntity> classesFor(User u) {
        if (SchoolAccess.role(u).equals("ADMIN")) return classes.findAll();
        List<ClassEntity> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Subject s : access.subjectsOfTeacher(u.getId()))
            if (seen.add(s.getClassEntity().getId())) out.add(s.getClassEntity());
        return out;
    }
}
