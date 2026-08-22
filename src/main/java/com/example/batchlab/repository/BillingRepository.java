package com.example.batchlab.repository;


import com.example.batchlab.model.Billing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingRepository extends JpaRepository<Billing, Long> {

    Billing findByStoreIdx(Long storeIdx);

}
