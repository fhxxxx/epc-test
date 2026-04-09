package com.envision.epc.module.taxledger.web;

import com.envision.epc.module.taxledger.application.command.DataLakePullCommand;
import com.envision.epc.module.taxledger.application.dto.DataLakeBatchPullResultDTO;
import com.envision.epc.module.taxledger.application.service.DataLakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据湖接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/datalake")
public class DataLakeController {
    private final DataLakeService dataLakeService;

    /**
     * 批量拉取数据湖数据并生成 DL 分类文件
     */
    @PostMapping("/pull")
    public DataLakeBatchPullResultDTO pull(@RequestBody DataLakePullCommand command) {
        return dataLakeService.pull(command);
    }
}