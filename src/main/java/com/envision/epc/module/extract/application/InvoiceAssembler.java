package com.envision.epc.module.extract.application;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.envision.epc.module.extract.application.command.DataLakeCompareCommand;
import com.envision.epc.module.extract.application.command.TaxBureauCompareCommand;
import com.envision.epc.module.extract.application.dtos.*;
import com.envision.epc.module.extract.domain.CompareRun;
import com.envision.epc.module.extract.domain.CompareRunDetail;
import com.envision.epc.module.extract.domain.CompareRunStatusEnum;
import com.envision.epc.module.uploadfile.domain.UploadFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wenjun.gu
 * @since 2025/8/15-11:59
 */
@Mapper(componentModel = "spring")
public abstract class InvoiceAssembler {

    @Mapping(target = "invoiceId", source = "dataLakeExportDTO.invoiceId")
    @Mapping(target = "taxAmount", source = "dataLakeExportDTO.taxAmount")
    @Mapping(target = "RInvoiceId", source = "resultExportDTO.invoiceId")
    @Mapping(target = "RTaxAmount", source = "resultExportDTO.taxAmount")
    @Mapping(target = "RInvoiceCode", source = "resultExportDTO.invoiceCode")
    @Mapping(target = "RVendorTaxId", source = "resultExportDTO.vendorTaxId")
    @Mapping(target = "RVendorName", source = "resultExportDTO.vendorName")
    @Mapping(target = "RSubTotal", source = "resultExportDTO.subTotal")
    public abstract ResultDataLakeCompareDTO toResultDataLakeCompareDTO(DataLakeExportDTO dataLakeExportDTO, ResultExportDTO resultExportDTO, String matchSuccess);

