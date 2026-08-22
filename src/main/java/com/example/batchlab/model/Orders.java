package com.example.batchlab.model;

import com.example.batchlab.enums.OrdersStatus;
import com.example.batchlab.enums.OrdersType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orders_idx")
    private Long idx;

    @Column(name = "price", nullable = false)
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrdersType ordersType;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrdersStatus ordersStatus;

    @Column(name = "is_danger", nullable = false)
    private boolean isDanger;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_idx", nullable = false)
    private Store store;


    public void confirm() {
        this.ordersStatus = OrdersStatus.CONFIRMED;
        this.createdAt = LocalDateTime.now();
    }

    public void cancel() {
        this.ordersStatus = OrdersStatus.CANCELLED;
    }

    public void approve() {
        this.ordersStatus = OrdersStatus.APPROVE;
    }

    public void reject() {
        this.ordersStatus = OrdersStatus.REJECT;
    }

    public void markDanger(boolean isDanger) {
        this.isDanger = isDanger;
    }

    public void updatePrice(Long price) {
        this.price = price;
    }
}
