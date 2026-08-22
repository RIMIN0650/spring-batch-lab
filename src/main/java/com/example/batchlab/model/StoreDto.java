package com.example.batchlab.model;

import lombok.*;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StoreDto {

    @Builder
    @Getter
    // 가맹점 리스트 조회
    public static class StoreListRes{
        private Long idx;
        private String storeName;
        private String ownerName;
        private String ownerEmail;
        private String address;
        private String business;
        private String status;


        public static StoreListRes from(Store entity){
            return StoreListRes.builder()
                    .idx(entity.getIdx())
                    .storeName(entity.getStoreName())
                    .ownerName(entity.getUser().getUserName())
                    .ownerEmail(entity.getUser().getEmail())
                    .address(entity.getAddress())
                    .business(entity.getBusiness())
                    .status(entity.isDeleted() ? "폐점":"입점")
                    .build();
        }
    }

    // 가맹점 조회 페이징 처리
    @Getter
    @Builder
    public static class StorePageRes{
        private List<StoreListRes> storeList;
        private int totalPage;
        private long totalCount;
        private int currentPage;
        private int currentSize;

        public static StorePageRes from(Page<Store> entity){
            return StorePageRes.builder()
                    .storeList(entity.map(StoreListRes::from).getContent())
                    .totalPage(entity.getTotalPages())
                    .totalCount(entity.getTotalElements())
                    .currentPage(entity.getPageable().getPageNumber())
                    .currentSize(entity.getPageable().getPageSize())
                    .build();
        }
    }

    // 가맹점 수 조회
    @Getter
    @Builder
    public static class StoreTotalRes{
        private Long totalCount;   // 총 가맹점 수
        private Long activeCount;  // 입점
        private Long closedCount;  // 폐점
    }

    @Builder
    @Getter
    // 가맹점 목록 상세 조회
    public static class StoreDetailListRes{
        private Long idx;
        private String storeName;
        private String ownerName;
        private String ownerEmail;
        private String postcode;
        private String address;
        private String addressDetail;
        private String totalAddress;
        private String business;
        private String createdAt;
        private String closedAt;
        private String filePath;

        public static StoreDetailListRes from(Store entity){

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            return StoreDetailListRes.builder()
                    .idx(entity.getIdx())
                    .storeName(entity.getStoreName())
                    .ownerName(entity.getUser().getUserName())
                    .ownerEmail(entity.getUser().getEmail())
                    .postcode(entity.getPostcode())
                    .address(entity.getAddress())
                    .addressDetail(entity.getAddressDetail())
                    .totalAddress(String.format("[%s] %s %s",
                            entity.getPostcode(),
                            entity.getAddress(),
                            entity.getAddressDetail()).trim())
                    .business(entity.getBusiness())
                    .createdAt(entity.getCreatedAt().format(formatter))
                    .closedAt(
                            entity.getClosedAt() == null && !entity.isDeleted()
                                    ? "운영 중"
                                    : entity.getClosedAt().format(formatter)

                    )
                    .filePath(entity.getFilePath())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreSearchReq {
        private String keyword;
    }

    // 가맹점 검색
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreSearchPagingReq {
        private String keyword;
        private String status;
    }

    @Builder
    @Getter
    public static class StoreSearchRes {
        private Long idx;
        private String storeName;
        private String address;

        public static StoreSearchRes from(Store entity) {
            return StoreSearchRes.builder()
                    .idx(entity.getIdx())
                    .storeName(entity.getStoreName())
                    .address(entity.getAddress())
                    .build();
        }
    }

    // 신규 가맹점 등록 req
    @Getter
    public static class StoreRegReq{
        private String storeName;
        private String ownerEmail;
        private String postcode;
        private String address;
        private String addressDetail;
        private String business;
        private String filePath;

        public Store toEntity(){
            return Store.builder()
                    .storeName(this.storeName)
                    .postcode(this.postcode)
                    .address(this.address)
                    .addressDetail(this.addressDetail)
                    .business(this.business)
                    .filePath(this.filePath)
                    .build();
        }
    }

    // 가맹점 수정
    @Getter
    @Builder
    public static class StoreUpdateReq{
        private String storeName;
        private String ownerName;
        private String ownerEmail;
        private String postcode;
        private String address;
        private String addressDetail;
        private LocalDateTime closedAt;
        private String filePath;

    }
}