    @Mapping(target = "projectId", source = "dataLakeCompareCommand.projectId")
    @Mapping(target = "type", expression = "java(com.envision.epc.module.extract.domain.CompareTypeEnum.DATALAKECOMPARE)")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "error", source = "error")
    public abstract CompareRun toDataLakeCompareRun(DataLakeCompareCommand dataLakeCompareCommand, CompareRunStatusEnum status, Map<String, String> compareResult,
                                                    String error, List<String> resultFileNames, List<String> dataLakeFileNames);

    @Mapping(target = "projectId", source = "compareCommand.projectId")
    @Mapping(target = "type", expression = "java(com.envision.epc.module.extract.domain.CompareTypeEnum.TAXBUREAUCOMPARE)")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "error", source = "error")
    public abstract CompareRun toTaxBureauCompareRun(TaxBureauCompareCommand compareCommand, CompareRunStatusEnum status, Map<String, String> compareResult,
                                                     String error, List<String> dataLakeCompareFileNames, List<String> taxBureauFileNames);


    @Mapping(target = "compareRunId", source = "compareRunId")
    @Mapping(target = "companyCode", source = "companyCode")
    @Mapping(target = "resultFileIds", expression = "java(toFileIdList(resultFiles))")
    @Mapping(target = "resultFileNames", expression = "java(toFileNameList(resultFiles))")
    @Mapping(target = "dataLakeFileIds", expression = "java(toFileIdList(dataLakeFiles))")
    @Mapping(target = "dataLakeFileNames", expression = "java(toFileNameList(dataLakeFiles))")
    @Mapping(target = "dataLakeCompareFileIds", expression = "java(java.util.Collections.emptyList())")
    @Mapping(target = "dataLakeCompareFileNames", expression = "java(java.util.Collections.emptyList())")
    @Mapping(target = "taxBureauFileIds", expression = "java(java.util.Collections.emptyList())")
    @Mapping(target = "taxBureauFileNames", expression = "java(java.util.Collections.emptyList())")
    @Mapping(target = "compareResult", expression = "java(null)")
    public abstract CompareRunDetail toDatalakeCompareRunDetail(Long compareRunId, String companyCode,
                                                                List<UploadFile> resultFiles, List<UploadFile> dataLakeFiles);

    @Mapping(target = "compareRunId", source = "compareRunId")
    @Mapping(target = "companyCode", source = "companyCode")
    @Mapping(target = "resultFileIds", expression = "java(java.util.Collections.emptyList())")
    @Mapping(target = "resultFileNames", expression = "java(java.util.Collections.emptyList())")
    @Mapping(target = "dataLakeFileIds", expression = "java(java.util.Collections.emptyList())")
    @Mapping(target = "dataLakeFileNames", expression = "java(java.util.Collections.emptyList())")
    @Mapping(target = "dataLakeCompareFileIds", expression = "java(toFileIdList(dataLakeCompareFiles))")
    @Mapping(target = "dataLakeCompareFileNames", expression = "java(toFileNameList(dataLakeCompareFiles))")
    @Mapping(target = "taxBureauFileIds", expression = "java(toFileIdList(taxBureauFiles))")
    @Mapping(target = "taxBureauFileNames", expression = "java(toFileNameList(taxBureauFiles))")
    @Mapping(target = "compareResult", expression = "java(null)")
    public abstract CompareRunDetail toTaxBureauCompareRunDetail(Long compareRunId, String companyCode,
                                                                List<UploadFile> dataLakeCompareFiles, List<UploadFile> taxBureauFiles);

    @Mapping(target = "id", source = "compareRun.id")
    @Mapping(target = "createTime", source = "compareRun.createTime")
    @Mapping(target = "createBy", source = "compareRun.createBy")
    @Mapping(target = "createByName", source = "compareRun.createByName")
    public abstract CompareRunDTO toCompareRunDTO(CompareRun compareRun);

    @Mapping(target = "id", source = "compareRun.id")
    @Mapping(target = "projectId", source = "compareRun.projectId")
    @Mapping(target = "type", source = "compareRun.type")
    @Mapping(target = "status", source = "compareRun.status")
    @Mapping(target = "error", source = "compareRun.error")
    @Mapping(target = "createTime", source = "compareRun.createTime")
    @Mapping(target = "createBy", source = "compareRun.createBy")
    @Mapping(target = "createByName", source = "compareRun.createByName")
    @Mapping(target = "resultFileNames", expression = "java(aggregateFileNames(details, CompareRunDetail::getResultFileNames))")
    @Mapping(target = "dataLakeFileNames", expression = "java(aggregateFileNames(details, CompareRunDetail::getDataLakeFileNames))")
    @Mapping(target = "dataLakeCompareFileNames", expression = "java(aggregateFileNames(details, CompareRunDetail::getDataLakeCompareFileNames))")
    @Mapping(target = "taxBureauFileNames", expression = "java(aggregateFileNames(details, CompareRunDetail::getTaxBureauFileNames))")
    @Mapping(target = "companyCodeList", expression = "java(aggregateCompanyCodes(details))")
    public abstract CompareRunDTO toCompareRunDTO(CompareRun compareRun, List<CompareRunDetail> details);

    protected List<Long> toFileIdList(List<UploadFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        return files.stream().map(UploadFile::getId).toList();
    }

    protected List<String> toFileNameList(List<UploadFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        return files.stream().map(UploadFile::getName).toList();
    }

    /**
     * 通用方法：汇总文件名称列表
     * 
     * @param details 详情列表
     * @param fieldExtractor 字段提取器，从 CompareRunDetail 中提取 List<String> 字段
     * @return 文件名称列表
     */
    protected List<String> aggregateFileNames(List<CompareRunDetail> details, Function<CompareRunDetail, List<String>> fieldExtractor) {
        if (CollUtil.isEmpty(details)) {
            return Collections.emptyList();
        }
        return details.stream().map(fieldExtractor).filter(CollUtil::isNotEmpty).flatMap(List::stream).toList();
    }

    /**
     * 汇总公司代码列表
     */
    protected List<String> aggregateCompanyCodes(List<CompareRunDetail> details) {
        if (CollUtil.isEmpty(details)) {
            return Collections.emptyList();
        }
        return details.stream().map(CompareRunDetail::getCompanyCode).filter(CharSequenceUtil::isNotBlank).distinct().toList();
    }


    public List<DataLakeExportDTO> toDataLakeExportDTOS(List<AccountingDocumentDTO> documentDTOList) {
        List<DataLakeExportDTO> resultList = new ArrayList<>();
        List<String> invoiceIdList = new ArrayList<>();

        documentDTOList.forEach(documentDTO -> {
            String invoiceId = "";
            //取发票号 贸易伙伴字段，如果该字段非空的话，发票号就在文本里取，如果为空，那还是要有那套逻辑从文本和参照里提取发票号
            if (CharSequenceUtil.isNotBlank(documentDTO.getCompanyIdOfTradingPartner())) {
                invoiceId = getInvoiceId(documentDTO.getItemText());
            } else {
                //贸易伙伴为空的话，就哪个有值取哪个
                invoiceId = getInvoiceId(documentDTO.getReference());
                if (CharSequenceUtil.isBlank(invoiceId)) {
                    invoiceId = getInvoiceId(documentDTO.getItemText());
                }
            }

            //税额
            String taxAmount = documentDTO.getDebitAmountInLocalCurrency().add(
                    documentDTO.getCreditAmountInLocalCurrency().multiply(new BigDecimal(-1))).toString();

            //数据湖提取的字段做一下汇总，相同的发票号，其对应的金额累加
            if (invoiceIdList.contains(invoiceId) && CharSequenceUtil.isNotBlank(invoiceId)) {
                DataLakeExportDTO mergedDto = resultList.get(invoiceIdList.indexOf(invoiceId));
                mergedDto.setTaxAmount(new BigDecimal(mergedDto.getTaxAmount()).add(new BigDecimal(taxAmount)).toString());
                //剩下的两个字段，如果有重复的就不用新增
                if (!mergedDto.getAccountingDocumentNumber().contains(documentDTO.getAccountingDocumentNumber())) {
                    mergedDto.setAccountingDocumentNumber(mergedDto.getAccountingDocumentNumber() + "," + documentDTO.getAccountingDocumentNumber());
                }
                if (!mergedDto.getFiscalYearPeriod().contains(documentDTO.getFiscalYearPeriod())) {
                    mergedDto.setFiscalYearPeriod(mergedDto.getFiscalYearPeriod() + "," + documentDTO.getFiscalYearPeriod());
                }
            } else {
                invoiceIdList.add(invoiceId);
                resultList.add(this.toCompareRunDTO(documentDTO, invoiceId, taxAmount));
            }
        });

        return resultList;
    }

    public abstract DataLakeExportDTO toCompareRunDTO(AccountingDocumentDTO accountingDocumentDTO, String invoiceId, String taxAmount);

    private String getInvoiceId(String text) {
        //匹配形如数字*数字的模式
        Pattern pattern = Pattern.compile("(\\d+)\\*(\\d+)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String part1 = matcher.group(1);
            String part2 = matcher.group(2);
            // 计算需要填充的0的数量
            int totalLength = part1.length() + part2.length();
            if (totalLength < 20) {
                int zerosNeeded = 20 - totalLength;
                // 填充0并重新拼接
                text = part1 + "0".repeat(zerosNeeded) + part2;
            }
        }

        pattern = Pattern.compile("(?<!\\d)(\\d{20}|\\d{16}|\\d{8})(?!\\d)");
        matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return "";
    }

}
