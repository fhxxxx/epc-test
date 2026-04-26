package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.module.taxledger.application.dto.BsAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.DlInputParsedDTO;
import com.envision.epc.module.taxledger.application.dto.DlOtherParsedDTO;
import com.envision.epc.module.taxledger.application.dto.DlOutputParsedDTO;
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
        assertEquals(ParsedResultTypeCatalog.Shape.OBJECT, dlIncome.shape());
        assertEquals(Object.class, dlIncome.valueType());

        ParsedResultTypeCatalog.Entry dlOutput = ParsedResultTypeCatalog.get(FileCategoryEnum.DL_OUTPUT);
        assertNotNull(dlOutput);
        assertEquals(ParsedResultTypeCatalog.Shape.OBJECT, dlOutput.shape());
        assertEquals(DlOutputParsedDTO.class, dlOutput.valueType());

        ParsedResultTypeCatalog.Entry dlInput = ParsedResultTypeCatalog.get(FileCategoryEnum.DL_INPUT);
        assertNotNull(dlInput);
        assertEquals(ParsedResultTypeCatalog.Shape.OBJECT, dlInput.shape());
        assertEquals(DlInputParsedDTO.class, dlInput.valueType());

        ParsedResultTypeCatalog.Entry dlIncomeTax = ParsedResultTypeCatalog.get(FileCategoryEnum.DL_INCOME_TAX);
        assertNotNull(dlIncomeTax);
        assertEquals(ParsedResultTypeCatalog.Shape.OBJECT, dlIncomeTax.shape());
        assertEquals(Object.class, dlIncomeTax.valueType());

        ParsedResultTypeCatalog.Entry dlOther = ParsedResultTypeCatalog.get(FileCategoryEnum.DL_OTHER);
        assertNotNull(dlOther);
        assertEquals(ParsedResultTypeCatalog.Shape.OBJECT, dlOther.shape());
        assertEquals(DlOtherParsedDTO.class, dlOther.valueType());
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
