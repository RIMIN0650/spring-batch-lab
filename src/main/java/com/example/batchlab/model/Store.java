package com.example.batchlab.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "store")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_idx")
    private Long idx;

    @Column(name = "store_name", nullable = false, unique = true)
    private String storeName;

    @Column(nullable = false)
    private String address;

    @Column(name = "address_detail", nullable = false)
    private String addressDetail;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(nullable = false,  unique = true)
    private String business;

    @Column(nullable = false)
    private String postcode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    public void update(StoreDto.StoreUpdateReq dto) {
        this.storeName = dto.getStoreName();
        this.postcode = dto.getPostcode();
        this.address = dto.getAddress();
        this.addressDetail = dto.getAddressDetail();
        this.filePath = dto.getFilePath();

        if (dto.getClosedAt() != null) {
            this.closedAt = dto.getClosedAt();
            this.isDeleted = true;
        }
    }

}
