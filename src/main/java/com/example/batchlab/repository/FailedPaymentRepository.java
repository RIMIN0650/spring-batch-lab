package com.example.batchlab.repository;


import com.example.batchlab.model.FailedPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailedPaymentRepository extends JpaRepository<FailedPayment, Long> {
    Page<FailedPayment> findByPayedMonth(String payedMonth, Pageable pageable);
    List<FailedPayment> findByPayedMonth(String payedMonth);
    boolean existsByStoreIdxAndPayedMonth(Long storeIdx, String payedMonth);
}
