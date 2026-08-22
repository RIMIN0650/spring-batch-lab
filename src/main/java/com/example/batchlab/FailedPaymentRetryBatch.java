package com.example.batchlab;

import com.example.batchlab.model.Billing;
import com.example.batchlab.model.FailedPayment;
import com.example.batchlab.repository.AfterBillingRepository;
import com.example.batchlab.repository.BillingRepository;
import com.example.batchlab.repository.FailedPaymentRepository;
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
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiredArgsConstructor
@Configuration
public class FailedPaymentRetryBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final FailedPaymentRepository failedPaymentRepository;
    private final BillingRepository billingRepository;
    private final AfterBillingRepository afterBillingRepository;

    @Value("${billing.secret-key}")
    private String secretKey;

    private static final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/1507205093588598854/R0K4NtHWlK5MU9rLs5hWq8urwlundf8GTXsKqCKMr3PuHvpP3mj4Soneg0ZPXP4VdM11";

    public record RetryRequestDto(
            Long failedPaymentIdx,
            Long storeIdx,
            int amount,
            String customerKey,
            String billingKey,
            String payedMonth
    ) {}

    public record RetryResult(
            Long storeIdx,
            int amount,
            boolean isSuccess,
            String reason
    ) {}

    private final List<RetryResult> results = Collections.synchronizedList(new ArrayList<>());

    @Bean
    public Job retryBillingJob() {
        return new JobBuilder("retryBillingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(retryBillingStep())
                .listener(retryJobListener())
                .build();
    }

    @Bean
    public JobExecutionListener retryJobListener() {
        return new JobExecutionListener() {
            private final RestTemplate restTemplate = new RestTemplate();

            @Override
            public void beforeJob(JobExecution jobExecution) {
                results.clear();
                sendDiscordMessage("[재시도 정산 배치] 작업 시작 - 일시: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                String status = jobExecution.getStatus().toString();
                String startTime = jobExecution.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String endTime = jobExecution.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                long successCount = results.stream().filter(RetryResult::isSuccess).count();
                long failCount = results.stream().filter(r -> !r.isSuccess()).count();

                StringBuilder sb = new StringBuilder();
                sb.append("[재시도 정산 배치] 작업 종료\n");
                sb.append("- 상태: ").append(status).append("\n");
                sb.append("- 시작 시간: ").append(startTime).append("\n");
                sb.append("- 종료 시간: ").append(endTime).append("\n");
                sb.append("- 총 재시도 건수: ").append(results.size()).append("\n");
                sb.append("- 성공: ").append(successCount).append("\n");
                sb.append("- 실패: ").append(failCount).append("\n");
                
                sendDiscordMessage(sb.toString());
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
        };
    }

    @Bean
    public Step retryBillingStep() {
        return new StepBuilder("retryBillingStep", jobRepository)
                .<FailedPayment, RetryRequestDto>chunk(5, platformTransactionManager)
                .reader(failedPaymentReader())
                .processor(retryProcessor())
                .writer(retryWriter())
                .build();
    }

    @Bean
    public ItemReader<FailedPayment> failedPaymentReader() {
        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return new RepositoryItemReaderBuilder<FailedPayment>()
                .name("failedPaymentReader")
                .pageSize(5)
                .methodName("findByPayedMonth")
                .arguments(Collections.singletonList(currentMonth))
                .repository(failedPaymentRepository)
                .sorts(Map.of("idx", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<FailedPayment, RetryRequestDto> retryProcessor() {
        return failedPayment -> {
            Billing billing = billingRepository.findByStoreIdx(failedPayment.getStoreIdx());
            if (billing == null) {
                results.add(new RetryResult(failedPayment.getStoreIdx(), failedPayment.getTotalPayAmount(), false, "결제 정보 없음"));
                return null;
            }

            return new RetryRequestDto(
                    failedPayment.getIdx(),
                    failedPayment.getStoreIdx(),
                    failedPayment.getTotalPayAmount(),
                    billing.getCustomerKey(),
                    billing.getBillingKey(),
                    failedPayment.getPayedMonth()
            );
        };
    }

    @Bean
    public ItemWriter<RetryRequestDto> retryWriter() {
        return new ItemWriter<RetryRequestDto>() {
            private final RestTemplate restTemplate = new RestTemplate();
            private final HttpHeaders headers = new HttpHeaders();

            {
                String encodedAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
                headers.set("Authorization", "Basic " + encodedAuth);
                headers.setContentType(MediaType.APPLICATION_JSON);
            }

            @Override
            public void write(Chunk<? extends RetryRequestDto> chunk) throws Exception {
                for (RetryRequestDto req : chunk) {
                    try {
                        Map<String, Object> map = new HashMap<>();
                        map.put("customerKey", req.customerKey());
                        map.put("amount", req.amount());
                        map.put("orderId", UUID.randomUUID().toString());
                        map.put("orderName", "정산 결제 재시도");

                        HttpEntity<Map<String, Object>> request = new HttpEntity<>(map, headers);
                        String url = "https://api.tosspayments.com/v1/billing/" + req.billingKey();

                        restTemplate.postForEntity(url, request, String.class);

                        // 성공 처리
                        // 1. AfterBilling 테이블 업데이트
                        afterBillingRepository.findByStoreIdxAndPayedMonth(req.storeIdx(), req.payedMonth())
                                .ifPresent(afterBilling -> {
                                    afterBilling.setIsPaid(true);
                                    afterBilling.setIsSuccess(true);
                                    afterBilling.setIsRetryFailed(false);
                                    afterBilling.setFailReason(null);
                                    afterBillingRepository.save(afterBilling);
                                });

                        // 2. FailedPayment 테이블에서 삭제 (재시도 성공했으므로)
                        failedPaymentRepository.deleteById(req.failedPaymentIdx());

                        results.add(new RetryResult(req.storeIdx(), req.amount(), true, "재시도 성공"));

                    } catch (Exception e) {
                        // 또 실패한 경우
                        results.add(new RetryResult(req.storeIdx(), req.amount(), false, "재시도 실패: " + e.getMessage()));
                        
                        // AfterBilling 에 실패 사유 및 재시도 실패 플래그 갱신
                        afterBillingRepository.findByStoreIdxAndPayedMonth(req.storeIdx(), req.payedMonth())
                                .ifPresent(afterBilling -> {
                                    afterBilling.setFailReason("재시도 실패: " + e.getMessage());
                                    afterBilling.setIsRetryFailed(true);
                                    afterBillingRepository.save(afterBilling);
                                });
                    }
                }
            }
        };
    }
}
