package com.sanskaracademy.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.MeetingSlot;
import com.sanskaracademy.entity.MeetingStatus;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.MeetingSlotRepository;
import com.sanskaracademy.repository.UserRepository;

/** Teacher opens meeting slots, parent books one, teacher confirms. */
@Service
public class MeetingService {

    private final MeetingSlotRepository repo;
    private final UserRepository users;
    private final SchoolAccess access;

    public MeetingService(MeetingSlotRepository repo, UserRepository users, SchoolAccess access) {
        this.repo = repo;
        this.users = users;
        this.access = access;
    }

    // ------------------------------------------------------------------ teacher
    @Transactional
    public int addSlots(User teacher, LocalDate date, LocalTime start, int count, int minutes, String mode) {
        if (date == null || start == null) throw new IllegalArgumentException("Choose the date and start time");
        if (count < 1 || count > 12) throw new IllegalArgumentException("Number of slots must be between 1 and 12");
        if (minutes < 5 || minutes > 120) throw new IllegalArgumentException("Slot length must be 5 to 120 minutes");
        LocalDateTime at = LocalDateTime.of(date, start);
        if (!at.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Slots must be in the future");
        int made = 0;
        for (int i = 0; i < count; i++) {
            LocalDateTime s = at.plusMinutes((long) i * minutes), e = s.plusMinutes(minutes);
            if (!repo.findByTeacherIdAndStartAtLessThanAndEndAtGreaterThan(teacher.getId(), e, s).isEmpty()) continue;
            MeetingSlot m = new MeetingSlot();
            m.setTeacher(teacher);
            m.setStartAt(s);
            m.setEndAt(e);
            m.setMode(mode == null || mode.isBlank() ? "In school" : (mode.trim().length() > 60 ? mode.trim().substring(0, 60) : mode.trim()));
            m.setStatus(MeetingStatus.OPEN);
            m.setCreatedAt(LocalDateTime.now());
            repo.save(m);
            made++;
        }
        if (made == 0) throw new IllegalArgumentException("Those times overlap with slots you already have");
        return made;
    }

    @Transactional(readOnly = true)
    public List<MeetingSlot> ofTeacher(Long teacherId) {
        List<MeetingSlot> out = new ArrayList<>();
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        for (MeetingSlot s : repo.findByTeacherIdOrderByStartAtAsc(teacherId)) if (s.getStartAt().isAfter(from)) out.add(s);
        return out;
    }

    private MeetingSlot mineSlot(Long id, Long teacherId) {
        MeetingSlot s = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (!s.getTeacher().getId().equals(teacherId)) throw new IllegalArgumentException("This slot belongs to another teacher");
        return s;
    }

    @Transactional
    public void confirm(Long id, Long teacherId) {
        MeetingSlot s = mineSlot(id, teacherId);
        if (s.getStatus() != MeetingStatus.BOOKED) throw new IllegalArgumentException("Only booked slots can be confirmed");
        s.setStatus(MeetingStatus.CONFIRMED);
        repo.save(s);
        access.tell(s.getParent(), "Your meeting with " + s.getTeacher().getFullName() + " on " + fmt(s) + " is confirmed.");
    }

    @Transactional
    public void done(Long id, Long teacherId, String note) {
        MeetingSlot s = mineSlot(id, teacherId);
        if (s.getStatus() != MeetingStatus.CONFIRMED && s.getStatus() != MeetingStatus.BOOKED)
            throw new IllegalArgumentException("This meeting is not booked");
        s.setStatus(MeetingStatus.DONE);
        s.setTeacherNote(note == null || note.isBlank() ? null : (note.trim().length() > 250 ? note.trim().substring(0, 250) : note.trim()));
        repo.save(s);
    }

    /** teacher cancels: an open slot is removed; a booked one is cancelled and the parent is told */
    @Transactional
    public void teacherCancel(Long id, Long teacherId, String reason) {
        MeetingSlot s = mineSlot(id, teacherId);
        if (s.getStatus() == MeetingStatus.OPEN) {
            repo.delete(s);
            return;
        }
        if (s.getStatus() == MeetingStatus.DONE || s.getStatus() == MeetingStatus.CANCELLED) throw new IllegalArgumentException("Already finished");
        User parent = s.getParent();
        s.setStatus(MeetingStatus.CANCELLED);
        s.setTeacherNote(reason == null || reason.isBlank() ? null : (reason.trim().length() > 250 ? reason.trim().substring(0, 250) : reason.trim()));
        repo.save(s);
        access.tell(parent, "Your meeting with " + s.getTeacher().getFullName() + " on " + fmt(s) + " was cancelled by the teacher. Please book another time.");
    }

    // ------------------------------------------------------------------ parent
    public static class TeacherSlots {
        private final User teacher;
        private final List<MeetingSlot> slots = new ArrayList<>();
        TeacherSlots(User t) { teacher = t; }
        public User getTeacher() { return teacher; }
        public List<MeetingSlot> getSlots() { return slots; }
    }

    /** open future slots of the teachers who teach the parent's children */
    @Transactional(readOnly = true)
    public List<TeacherSlots> openFor(User parent) {
        Map<Long, TeacherSlots> out = new LinkedHashMap<>();
        for (User kid : access.childrenOf(parent.getId())) {
            Long cid = access.classOf(kid.getId());
            if (cid == null) continue;
            for (Subject sub : access.subjectsOfClass(cid)) {
                User t = sub.getTeacher();
                if (t == null || out.containsKey(t.getId())) continue;
                TeacherSlots ts = new TeacherSlots(t);
                ts.slots.addAll(repo.findByTeacherIdAndStatusAndStartAtAfterOrderByStartAtAsc(t.getId(), MeetingStatus.OPEN, LocalDateTime.now()));
                if (!ts.slots.isEmpty()) out.put(t.getId(), ts);
            }
        }
        return new ArrayList<>(out.values());
    }

    @Transactional(readOnly = true)
    public List<MeetingSlot> ofParent(Long parentId) {
        return repo.findByParentIdOrderByStartAtAsc(parentId);
    }

    @Transactional
    public void book(User parent, Long slotId, Long studentId, String purpose) {
        MeetingSlot s = repo.findById(slotId).orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (s.getStatus() != MeetingStatus.OPEN) throw new IllegalArgumentException("Sorry, this slot was just taken. Please choose another.");
        if (!s.getStartAt().isAfter(LocalDateTime.now())) throw new IllegalArgumentException("This slot is in the past");
        if (!access.isParentOf(parent.getId(), studentId)) throw new IllegalArgumentException("This student is not linked to your account");
        if (!access.teacherTeachesClass(s.getTeacher().getId(), access.classOf(studentId)))
            throw new IllegalArgumentException("This teacher does not teach your child");
        User kid = users.findById(studentId).orElseThrow(() -> new IllegalArgumentException("Student not found"));
        s.setParent(parent);
        s.setStudent(kid);
        s.setPurpose(purpose == null || purpose.isBlank() ? null : (purpose.trim().length() > 250 ? purpose.trim().substring(0, 250) : purpose.trim()));
        s.setStatus(MeetingStatus.BOOKED);
        repo.save(s);   // @Version stops two parents booking the same slot
        access.tell(s.getTeacher(), parent.getFullName() + " booked a meeting about " + kid.getFullName() + " on " + fmt(s));
    }

    @Transactional
    public void parentCancel(User parent, Long slotId) {
        MeetingSlot s = repo.findById(slotId).orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (s.getParent() == null || !s.getParent().getId().equals(parent.getId())) throw new IllegalArgumentException("This is not your booking");
        if (s.getStatus() != MeetingStatus.BOOKED && s.getStatus() != MeetingStatus.CONFIRMED) throw new IllegalArgumentException("This booking cannot be cancelled");
        if (!s.getStartAt().isAfter(LocalDateTime.now())) throw new IllegalArgumentException("The meeting time has already passed");
        User kid = s.getStudent();
        s.setParent(null);
        s.setStudent(null);
        s.setPurpose(null);
        s.setStatus(MeetingStatus.OPEN);
        repo.save(s);
        access.tell(s.getTeacher(), parent.getFullName() + " cancelled the meeting about " + (kid == null ? "their child" : kid.getFullName()) + " on " + fmt(s) + ". The slot is open again.");
    }

    private static String fmt(MeetingSlot s) {
        return s.getStartAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, hh:mm a", java.util.Locale.ENGLISH));
    }
}
