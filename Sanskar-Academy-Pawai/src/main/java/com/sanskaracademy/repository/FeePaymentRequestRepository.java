package com.sanskaracademy.repository;

import java.time.*;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sanskaracademy.entity.*;

public interface FeePaymentRequestRepository extends JpaRepository<FeePaymentRequest, Long> {
    List<FeePaymentRequest> findByParentIdOrderByCreatedAtDesc(Long parentId);
    List<FeePaymentRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);
    List<FeePaymentRequest> findTop100ByOrderByCreatedAtDesc();
    boolean existsByReferenceIgnoreCaseAndStatusIn(String reference, Collection<RequestStatus> statuses);
    long countByStatus(RequestStatus status);
}
