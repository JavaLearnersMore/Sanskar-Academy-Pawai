package com.sanskaracademy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.StaffProfile;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
    Optional<StaffProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    List<StaffProfile> findAllByOrderByUserFullNameAsc();
    List<StaffProfile> findByActiveTrueOrderByUserFullNameAsc();
}
