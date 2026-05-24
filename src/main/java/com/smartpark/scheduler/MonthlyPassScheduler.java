package com.smartpark.scheduler;

import com.smartpark.model.MonthlyPass;
import com.smartpark.repository.MonthlyPassRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Tự động mark EXPIRED cho thẻ tháng đã hết hạn.
 * Chạy mỗi ngày lúc 00:05.
 */
@Component
public class MonthlyPassScheduler {

    private final MonthlyPassRepository repo;

    public MonthlyPassScheduler(MonthlyPassRepository repo) {
        this.repo = repo;
    }

    /**
     * Chạy mỗi ngày lúc 00:05 sáng.
     * Tìm tất cả thẻ ACTIVE có endDate < hôm nay → set EXPIRED.
     */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void markExpiredPasses() {
        List<MonthlyPass> expired =
                repo.findActiveButExpired(LocalDate.now());

        if (expired.isEmpty()) return;

        expired.forEach(p -> p.setStatus("EXPIRED"));
        repo.saveAll(expired);

        System.out.printf("[SCHEDULER] ✅ Đã mark %d thẻ tháng EXPIRED (endDate < %s)%n",
                expired.size(), LocalDate.now());
    }
}
