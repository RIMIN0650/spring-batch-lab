package com.example.batchlab.service;

import com.example.batchlab.model.Orders;
import com.example.batchlab.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@RequiredArgsConstructor
@Service
public class OrdersService {

    private final OrdersRepository ordersRepository;


    public int totalPayAmount(Long storeIdx) {

        YearMonth currentMonth = YearMonth.now();
        YearMonth lastMonth = currentMonth.minusMonths(1);


        LocalDateTime startOfMonth = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = lastMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);


        List<Orders> lastMonthOrders = ordersRepository.findByStoreIdxAndCreatedAtBetween(
                storeIdx, startOfMonth, endOfMonth
        );

        int totalPayAmount = 0;

        for (Orders orders : lastMonthOrders) {
            totalPayAmount += orders.getPrice();
        }

        return totalPayAmount;
    }
}
