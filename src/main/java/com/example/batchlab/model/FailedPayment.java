package com.example.batchlab.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "failed_payment")
public class FailedPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(nullable = false)
    private Long storeIdx;

    @Column(nullable = false)
    private Integer totalPayAmount;

    @Column(nullable = false)
    private String payedMonth; // yyyy-MM 형식

    @Column(length = 500)
    private String failReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
