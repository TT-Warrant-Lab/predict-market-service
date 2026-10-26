package com.fadx.predict_market_service.batch.scheduler;

import com.fadx.predict_market_service.modules.common.services.StockTradingService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockDataLoadScheduler extends ThreadPoolTaskScheduler {

  @Autowired private StockTradingService stockTradingService;

  @PostConstruct
  public void init() {
    this.setPoolSize(1); // Only one thread needed for this task
    this.setThreadNamePrefix("Scheduler-StockDataLoad");
    this.initialize();

    // Schedule to run at 15:10 (3:10 PM) from Monday to Friday
    // Cron expression: minute hour dayOfMonth month dayOfWeek
    // 0 10 15 ? * MON-FRI
    this.schedule(
        () -> {
          log.info("Starting StockDataLoadScheduler task...");
          stockTradingService.loadAllData();
          log.info("StockDataLoadScheduler task completed.");
        },
        new CronTrigger("0 10 15 ? * MON-FRI"));
  }
}
