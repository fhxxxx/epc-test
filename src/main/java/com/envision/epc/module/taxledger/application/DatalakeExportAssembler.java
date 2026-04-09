package com.envision.epc.module.taxledger.application;

import com.envision.epc.module.taxledger.application.dto.DatalakeDTO;
import com.envision.epc.module.taxledger.application.dto.DatalakeExportRowDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datalake export row assembler based on MapStruct.
 */
@Mapper(componentModel = "spring")
public abstract class DatalakeExportAssembler {

    @Mapping(target = "localAmount", expression = "java(toSignedAmount(dto.getDebitCreditIndicator(), dto.getDebitAmountInLocalCurrency(), dto.getCreditAmountInLocalCurrency()))")
    @Mapping(target = "documentAmount", expression = "java(toSignedAmount(dto.getDebitCreditIndicator(), dto.getDebitAmountInDocumentCurrency(), dto.getCreditAmountInDocumentCurrency()))")
    public abstract DatalakeExportRowDTO toExportRow(DatalakeDTO dto);

    public abstract List<DatalakeExportRowDTO> toExportRows(List<DatalakeDTO> rows);

    protected String toSignedAmount(String debitCreditIndicator, BigDecimal debitAmount, BigDecimal creditAmount) {
        if ("S".equalsIgnoreCase(debitCreditIndicator)) {
            return toPlainString(debitAmount);
        }
        if ("H".equalsIgnoreCase(debitCreditIndicator)) {
            return creditAmount == null ? "" : creditAmount.negate().stripTrailingZeros().toPlainString();
        }
        return "";
    }

    private String toPlainString(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}

