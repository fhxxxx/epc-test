package com.envision.epc.module.taxledger.web;

import com.envision.epc.module.taxledger.application.command.DataLakePullCommand;
import com.envision.epc.module.taxledger.application.service.DataLakeService;
import com.envision.epc.module.taxledger.domain.FileRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据湖接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/datalake")
public class DataLakeController {
    private final DataLakeService dataLakeService;

    /**
     * 拉取数据湖数据并生成DL分类文件
     */
    @PostMapping("/pull")
    public List<FileRecord> pull(@RequestBody DataLakePullCommand command) {
        return dataLakeService.pull(command);
    }
}
