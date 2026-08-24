package com.example.batchlab;

import lombok.RequiredArgsConstructor;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
public class MonthlyBillingScheduler {

    private final JobLauncher jobLauncher;
    private final Job settlementJob; // BillingBatch에 정의된 Job Bean 주입
    private final Job retryBillingJob; // FailedPaymentRetryBatch에 정의된 Job Bean 주입

    // 매달 10일 4시 0분 0초에 실행 (정기 결제)
//    @Scheduled(cron = "0 0 4 10 * *")
//    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)

    @PostMapping("/billing")
    public ResponseEntity<String> runBillingJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(settlementJob, jobParameters);
            return ResponseEntity.ok("정산 배치 요청 완료 - 상태: " + jobExecution.getStatus());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("정산 배치 실행 실패: " + e.getMessage());
        }
    }


    public void executeMonthlyBilling() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("requestDate", LocalDateTime.now().toString())
                    .toJobParameters();

            System.out.println("--- 월 정산 배치 시작 ---");
            jobLauncher.run(settlementJob, jobParameters);
            System.out.println("--- 월 정산 배치 종료 ---");

        } catch (Exception e) {
            System.err.println("배치 실행 실패: " + e.getMessage());
        }
    }

    // 매달 11일 4시 0분 0초에 실행 (실패 건 재시도)
    @Scheduled(cron = "0 0 4 11 * *")
    public void executeRetryBilling() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("requestDate", LocalDateTime.now().toString())
                    .toJobParameters();

            System.out.println("--- 월 정산 재시도 배치 시작 ---");
            jobLauncher.run(retryBillingJob, jobParameters);
            System.out.println("--- 월 정산 재시도 배치 종료 ---");

        } catch (Exception e) {
            System.err.println("재시도 배치 실행 실패: " + e.getMessage());
        }
    }
}