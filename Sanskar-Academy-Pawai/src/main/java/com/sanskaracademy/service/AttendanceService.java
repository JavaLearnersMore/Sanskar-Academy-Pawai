package com.sanskaracademy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sanskaracademy.entity.Attendance;
import com.sanskaracademy.entity.AttendanceStatus;
import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;

@Service
public interface AttendanceService {
	
	public void markAttendance(User student,
            Subject subject,
            AttendanceStatus status,
            User teacher);
	
	public List<Attendance> getStudentAttendance(Long studentId);

}
