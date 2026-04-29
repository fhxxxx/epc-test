package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.dto.SummarySheetDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerConfigSnapshot;
import com.envision.epc.module.taxledger.application.ledger.data.SummaryLedgerSheetData;
import com.envision.epc.module.taxledger.domain.TaxCategoryConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryLedgerSheetDataBuilderTest {

    private final SummaryLedgerSheetDataBuilder builder = new SummaryLedgerSheetDataBuilder();

    @Test
    void shouldSortSeqNoWithDecimalBeforeIntegerWithinSameIntegerPart() {
        LedgerBuildContext ctx = buildContext(List.of(
                cfg("1.1", "买卖合同"),
                cfg("1.2", "融资租赁合同"),
                cfg("1.3", "借款合同"),
                cfg("1.8", "运输合同"),
                cfg("1.9", "技术合同"),
                cfg("1.10", "租赁合同-租房"),
                cfg("1.15", "营业账簿"),
                cfg("1", "印花税合计"),
                cfg("2.1", "销项税额-主营业务收入"),
                cfg("2", "增值税")
        ));

        SummaryLedgerSheetData data = builder.build(ctx);
        List<String> orderedTaxItems = data.getSummary().getStampDutyRows()
                .stream()
                .map(SummarySheetDTO.StampDutyItem::getTaxItem)
                .collect(Collectors.toList());

        assertEquals(List.of(
                "买卖合同",
                "融资租赁合同",
                "借款合同",
                "运输合同",
                "技术合同",
                "租赁合同-租房",
                "营业账簿",
                "印花税合计",
                "销项税额-主营业务收入",
                "增值税"
        ), orderedTaxItems);
    }

    @Test
    void shouldTreatPointZeroAsIntegerAndPlaceInvalidSeqAtTail() {
        LedgerBuildContext ctx = buildContext(List.of(
                cfg("1.2", "a-decimal"),
                cfg("1.0", "b-point-zero"),
                cfg("1", "c-integer"),
                cfg("", "d-empty"),
                cfg("abc", "e-invalid")
        ));

        SummaryLedgerSheetData data = builder.build(ctx);
        List<String> orderedTaxItems = data.getSummary().getStampDutyRows()
                .stream()
                .map(SummarySheetDTO.StampDutyItem::getTaxItem)
                .collect(Collectors.toList());

        assertEquals(List.of(
                "a-decimal",
                "c-integer",
                "b-point-zero",
                "d-empty",
                "e-invalid"
        ), orderedTaxItems);
    }

    @Test
    void shouldSplitVatRowsFromCommonRows() {
        LedgerBuildContext ctx = buildContext(List.of(
                cfg("2.1", "增值税", "销项税额-主营业务收入"),
                cfg("2.2", "增值税", "销项税额-固定资产处置"),
                cfg("3", "城建税", "城建税"),
                cfg("4", "房产税", "房产税")
        ));

        SummaryLedgerSheetData data = builder.build(ctx);

        List<SummarySheetDTO.CommonTaxItem> vatRows = data.getSummary().getVatTaxRows();
        List<SummarySheetDTO.CommonTaxItem> commonRows = data.getSummary().getCommonTaxRows();

        assertEquals(2, vatRows.size());
        assertEquals(2, commonRows.size());
        assertTrue(vatRows.stream().allMatch(row -> row.getTaxType().contains("增值税")));
        assertTrue(commonRows.stream().noneMatch(row -> row.getTaxType().contains("增值税")));
    }

    private LedgerBuildContext buildContext(List<TaxCategoryConfig> taxConfigs) {
        LedgerConfigSnapshot snapshot = LedgerConfigSnapshot.builder()
                .companyCode("2320")
                .taxCategoryConfigs(taxConfigs)
                .projectConfigs(List.of())
                .build();

        return LedgerBuildContext.builder()
                .companyCode("2320")
                .yearMonth("2025-12")
                .files(List.of())
                .traceId("test-trace")
                .operator("tester")
                .configSnapshot(snapshot)
                .preloadedParsedData(Map.of())
                .preloadSummary(new LinkedHashMap<>())
                .builtSheetDataMap(new LinkedHashMap<>())
                .build();
    }

    private TaxCategoryConfig cfg(String seqNo, String taxCategory) {
        return cfg(seqNo, "印花税", taxCategory);
    }

    private TaxCategoryConfig cfg(String seqNo, String taxType, String taxCategory) {
        TaxCategoryConfig cfg = new TaxCategoryConfig();
        cfg.setCompanyCode("2320");
        cfg.setSeqNo(seqNo);
        cfg.setTaxType(taxType);
        cfg.setTaxCategory(taxCategory);
        return cfg;
    }
}
