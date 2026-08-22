package com.example.batchlab.repository;


import com.example.batchlab.model.AfterBilling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AfterBillingRepository extends JpaRepository<AfterBilling, Long> {
    boolean existsByStoreIdxAndPayedMonth(Long storeIdx, String payedMonth);
    boolean existsByStoreIdxAndPayedMonthAndIsSuccessTrue(Long storeIdx, String payedMonth);
    Optional<AfterBilling> findByStoreIdxAndPayedMonth(Long storeIdx, String payedMonth);
    List<AfterBilling> findByPayedMonthAndIsRetryFailedTrue(String payedMonth);
}
