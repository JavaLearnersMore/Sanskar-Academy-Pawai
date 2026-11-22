package com.sanskaracademy.repository;

import java.time.*;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sanskaracademy.entity.*;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByPinnedDescPostedAtDesc();
    List<Notice> findByPostedByIdOrderByPostedAtDesc(Long userId);
}
