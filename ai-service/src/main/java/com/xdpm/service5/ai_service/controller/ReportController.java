package com.xdpm.service5.ai_service.controller;

import com.xdpm.service5.ai_service.dto.ReportResponse;
import com.xdpm.service5.ai_service.service.ReportService;
import com.xdpm.service5.ai_service.scheduler.DataFetchScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Slf4j
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final DataFetchScheduler dataFetchScheduler;

    public ReportController(ReportService reportService, DataFetchScheduler dataFetchScheduler) {
        this.reportService = reportService;
        this.dataFetchScheduler = dataFetchScheduler;
    }

    /**
     * Lấy daily report theo ngày
     * @param date Ngày cần lấy report (format: yyyy-MM-dd). Nếu không có thì lấy hôm nay
     * @return ReportResponse hoặc 404 nếu không tìm thấy
     */
    @GetMapping(value = "/daily", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ReportResponse> getDailyReport(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        ZonedDateTime targetDate = date != null ? date.atStartOfDay(java.time.ZoneId.systemDefault()) : ZonedDateTime.now();
        log.debug("Getting daily report for date: {}", targetDate);
        
        return reportService.getDailyReport(targetDate)
                .switchIfEmpty(Mono.error(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        String.format("Report not found for date: %s", date != null ? date : LocalDate.now()))));
    }

    /**
     * Trigger scheduler thủ công để tạo report cho hôm nay
     * @return ReportResponse của report vừa tạo
     */
    @PostMapping(value = "/daily/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ReportResponse> generateDailyReport() {
        log.info("Manual trigger: Generating daily report for today");
        
        // Chạy scheduler trong background thread
        return Mono.fromRunnable(() -> dataFetchScheduler.runDaily())
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .delayElement(java.time.Duration.ofSeconds(3))
                .then(reportService.getDailyReport(ZonedDateTime.now()))
                .switchIfEmpty(Mono.error(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to generate report. Check logs for details.")));
    }
}

