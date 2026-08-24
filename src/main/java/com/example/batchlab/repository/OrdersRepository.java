package com.example.batchlab.repository;


import com.example.batchlab.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

//    List<Orders> findByStoreIdxAndCreatedAtBetween(Long storeIdx, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.price), 0) FROM Orders o " +
        "WHERE o.store.idx = :storeIdx " +
        "AND o.createdAt >= :startDate " +
        "AND o.createdAt < :endDate")
        int sumPriceByStoreAndPeriod(@Param("storeIdx") Long storeIdx,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);


}
