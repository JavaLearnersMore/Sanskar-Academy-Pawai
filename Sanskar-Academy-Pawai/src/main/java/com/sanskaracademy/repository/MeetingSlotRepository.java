package com.sanskaracademy.repository;

import java.time.*;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sanskaracademy.entity.*;

public interface MeetingSlotRepository extends JpaRepository<MeetingSlot, Long> {
    List<MeetingSlot> findByTeacherIdOrderByStartAtAsc(Long teacherId);
    List<MeetingSlot> findByParentIdOrderByStartAtAsc(Long parentId);
    List<MeetingSlot> findByTeacherIdAndStatusAndStartAtAfterOrderByStartAtAsc(Long teacherId, MeetingStatus status, LocalDateTime after);
    List<MeetingSlot> findByTeacherIdAndStartAtLessThanAndEndAtGreaterThan(Long teacherId, LocalDateTime end, LocalDateTime start);
}
