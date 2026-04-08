package com.envision.epc.module.taxledger.web;

import com.envision.epc.module.taxledger.application.command.GrantPermissionCommand;
import com.envision.epc.module.taxledger.application.service.TaxPermissionService;
import com.envision.epc.module.taxledger.domain.TaxUserPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/permissions")
public class TaxPermissionController {
    private final TaxPermissionService permissionService;

    @GetMapping
    public List<TaxUserPermission> list(@RequestParam(required = false) String companyCode) {
        return permissionService.listByCompany(companyCode);
    }

    @PostMapping
    public TaxUserPermission grant(@RequestBody GrantPermissionCommand command) {
        return permissionService.grant(command);
    }

    @DeleteMapping
    public void revoke(@RequestParam String employeeId, @RequestParam(required = false) String companyCode) {
        permissionService.revoke(employeeId, companyCode);
    }
}
