package com.sanskaracademy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.Chapter;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findBySubjectIdOrderByOrderNoAscIdAsc(Long subjectId);
    List<Chapter> findAllByOrderBySubjectIdAscOrderNoAscIdAsc();
    int countBySubjectId(Long subjectId);
}
