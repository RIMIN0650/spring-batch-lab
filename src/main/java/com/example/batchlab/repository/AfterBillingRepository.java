package com.example.batchlab.repository;


import com.example.batchlab.model.AfterBilling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AfterBillingRepository extends JpaRepository<AfterBilling, Long> {
    boolean existsByStoreIdxAndPayedMonth(Long storeIdx, String payedMonth);
    boolean existsByStoreIdxAndPayedMonthAndIsSuccessTrue(Long storeIdx, String payedMonth);
    Optional<AfterBilling> findByStoreIdxAndPayedMonth(Long storeIdx, String payedMonth);
    List<AfterBilling> findByPayedMonthAndIsRetryFailedTrue(String payedMonth);

    @Query("""
    SELECT a.storeIdx
    FROM AfterBilling a
    WHERE a.storeIdx IN :storeIdxs
      AND a.payedMonth = :payedMonth
      AND a.isSuccess = true
    """)
    List<Long> findSuccessfulStoreIdxs(
            @Param("storeIdxs") List<Long> storeIdxs,
            @Param("payedMonth") String payedMonth
    );
}
