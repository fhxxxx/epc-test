package com.envision.epc.module.taxledger.application.service;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.facade.platform.PlatformRemote;
import com.envision.epc.module.extract.application.dtos.AccountingDocumentDTO;
import com.envision.epc.module.taxledger.application.command.DataLakePullCommand;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileSourceEnum;
import com.envision.epc.module.taxledger.domain.TaxFileRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaxDataLakeService {
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final PlatformRemote platformRemote;
    private final BlobStorageRemote blobStorageRemote;
    private final TaxPermissionService permissionService;
    private final TaxFileService fileService;

    @Value("${custom.platform.token.domain}")
    private String platformDomain;

    public List<TaxFileRecord> pull(DataLakePullCommand command) {
        permissionService.checkCompanyAccess(command.getCompanyCode());
        String reqUrl = platformDomain + String.format(
                "/api/finance/electronicArchives/%s/%s/%s/0/5000",
                command.getCompanyCode(), command.getFiscalYearPeriodStart(), command.getFiscalYearPeriodEnd());
        List<AccountingDocumentDTO> documents = platformRemote.fetchFromDataLake(
                "FINANCE_ELECTRONICARCHIVES_SVC", reqUrl, AccountingDocumentDTO::fromPltData);

        Map<FileCategoryEnum, List<AccountingDocumentDTO>> grouped = new EnumMap<>(FileCategoryEnum.class);
        grouped.put(FileCategoryEnum.DL_INCOME, new ArrayList<>());
        grouped.put(FileCategoryEnum.DL_OUTPUT, new ArrayList<>());
        grouped.put(FileCategoryEnum.DL_INPUT, new ArrayList<>());
        grouped.put(FileCategoryEnum.DL_INCOME_TAX, new ArrayList<>());
        grouped.put(FileCategoryEnum.DL_OTHER, new ArrayList<>());

        for (AccountingDocumentDTO dto : documents) {
            grouped.get(resolveCategory(dto.getAccount())).add(dto);
        }

        List<TaxFileRecord> records = new ArrayList<>();
        for (Map.Entry<FileCategoryEnum, List<AccountingDocumentDTO>> entry : grouped.entrySet()) {
            byte[] bytes = toCsv(entry.getValue()).getBytes(StandardCharsets.UTF_8);
            String blobPath = String.format("tax-ledger/%s/%s/%s/%s_%s.csv",
                    command.getCompanyCode(), command.getYearMonth(), entry.getKey().name(),
                    LocalDateTime.now().format(TS_FORMATTER), UUID.randomUUID());
            blobStorageRemote.upload(blobPath, new ByteArrayInputStream(bytes));
            TaxFileRecord record = fileService.saveOrReplace(
                    command.getCompanyCode(), command.getYearMonth(),
                    entry.getKey().name() + ".csv", entry.getKey(), FileSourceEnum.DATALAKE, blobPath, (long) bytes.length);
            records.add(record);
        }
        return records;
    }

    private static FileCategoryEnum resolveCategory(String account) {
        if (account == null) {
            return FileCategoryEnum.DL_OTHER;
        }
        if (account.startsWith("222101") || account.startsWith("222102")) {
            return FileCategoryEnum.DL_INPUT;
        }
        if (account.startsWith("6")) {
            return FileCategoryEnum.DL_INCOME;
        }
        if (account.startsWith("2221")) {
            return FileCategoryEnum.DL_OUTPUT;
        }
        if (account.startsWith("25")) {
            return FileCategoryEnum.DL_INCOME_TAX;
        }
        return FileCategoryEnum.DL_OTHER;
    }

    private static String toCsv(List<AccountingDocumentDTO> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("reference,fiscalYearPeriod,companyCode,account,debit,credit,itemText\n");
        for (AccountingDocumentDTO row : rows) {
            sb.append(safe(row.getReference())).append(",")
                    .append(safe(row.getFiscalYearPeriod())).append(",")
                    .append(safe(row.getCompanyCode())).append(",")
                    .append(safe(row.getAccount())).append(",")
                    .append(row.getDebitAmountInLocalCurrency() == null ? "" : row.getDebitAmountInLocalCurrency()).append(",")
                    .append(row.getCreditAmountInLocalCurrency() == null ? "" : row.getCreditAmountInLocalCurrency()).append(",")
                    .append(safe(row.getItemText())).append("\n");
        }
        return sb.toString();
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", " ");
    }
}
