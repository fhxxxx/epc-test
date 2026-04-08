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

/**
 * 台账运行接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/ledger")
public class TaxLedgerController {
    private final TaxLedgerService ledgerService;

    /**
     * 创建运行
     */
    @PostMapping("/runs")
    public LedgerRunDetailDTO createRun(@RequestBody CreateLedgerRunCommand command) {
        return ledgerService.createRun(command);
    }

    /**
     * 查询运行详情
     */
    @GetMapping("/runs/{runId}")
    public LedgerRunDetailDTO runDetail(@PathVariable Long runId) {
        return ledgerService.getRunDetail(runId);
    }

    /**
     * 人工确认批次（GATED模式）
     */
    @PostMapping("/runs/{runId}/confirm")
    public LedgerRunDetailDTO confirm(@PathVariable Long runId, @RequestBody ConfirmStageCommand command) {
        return ledgerService.confirm(runId, command);
    }

    /**
     * 查询某公司某账期运行历史
     */
    @GetMapping("/{companyCode}/{yearMonth}/runs")
    public List<TaxLedgerRun> listRuns(@PathVariable String companyCode, @PathVariable String yearMonth) {
        return ledgerService.listRuns(companyCode, yearMonth);
    }

    /**
     * 下载最终台账
     */
    @GetMapping("/{companyCode}/{yearMonth}/download")
    public void download(@PathVariable String companyCode,
                         @PathVariable String yearMonth,
                         HttpServletResponse response) throws IOException {
        ledgerService.downloadFinalLedger(companyCode, yearMonth, response);
    }
}
