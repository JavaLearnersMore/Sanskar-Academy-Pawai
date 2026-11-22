package com.sanskaracademy.repository;

import java.time.*;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sanskaracademy.entity.*;

public interface NoticeReadRepository extends JpaRepository<NoticeRead, Long> {
    List<NoticeRead> findByUserId(Long userId);
    long countByNoticeId(Long noticeId);
    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);
    void deleteByNoticeId(Long noticeId);
}
