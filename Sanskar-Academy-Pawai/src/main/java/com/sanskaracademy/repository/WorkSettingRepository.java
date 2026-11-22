package com.sanskaracademy.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.WorkSetting;

public interface WorkSettingRepository extends JpaRepository<WorkSetting, Long> {
}
