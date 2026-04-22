package com.envision.epc.module.taxledger.application.dto;

import com.envision.epc.module.taxledger.domain.LedgerJob;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 台账任务列表
 */
@Data
public class LedgerJobListDTO {
    private List<LedgerJob> items;
    private long total;
    private int page;
    private int size;

    public static LedgerJobListDTO of(List<LedgerJob> items, long total, int page, int size) {
        LedgerJobListDTO dto = new LedgerJobListDTO();
        dto.setItems(items == null ? Collections.emptyList() : items);
        dto.setTotal(total);
        dto.setPage(page);
        dto.setSize(size);
        return dto;
    }
}
