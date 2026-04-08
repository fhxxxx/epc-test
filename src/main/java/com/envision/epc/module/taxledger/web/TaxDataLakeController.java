package com.envision.epc.module.taxledger.web;

import com.envision.epc.module.taxledger.application.command.DataLakePullCommand;
import com.envision.epc.module.taxledger.application.service.TaxDataLakeService;
import com.envision.epc.module.taxledger.domain.TaxFileRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/datalake")
public class TaxDataLakeController {
    private final TaxDataLakeService dataLakeService;

    @PostMapping("/pull")
    public List<TaxFileRecord> pull(@RequestBody DataLakePullCommand command) {
        return dataLakeService.pull(command);
    }
}
