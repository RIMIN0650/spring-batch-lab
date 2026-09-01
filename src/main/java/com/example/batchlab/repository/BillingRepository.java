package com.example.batchlab.repository;


import com.example.batchlab.model.Billing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BillingRepository extends JpaRepository<Billing, Long> {

    Billing findByStoreIdx(Long storeIdx);

    @Query("""
        SELECT b
        FROM Billing b
        WHERE b.storeIdx IN :storeIdxs
    """)
    List<Billing> findByStoreIdxIn(@Param("storeIdxs") List<Long> storeIdxs);

}
