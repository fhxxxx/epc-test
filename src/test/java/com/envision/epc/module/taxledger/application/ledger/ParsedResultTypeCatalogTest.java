package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.module.taxledger.application.dto.BsAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.DatalakeExportRowDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.VatOutputSheetUploadDTO;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsedResultTypeCatalogTest {

    @Test
    void shouldDefineCriticalCategories() {
        ParsedResultTypeCatalog.Entry bsAppendix = ParsedResultTypeCatalog.get(FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE);
        assertNotNull(bsAppendix);
        assertEquals(ParsedResultTypeCatalog.Shape.LIST, bsAppendix.shape());
        assertEquals(BsAppendixUploadDTO.class, bsAppendix.valueType());

        ParsedResultTypeCatalog.Entry vatOutput = ParsedResultTypeCatalog.get(FileCategoryEnum.VAT_OUTPUT);
        assertNotNull(vatOutput);
        assertEquals(ParsedResultTypeCatalog.Shape.OBJECT, vatOutput.shape());
        assertEquals(VatOutputSheetUploadDTO.class, vatOutput.valueType());

        ParsedResultTypeCatalog.Entry vatChangeAppendix = ParsedResultTypeCatalog.get(FileCategoryEnum.VAT_CHANGE_APPENDIX);
        assertNotNull(vatChangeAppendix);
        assertEquals(ParsedResultTypeCatalog.Shape.OBJECT, vatChangeAppendix.shape());
        assertEquals(VatChangeAppendixUploadDTO.class, vatChangeAppendix.valueType());

        ParsedResultTypeCatalog.Entry dlIncome = ParsedResultTypeCatalog.get(FileCategoryEnum.DL_INCOME);
        assertNotNull(dlIncome);
        assertEquals(ParsedResultTypeCatalog.Shape.LIST, dlIncome.shape());
        assertEquals(DatalakeExportRowDTO.class, dlIncome.valueType());
    }

    @Test
    void shouldContainAllDatalakeCategories() {
        assertTrue(ParsedResultTypeCatalog.supports(FileCategoryEnum.DL_INCOME));
        assertTrue(ParsedResultTypeCatalog.supports(FileCategoryEnum.DL_OUTPUT));
        assertTrue(ParsedResultTypeCatalog.supports(FileCategoryEnum.DL_INPUT));
        assertTrue(ParsedResultTypeCatalog.supports(FileCategoryEnum.DL_INCOME_TAX));
        assertTrue(ParsedResultTypeCatalog.supports(FileCategoryEnum.DL_OTHER));
    }
}
