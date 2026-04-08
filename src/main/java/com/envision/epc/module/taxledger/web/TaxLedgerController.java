package com.envision.epc.module.taxledger.web;

import com.envision.epc.module.taxledger.application.command.ConfirmStageCommand;
import com.envision.epc.module.taxledger.application.command.CreateLedgerRunCommand;
import com.envision.epc.module.taxledger.application.dto.LedgerRunDetailDTO;
import com.envision.epc.module.taxledger.application.service.TaxLedgerService;
import com.envision.epc.module.taxledger.domain.TaxLedgerRun;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/ledger")
public class TaxLedgerController {
    private final TaxLedgerService ledgerService;

    @PostMapping("/runs")
    public LedgerRunDetailDTO createRun(@RequestBody CreateLedgerRunCommand command) {
        return ledgerService.createRun(command);
    }

    @GetMapping("/runs/{runId}")
    public LedgerRunDetailDTO runDetail(@PathVariable Long runId) {
        return ledgerService.getRunDetail(runId);
    }

    @PostMapping("/runs/{runId}/confirm")
    public LedgerRunDetailDTO confirm(@PathVariable Long runId, @RequestBody ConfirmStageCommand command) {
        return ledgerService.confirm(runId, command);
    }

    @GetMapping("/{companyCode}/{yearMonth}/runs")
    public List<TaxLedgerRun> listRuns(@PathVariable String companyCode, @PathVariable String yearMonth) {
        return ledgerService.listRuns(companyCode, yearMonth);
    }

    @GetMapping("/{companyCode}/{yearMonth}/download")
    public void download(@PathVariable String companyCode,
                         @PathVariable String yearMonth,
                         HttpServletResponse response) throws IOException {
        ledgerService.downloadFinalLedger(companyCode, yearMonth, response);
    }
}
