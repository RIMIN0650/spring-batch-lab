package com.example.batchlab;

import com.example.batchlab.model.AfterBilling;
import com.example.batchlab.model.Billing;
import com.example.batchlab.model.FailedPayment;
import com.example.batchlab.model.Store;
import com.example.batchlab.repository.AfterBillingRepository;
import com.example.batchlab.repository.BillingRepository;
import com.example.batchlab.repository.FailedPaymentRepository;
import com.example.batchlab.repository.StoreRepository;
import com.example.batchlab.service.OrdersService;
import jakarta.persistence.EntityManagerFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiredArgsConstructor
@Configuration
public class BillingBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final StoreRepository storeRepository;
    private final BillingRepository billingRepository;
    private final OrdersService ordersService;
    private final AfterBillingRepository afterBillingRepository;
    private final FailedPaymentRepository failedPaymentRepository;
    private final EntityManagerFactory entityManagerFactory;

    @Value("${billing.secret-key}")
    private String secretKey;

    private static final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/1540712775842529400/Bl04p8VeJacw5dB6qgyMxA0lZJgXCv9kDxmsIAww-QNGTcCcHpaap9MvwP7vz8J6Xcl4";


    public void printQueryStats() {
        var stats = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        System.out.println("Query Execution Count: " + stats.getQueryExecutionCount());
        System.out.println("Entity Fetch Count: " + stats.getEntityFetchCount());
    }




    // Processor에서 Writer로 데이터를 전달하기 위한 DTO
    public record BillingRequestDto(
            Long storeIdx,
            int totalPayAmount,
            String customerKey,
            String billingKey,
            boolean hasBillingInfo // 결제 정보 존재 여부 추가
    ) {}

    @Bean
    public Job settlementJob() {
        return new JobBuilder("billingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(billingStep())
                .listener(billingJobListener())
                .build();
    }

    private final List<SettlementResult> results = Collections.synchronizedList(new ArrayList<>());

    public record SettlementResult(
            Long storeIdx,
            int amount,
            boolean isSuccess,
            String reason
    ) {}

    @Bean
    public JobExecutionListener billingJobListener() {
        return new JobExecutionListener() {
            private final RestTemplate restTemplate = new RestTemplate();

            @Override
            public void beforeJob(JobExecution jobExecution) {
                results.clear(); // 결과 리스트 초기화
                entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics().clear();
                sendDiscordMessage("🚀 [정산 배치] 작업 시작 - 일시: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                String status = jobExecution.getStatus().toString();
                String startTime = jobExecution.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String endTime = jobExecution.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                long successCount = results.stream().filter(SettlementResult::isSuccess).count();
                long failCount = results.stream().filter(r -> !r.isSuccess()).count();

                var stats = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();

                StringBuilder sb = new StringBuilder();
                sb.append("✅ [정산 배치] 작업 종료\n");
                sb.append("- 상태: ").append(status).append("\n");
                sb.append("- 시작 시간: ").append(startTime).append("\n");
                sb.append("- 종료 시간: ").append(endTime).append("\n");
                sb.append("- 총 건수: ").append(results.size()).append("\n");
                sb.append("- 성공: ").append(successCount).append("\n");
                sb.append("- 실패: ").append(failCount).append("\n");
                sb.append("\"배치 작업 중 실행된 쿼리 수: ").append(stats.getQueryExecutionCount());
                
                if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                    sb.append("- 치명적 에러: ").append(jobExecution.getAllFailureExceptions().get(0).getMessage()).append("\n");
                }
                
                sendDiscordMessage(sb.toString());
                
                // 상세 리포트 생성 및 전송
                if (!results.isEmpty()) {
                    sendDiscordFile(generateMarkdownReport(startTime, endTime, successCount, failCount));
                }
            }

            private String generateMarkdownReport(String start, String end, long success, long fail) {
                StringBuilder md = new StringBuilder();
                md.append("# 정산 결과 리포트\n\n");
                md.append("- 실행 일시: ").append(start).append(" ~ ").append(end).append("\n");
                md.append("- 결과 요약: 총 ").append(results.size()).append("건 (성공: ").append(success).append(", 실패: ").append(fail).append(")\n\n");
                md.append("## 상세 내역\n\n");
                md.append("| 가맹점 ID | 금액 | 상태 | 비고 |\n");
                md.append("| :--- | :--- | :--- | :--- |\n");
                
                for (SettlementResult r : results) {
                    md.append("| ").append(r.storeIdx())
                      .append(" | ").append(String.format("%,d", r.amount()))
                      .append(" | ").append(r.isSuccess() ? "✅ 성공" : "❌ 실패")
                      .append(" | ").append(r.reason() != null ? r.reason() : "-")
                      .append(" |\n");
                }
                return md.toString();
            }

            private void sendDiscordMessage(String content) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    Map<String, String> body = new HashMap<>();
                    body.put("content", content);
                    HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
                    restTemplate.postForEntity(DISCORD_WEBHOOK_URL, entity, String.class);
                } catch (Exception e) {
                    System.err.println("Discord 메시지 전송 실패: " + e.getMessage());
                }
            }

            private void sendDiscordFile(String content) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                    org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
                    
                    // 파일 파트 구성
                    HttpHeaders fileHeaders = new HttpHeaders();
                    fileHeaders.setContentDispositionFormData("file", "settlement_report.md");
                    HttpEntity<byte[]> fileEntity = new HttpEntity<>(content.getBytes(), fileHeaders);
                    body.add("file", fileEntity);

                    HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                    restTemplate.postForEntity(DISCORD_WEBHOOK_URL, requestEntity, String.class);
                } catch (Exception e) {
                    System.err.println("Discord 파일 전송 실패: " + e.getMessage());
                }
            }
        };
    }

    @Bean
    public Step billingStep() {
        return new StepBuilder("billingStep", jobRepository)
                .<Store, BillingRequestDto>chunk(5, platformTransactionManager)
                .reader(storeIdReader())
                .processor(billingProcessor())
                .writer(billingWriter(afterBillingRepository))
                .faultTolerant()
                .retry(Exception.class) // 에러 발생 시 재시도
                .retryLimit(3)          // 최대 3회 재시도
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    @Bean
    public ItemReader<Store> storeIdReader() {
        RepositoryItemReader<Store> delegate =
                new RepositoryItemReaderBuilder<Store>()
                        .name("storeIdReader")
                        .pageSize(5)
                        .methodName("findAll")
                        .repository(storeRepository)
                        .sorts(Map.of("idx", Sort.Direction.ASC))
                        .build();

        return () -> {
            Store store = delegate.read();

            if (store != null) {
                System.out.println("DEBUG: READER READ SUCCESS = " + store.getIdx());
            } else {
                System.out.println("DEBUG: READER READ = NULL (데이터 없음)");
            }

//            System.out.println("READER READ = " + (store != null ? store.getIdx() : "NULL"));
            return store;
        };
    }

    @Bean
    public ItemProcessor<Store, BillingRequestDto> billingProcessor() {
        return store -> {
            Long storeIdx = store.getIdx();
            String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

            if (afterBillingRepository.existsByStoreIdxAndPayedMonthAndIsSuccessTrue(storeIdx, currentMonth)) {
                System.out.println("SKIP: 가맹점 " + storeIdx + "는 이미 " + currentMonth + " 정산 완료(성공) 데이터가 존재합니다.");
                results.add(new SettlementResult(storeIdx, 0, true, "이미 정산 완료됨 (Skip)"));
                return null;
            }

            int totalPayAmount = ordersService.totalPayAmount(storeIdx);
            Billing target = billingRepository.findByStoreIdx(storeIdx);

            // 결제 정보가 없더라도 null을 반환하지 않고 DTO를 생성하여 Writer로 넘김
            if (target == null) {
                return new BillingRequestDto(storeIdx, totalPayAmount, null, null, false);
            }

            return new BillingRequestDto(
                    storeIdx,
                    totalPayAmount,
                    target.getCustomerKey(),
                    target.getBillingKey(),
                    true
            );
        };
    }

    @Bean
    public ItemWriter<BillingRequestDto> billingWriter(AfterBillingRepository afterBillingRepository) {
        return new ItemWriter<BillingRequestDto>() {
            private final RestTemplate restTemplate = new RestTemplate();
            private final HttpHeaders headers = new HttpHeaders();

            {
                String encodedAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
                headers.set("Authorization", "Basic " + encodedAuth);
                headers.setContentType(MediaType.APPLICATION_JSON);
            }

            @Override
            public void write(@NonNull Chunk<? extends BillingRequestDto> chunk) throws Exception {
                for (BillingRequestDto req : chunk) {
                    AfterBilling afterBilling = AfterBilling.builder()
                            .storeIdx(req.storeIdx())
                            .totalPayAmount(req.totalPayAmount())
                            .payedMonth(YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
                            .createdAt(LocalDateTime.now())
                            .build();

                    // 1. 결제 정보가 없는 경우 처리
//                    if (!req.hasBillingInfo()) {
//                        afterBilling.setIsPaid(false);
//                        afterBilling.setIsSuccess(false);
//                        afterBilling.setFailReason("가맹점 결제 정보(Billing)가 존재하지 않습니다.");
//                        afterBillingRepository.save(afterBilling);
//                        results.add(new SettlementResult(req.storeIdx(), req.totalPayAmount(), false, "결제 정보 없음"));
//                        continue;
//                    }

                    try {
                        // 2. 금액이 0원 이하인 경우 (선택 사항)
                        if (req.totalPayAmount() <= 0) {
                            afterBilling.setIsPaid(false);
                            afterBilling.setIsSuccess(false);
                            afterBilling.setFailReason("결제할 금액이 0원 이하입니다.");
                            afterBillingRepository.save(afterBilling);
                            results.add(new SettlementResult(req.storeIdx(), req.totalPayAmount(), false, "결제 금액 0원 이하"));
                            continue;
                        }

                        // 3. 결제 API 호출
//                        Map<String, Object> map = new HashMap<>();
//                        map.put("customerKey", req.customerKey());
//                        map.put("amount", req.totalPayAmount());
//                        map.put("orderId", UUID.randomUUID().toString());
//                        map.put("orderName", "지난 달 정산 결제");
//
//                        HttpEntity<Map<String, Object>> request = new HttpEntity<>(map, headers);
//                        String url = "https://api.tosspayments.com/v1/billing/" + req.billingKey();
//
//                        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
                        
                        // 성공 기록
                        afterBilling.setIsPaid(true);
                        afterBilling.setIsSuccess(true);
                        System.out.println("결제 성공: " + req.storeIdx());
                        afterBillingRepository.save(afterBilling);
                        results.add(new SettlementResult(req.storeIdx(), req.totalPayAmount(), true, "정산 성공"));

                    } catch (Exception e) {
                        // API 호출 실패 시 에러 기록 후 예외를 던져서 Retry 유도
                        System.err.println("결제 API 에러 (재시도 대상): " + req.storeIdx() + " - " + e.getMessage());
                        afterBilling.setIsPaid(false);
                        afterBilling.setIsSuccess(false);
                        afterBilling.setFailReason("API 호출 실패: " + e.getMessage());
                        afterBillingRepository.save(afterBilling);

                        // 1회 결제 실패 테이블에 저장 (중복 체크)
                        String month = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                        if (!failedPaymentRepository.existsByStoreIdxAndPayedMonth(req.storeIdx(), month)) {
                            FailedPayment failedPayment = FailedPayment.builder()
                                    .storeIdx(req.storeIdx())
                                    .totalPayAmount(req.totalPayAmount())
                                    .payedMonth(month)
                                    .failReason(e.getMessage())
                                    .createdAt(LocalDateTime.now())
                                    .build();
                            failedPaymentRepository.save(failedPayment);
                        }
                        
                        // Retry 전략 시 중복 기록 방지를 위해 리스트에는 최종 실패(Skip 시점)나 성공 시점에만 담는 것이 좋으나,
                        // 여기서는 일단 담고 나중에 중복 제거 처리하거나 그대로 둠
                        results.add(new SettlementResult(req.storeIdx(), req.totalPayAmount(), false, "API 오류: " + e.getMessage()));
                        
                        // Exception을 던져야 Spring Batch가 Retry를 인식합니다.
                        throw e;
                    }
                }
            }
        };
    }
}