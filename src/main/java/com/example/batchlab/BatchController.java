package com.example.batchlab;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job settlementJob;
//    private final Job retryBillingJob;

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

}
