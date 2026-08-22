package com.example.batchlab.repository;


import com.example.batchlab.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    List<Orders> findByStoreIdxAndCreatedAtBetween(Long storeIdx, LocalDateTime start, LocalDateTime end);
}
