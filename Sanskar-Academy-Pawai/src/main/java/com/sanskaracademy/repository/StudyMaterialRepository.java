package com.sanskaracademy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.StudyMaterial;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    List<StudyMaterial> findBySubjectTeacherIdOrderByUploadedAtDesc(Long teacherId);
    List<StudyMaterial> findBySubjectClassEntityIdOrderBySubjectSubjectNameAscChapterAscUploadedAtDesc(Long classId);
}
