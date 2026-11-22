package com.sanskaracademy.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.dto.AttendanceRowDto;
import com.sanskaracademy.entity.Attendance;
import com.sanskaracademy.entity.AttendanceStatus;
import com.sanskaracademy.entity.ClassEntity;
import com.sanskaracademy.entity.LeaveStatus;
import com.sanskaracademy.entity.LectureSchedule;
import com.sanskaracademy.entity.StudentEnrollment;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.AttendanceRepository;
import com.sanskaracademy.repository.ClassRepository;
import com.sanskaracademy.repository.LeaveApplicationRepository;
import com.sanskaracademy.repository.LectureScheduleRepository;
import com.sanskaracademy.repository.StudentEnrollmentRepository;
import com.sanskaracademy.repository.StudentParentRepository;
import com.sanskaracademy.repository.SubjectRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.AcademicService;
import com.sanskaracademy.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AcademicServiceImpl implements AcademicService {

    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final LectureScheduleRepository lectureRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveApplicationRepository leaveRepository;
    private final StudentParentRepository studentParentRepository;
    private final NotificationService notificationService;

    private static final Comparator<LectureSchedule> BY_DAY_TIME =
            Comparator.comparing(LectureSchedule::getDayOfWeek)
                      .thenComparing(LectureSchedule::getStartTime);

    // ================= CLASSES & SUBJECTS =================
    @Override
    @Transactional(readOnly = true)
    public List<ClassEntity> getAllClasses() {
        return classRepository.findAll();
    }

    @Override
    @Transactional
    public ClassEntity createClass(String className, String academicYear) {
        if (className == null || className.isBlank() || academicYear == null || academicYear.isBlank()) {
            throw new IllegalArgumentException("Class name and academic year are required");
        }
        String name = className.trim();
        String year = academicYear.trim();
        if (classRepository.findByClassNameAndAcademicYear(name, year).isPresent()) {
            throw new IllegalArgumentException("Class " + name + " (" + year + ") already exists");
        }
        ClassEntity c = new ClassEntity();
        c.setClassName(name);
        c.setAcademicYear(year);
        return classRepository.save(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @Override
    @Transactional
    public Subject createSubject(String subjectName, Long classId, Long teacherId) {
        if (subjectName == null || subjectName.isBlank()) {
            throw new IllegalArgumentException("Subject name is required");
        }
        String name = subjectName.trim();
        ClassEntity c = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found"));
        User teacher = requireRole(teacherId, "TEACHER");

        boolean exists = subjectRepository.findByClassEntityId(classId).stream()
                .anyMatch(s -> s.getSubjectName().equalsIgnoreCase(name));
        if (exists) {
            throw new IllegalArgumentException(name + " already exists in class " + c.getClassName());
        }

        Subject s = new Subject();
        s.setSubjectName(name);
        s.setClassEntity(c);
        s.setTeacher(teacher);
        s = subjectRepository.save(s);

        notificationService.sendNotification(teacher,
                "You have been assigned " + name + " for class " + c.getClassName() + ".");
        return s;
    }

    @Override
    @Transactional
    public void reassignTeacher(Long subjectId, Long teacherId) {
        Subject s = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        User teacher = requireRole(teacherId, "TEACHER");
        s.setTeacher(teacher);
        subjectRepository.save(s);

        notificationService.sendNotification(teacher,
                "You have been assigned " + s.getSubjectName() + " for class "
                        + s.getClassEntity().getClassName() + ".");
    }

    // ================= STUDENTS IN CLASSES =================
    @Override
    @Transactional
    public void enrollStudent(Long studentId, Long classId) {
        User student = requireRole(studentId, "STUDENT");
        ClassEntity c = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found"));

        StudentEnrollment e = enrollmentRepository.findByStudentId(studentId)
                .orElseGet(StudentEnrollment::new);
        e.setStudent(student);
        e.setClassEntity(c);
        enrollmentRepository.save(e);
    }

    @Override
    @Transactional
    public void unenrollStudent(Long studentId) {
        enrollmentRepository.findByStudentId(studentId).ifPresent(enrollmentRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ClassEntity> getClassByStudent() {
        Map<Long, ClassEntity> map = new HashMap<>();
        for (StudentEnrollment e : enrollmentRepository.findAll()) {
            map.put(e.getStudent().getId(), e.getClassEntity());
        }
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getStudentCountByClass() {
        Map<Long, Long> map = new HashMap<>();
        for (StudentEnrollment e : enrollmentRepository.findAll()) {
            map.merge(e.getClassEntity().getId(), 1L, Long::sum);
        }
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClassEntity> getStudentClass(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId).map(StudentEnrollment::getClassEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getStudentsOfClass(Long classId) {
        List<User> list = new ArrayList<>();
        for (StudentEnrollment e : enrollmentRepository.findByClassEntityId(classId)) {
            list.add(e.getStudent());
        }
        list.sort(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    // ================= TIMETABLE =================
    @Override
    @Transactional(readOnly = true)
    public List<LectureSchedule> getAllLectures() {
        List<LectureSchedule> list = new ArrayList<>(lectureRepository.findAll());
        list.sort(BY_DAY_TIME);
        return list;
    }

    @Override
    @Transactional
    public LectureSchedule addLecture(Long subjectId, DayOfWeek day, LocalTime start, LocalTime end, String room) {
        if (subjectId == null || day == null || start == null || end == null) {
            throw new IllegalArgumentException("Subject, day, start time and end time are required");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        long clashes = lectureRepository.countClashes(day, start, end,
                subject.getTeacher().getId(), subject.getClassEntity().getId());
        if (clashes > 0) {
            throw new IllegalArgumentException(
                    "Time clash: the teacher or the class already has a lecture at this time");
        }

        LectureSchedule l = new LectureSchedule();
        l.setSubject(subject);
        l.setDayOfWeek(day);
        l.setStartTime(start);
        l.setEndTime(end);
        l.setRoom(room == null || room.isBlank() ? null : room.trim());
        return lectureRepository.save(l);
    }

    @Override
    @Transactional
    public LectureSchedule updateLecture(Long lectureId, Long subjectId, DayOfWeek day, LocalTime start, LocalTime end, String room) {
        if (lectureId == null || subjectId == null || day == null || start == null || end == null) {
            throw new IllegalArgumentException("Subject, day, start time and end time are required");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        LectureSchedule l = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture not found"));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        long clashes = lectureRepository.countClashesExcluding(lectureId, day, start, end,
                subject.getTeacher().getId(), subject.getClassEntity().getId());
        if (clashes > 0) {
            throw new IllegalArgumentException(
                    "Time clash: the teacher or the class already has a lecture at this time");
        }
        l.setSubject(subject);
        l.setDayOfWeek(day);
        l.setStartTime(start);
        l.setEndTime(end);
        l.setRoom(room == null || room.isBlank() ? null : room.trim());
        return lectureRepository.save(l);
    }

    @Override
    @Transactional
    public void deleteLecture(Long lectureId) {
        lectureRepository.deleteById(lectureId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LectureSchedule> getTeacherLectures(Long teacherId) {
        List<LectureSchedule> list = new ArrayList<>(lectureRepository.findByTeacher(teacherId));
        list.sort(BY_DAY_TIME);
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LectureSchedule> getTeacherLecturesToday(Long teacherId) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        List<LectureSchedule> list = new ArrayList<>();
        for (LectureSchedule l : getTeacherLectures(teacherId)) {
            if (l.getDayOfWeek() == today) list.add(l);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LectureSchedule> getClassLectures(Long classId) {
        List<LectureSchedule> list = new ArrayList<>(lectureRepository.findByClassId(classId));
        list.sort(BY_DAY_TIME);
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LectureSchedule> getClassLecturesToday(Long classId) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        List<LectureSchedule> list = new ArrayList<>();
        for (LectureSchedule l : getClassLectures(classId)) {
            if (l.getDayOfWeek() == today) list.add(l);
        }
        return list;
    }

    // ================= ATTENDANCE =================
    @Override
    @Transactional(readOnly = true)
    public List<Subject> getTeacherSubjects(Long teacherId) {
        return subjectRepository.findSubjectsByTeacherId(teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public Subject getTeacherSubject(Long subjectId, Long teacherId) {
        Subject s = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        if (!s.getTeacher().getId().equals(teacherId)) {
            throw new IllegalArgumentException("This subject is not assigned to you");
        }
        return s;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRowDto> getAttendanceRows(Long teacherId, Long subjectId, LocalDate date) {

        Subject subject = getTeacherSubject(subjectId, teacherId);
        List<User> students = getStudentsOfClass(subject.getClassEntity().getId());

        Map<Long, Attendance> existing = new HashMap<>();
        for (Attendance a : attendanceRepository.findBySubjectIdAndAttendanceDate(subjectId, date)) {
            existing.put(a.getStudent().getId(), a);
        }
        Set<Long> onLeave = new HashSet<>(leaveRepository.findStudentIdsOnLeave(LeaveStatus.APPROVED, date));

        List<AttendanceRowDto> rows = new ArrayList<>();
        for (User s : students) {
            Attendance a = existing.get(s.getId());
            boolean leave = onLeave.contains(s.getId());
            String status = a != null ? a.getStatus().name() : (leave ? "ABSENT" : "PRESENT");
            rows.add(new AttendanceRowDto(s.getId(), s.getFullName(), s.getEmail(), status, leave));
        }
        return rows;
    }

    @Override
    @Transactional
    public int markAttendance(Long teacherId, Long subjectId, LocalDate date, Map<Long, String> statusByStudent) {

        Subject subject = getTeacherSubject(subjectId, teacherId);
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        LocalDate day = date != null ? date : LocalDate.now();
        if (day.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Attendance cannot be marked for a future date");
        }

        Map<Long, Attendance> existing = new HashMap<>();
        for (Attendance a : attendanceRepository.findBySubjectIdAndAttendanceDate(subjectId, day)) {
            existing.put(a.getStudent().getId(), a);
        }

        int saved = 0;
        for (User student : getStudentsOfClass(subject.getClassEntity().getId())) {

            String st = statusByStudent.get(student.getId());
            if (st == null) continue;

            AttendanceStatus status = "ABSENT".equals(st) ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT;

            Attendance a = existing.get(student.getId());
            boolean newlyAbsent = status == AttendanceStatus.ABSENT
                    && (a == null || a.getStatus() != AttendanceStatus.ABSENT);

            if (a == null) {
                a = new Attendance();
                a.setStudent(student);
                a.setSubject(subject);
                a.setAttendanceDate(day);
            }
            a.setStatus(status);
            a.setMarkedBy(teacher);
            attendanceRepository.save(a);
            saved++;

            if (newlyAbsent) {
                for (User parent : studentParentRepository.findParentsByStudent(student.getId())) {
                    notificationService.sendNotification(parent,
                            "Your child " + student.getFullName() + " was absent on " + day
                                    + " (" + subject.getSubjectName() + ").");
                }
            }
        }
        return saved;
    }

    // ================= PARENT <-> STUDENT =================
    @Override
    @Transactional(readOnly = true)
    public List<User> getChildren(Long parentId) {
        return studentParentRepository.findStudentsByParent(parentId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isParentOf(Long parentId, Long studentId) {
        for (User s : studentParentRepository.findStudentsByParent(parentId)) {
            if (s.getId().equals(studentId)) return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void linkParent(Long parentId, Long studentId) {
        requireRole(parentId, "PARENT");
        requireRole(studentId, "STUDENT");
        if (isParentOf(parentId, studentId)) {
            throw new IllegalArgumentException("This parent is already linked to this student");
        }
        studentParentRepository.link(studentId, parentId);
    }

    @Override
    @Transactional
    public void unlinkParent(Long parentId, Long studentId) {
        studentParentRepository.unlink(studentId, parentId);
    }

    private User requireRole(Long userId, String roleName) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (u.getRole() == null || !roleName.equals(u.getRole().getName())) {
            throw new IllegalArgumentException("Selected user is not a " + roleName.toLowerCase());
        }
        return u;
    }
}
