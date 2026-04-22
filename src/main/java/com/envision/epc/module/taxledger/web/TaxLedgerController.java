package com.envision.epc.module.taxledger.web;

import com.envision.epc.module.taxledger.application.command.CreateLedgerJobCommand;
import com.envision.epc.module.taxledger.application.dto.LedgerJobListDTO;
import com.envision.epc.module.taxledger.application.service.LedgerJobService;
import com.envision.epc.module.taxledger.domain.LedgerJob;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 台账任务接口（单按钮任务流）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/ledger")
public class TaxLedgerController {
    private final LedgerJobService ledgerJobService;

    @PostMapping("/jobs")
    public LedgerJob createJob(@RequestBody CreateLedgerJobCommand command) {
        return ledgerJobService.createJob(command);
    }

    @GetMapping("/jobs")
    public LedgerJobListDTO listJobs(@RequestParam(required = false) String companyCode,
                                     @RequestParam(required = false) String yearMonth,
                                     @RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size) {
        return ledgerJobService.list(companyCode, yearMonth, page, size);
    }

    @GetMapping("/jobs/{jobId}")
    public LedgerJob jobDetail(@PathVariable Long jobId) {
        return ledgerJobService.detail(jobId);
    }

    @PostMapping("/jobs/{jobId}/retry")
    public LedgerJob retry(@PathVariable Long jobId) {
        return ledgerJobService.retry(jobId);
    }

    @GetMapping("/jobs/{jobId}/download")
    public void download(@PathVariable Long jobId, HttpServletResponse response) throws IOException {
        ledgerJobService.download(jobId, response);
    }
}
