package com.nitor.nauction

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@Configuration
@EnableScheduling
class ScheduledTasks(private val bidService: BidService) {

    @Scheduled(cron = "0 1 0 * * *", zone = "EET")
    fun renewExpiredAuctions() {
        log.info { "Task executed at ${LocalDateTime.now()}" }
        bidService.renewExpiredAuctions()
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "EET")
    fun exportFinishedAuctions() {
        log.info { "Task executed at ${LocalDateTime.now()}" }
        bidService.exportFinishedAuctions()
    }
}