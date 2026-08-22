package com.example.batchlab.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Builder
@AllArgsConstructor @NoArgsConstructor
@Entity
@Getter
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mId") // 대소문자 명시
    private String mId;

    @Column(name = "customer_key") // 대소문자 명시
    private String customerKey;

    @Column(name = "authenticatedAt") // 대소문자 명시
    private String authenticatedAt;

    private String method;

    @Column(name = "billing_key") // 대소문자 명시
    private String billingKey;

    @Column(name = "store_idx") // 기존 store_idx 대신 데이터가 들어있는 컬럼으로 지정
    private Long storeIdx;
}
