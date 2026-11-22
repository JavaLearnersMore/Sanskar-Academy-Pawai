package com.sanskaracademy.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sanskaracademy.dto.AttendanceRowDto;
import com.sanskaracademy.entity.ClassEntity;
import com.sanskaracademy.entity.LectureSchedule;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;

public interface AcademicService {

    // ---- classes & subjects ----
    List<ClassEntity> getAllClasses();
    ClassEntity createClass(String className, String academicYear);

    List<Subject> getAllSubjects();
    Subject createSubject(String subjectName, Long classId, Long teacherId);
    void reassignTeacher(Long subjectId, Long teacherId);

    // ---- students in classes ----
    void enrollStudent(Long studentId, Long classId);
    void unenrollStudent(Long studentId);
    Map<Long, ClassEntity> getClassByStudent();          // studentId -> class
    Map<Long, Long> getStudentCountByClass();            // classId -> number of students
    Optional<ClassEntity> getStudentClass(Long studentId);
    List<User> getStudentsOfClass(Long classId);

    // ---- timetable ----
    List<LectureSchedule> getAllLectures();
    LectureSchedule addLecture(Long subjectId, DayOfWeek day, LocalTime start, LocalTime end, String room);
    void deleteLecture(Long lectureId);
    LectureSchedule updateLecture(Long lectureId, Long subjectId, DayOfWeek day, LocalTime start, LocalTime end, String room);
    List<LectureSchedule> getTeacherLectures(Long teacherId);
    List<LectureSchedule> getTeacherLecturesToday(Long teacherId);
    List<LectureSchedule> getClassLectures(Long classId);
    List<LectureSchedule> getClassLecturesToday(Long classId);

    // ---- attendance (teacher) ----
    List<Subject> getTeacherSubjects(Long teacherId);
    Subject getTeacherSubject(Long subjectId, Long teacherId);
    List<AttendanceRowDto> getAttendanceRows(Long teacherId, Long subjectId, LocalDate date);
    int markAttendance(Long teacherId, Long subjectId, LocalDate date, Map<Long, String> statusByStudent);

    // ---- parent <-> student links ----
    List<User> getChildren(Long parentId);
    boolean isParentOf(Long parentId, Long studentId);
    void linkParent(Long parentId, Long studentId);
    void unlinkParent(Long parentId, Long studentId);
}
