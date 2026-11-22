package com.sanskaracademy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.BankQuestion;

public interface BankQuestionRepository extends JpaRepository<BankQuestion, Long> {
    List<BankQuestion> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
