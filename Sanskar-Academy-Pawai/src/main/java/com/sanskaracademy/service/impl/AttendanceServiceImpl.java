package com.sanskaracademy.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sanskaracademy.entity.Attendance;
import com.sanskaracademy.entity.AttendanceStatus;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.AttendanceRepository;
import com.sanskaracademy.repository.StudentParentRepository;
import com.sanskaracademy.service.AttendanceService;
import com.sanskaracademy.service.NotificationService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

	private final AttendanceRepository attendanceRepository = null;
	private final StudentParentRepository studentParentRepository = null;
	private final NotificationService notificationService = null;

	@Override
	public void markAttendance(User student,
			Subject subject,
			AttendanceStatus status,
			User teacher) {

		Attendance attendance = Attendance.builder()
				.student(student)
				.subject(subject)
				.attendanceDate(LocalDate.now())
				.status(status)
				.markedBy(teacher)
				.build();

		attendanceRepository.save(attendance);

		if (status == AttendanceStatus.ABSENT) {

			List<User> parents =
					studentParentRepository.findParentsByStudent(student.getId());

			for (User parent : parents) {
				notificationService.sendNotification(
						parent,
						"Your child " + student.getFullName() +
						" was absent on " + LocalDate.now()
						);
			}
		}
	}

	@Override
	public List<Attendance> getStudentAttendance(Long studentId) {
        return attendanceRepository.findWithSubjectByStudent(studentId);
    }

}
