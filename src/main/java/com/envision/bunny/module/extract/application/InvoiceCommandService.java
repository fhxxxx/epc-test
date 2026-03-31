package com.envision.bunny.module.extract.application;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.envision.extract.facade.azure.BlobStorageRemote;
import com.envision.extract.facade.platform.PlatformRemote;
import com.envision.extract.infrastructure.easyExcel.AutoColumnWidthStrategy;
import com.envision.extract.infrastructure.easyExcel.CustomHeaderColorHandler;
import com.envision.extract.infrastructure.response.BizException;
import com.envision.extract.infrastructure.response.ErrorCode;
import com.envision.extract.infrastructure.util.ApplicationContextUtils;
import com.envision.extract.module.extract.application.command.DataLakeCompareCommand;
import com.envision.extract.module.extract.application.command.TaxBureauCompareCommand;
import com.envision.extract.module.extract.application.dtos.*;
import com.envision.extract.module.extract.application.query.InvoiceQuery;
import com.envision.extract.module.extract.application.tasks.Constants;
import com.envision.extract.module.extract.application.tasks.MessageProducer;
import com.envision.extract.module.extract.application.validations.CompareValidation;
import com.envision.extract.module.extract.domain.*;
import com.envision.extract.module.extract.infrastructure.Constant;
import com.envision.extract.module.uploadfile.application.UploadFileCommandService;
import com.envision.extract.module.uploadfile.application.UploadFileQueryService;
import com.envision.extract.module.uploadfile.application.dtos.UploadFileDTO;
import com.envision.extract.module.uploadfile.domain.UploadFile;
import com.envision.extract.module.uploadfile.domain.UploadFileRepository;
import com.envision.extract.module.uploadfile.domain.UploadTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author gangxiang.guan
 * @date 2025/9/26 11:18
 */
@Slf4j
@Service
@EnableConfigurationProperties(CompareValidation.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InvoiceCommandService {
    private final PlatformRemote platformRemote;
    private final BlobStorageRemote blobStorageRemote;
    private final InvoiceAssembler assembler;
    private final UploadFileCommandService uploadFileCommandService;
    private final UploadFileQueryService uploadFileQueryService;
    private final UploadFileRepository uploadFileRepository;
    private final CompareRunRepository compareRunRepository;
    private final CompareRunDetailRepository compareRunDetailRepository;
    private final TaskExecutor taskExecutor;
    private final MessageProducer messageProducer;
    private final CompareValidation compareValidation;

    @Value("${custom.platform.token.domain}")
    private String platformDomain;

    public List<String> queryFromDataLakeBatch(InvoiceQuery query) {
        List<CompletableFuture<String>> futures = new ArrayList<>();

        // 为每个公司代码创建异步任务
        for (String companyCode : query.getCompanyCodeList()) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    List<AccountingDocumentDTO> accountingDocumentDTOS = queryFromDataLake(
                            companyCode,
                            query.getFiscalYearPeriodStart(),
                            query.getFiscalYearPeriodEnd(),
                            query.getPostingDateInTheDocumentStart(),
                            query.getPostingDateInTheDocumentEnd()
                    );

                    if (CollectionUtils.isEmpty(accountingDocumentDTOS)) {
                        return "公司代码：" + companyCode + " 查无数据";
                    }

                    // 直接保存结果到Excel
                    saveDataToExcel(companyCode, query.getProjectId(), accountingDocumentDTOS);
                    return "";

                } catch (Exception e) {
                    log.error("查询数据湖数据异常，公司代码: {}", companyCode, e);
                    return "公司代码 " + companyCode + " 查询异常: " + e.getMessage();
                }
            }, taskExecutor);

            futures.add(future);
        }

        // 等待所有任务完成
        return futures.stream().map(CompletableFuture::join).filter(CharSequenceUtil::isNotBlank).toList();
    }

    public List<AccountingDocumentDTO> queryFromDataLake(String companyCode, String fiscalYearPeriodStart, String fiscalYearPeriodEnd,
                                                         String postingDateInTheDocumentStart, String postingDateInTheDocumentEnd) {
        int offset = 0;
        int limit = 5000;
        String reqUrl = platformDomain + CharSequenceUtil.format(Constant.FINANCE_ELECTRONICARCHIVES_REQ_PATH_PATTERN,
                companyCode, fiscalYearPeriodStart, fiscalYearPeriodEnd, offset, limit);
        if (CharSequenceUtil.isNotBlank(postingDateInTheDocumentStart) && CharSequenceUtil.isNotBlank(postingDateInTheDocumentEnd)) {
            reqUrl += CharSequenceUtil.format(Constant.DATE_PARAMETER, postingDateInTheDocumentStart, postingDateInTheDocumentEnd);
        }
        return platformRemote.fetchFromDataLake(Constant.FINANCE_ELECTRONICARCHIVES_SVC, reqUrl, AccountingDocumentDTO::fromPltData);
    }

    /**
     * 将数据湖的结果导出成excel上传到文件管理中
     *
     * @return
     */
    public UploadFileDTO saveDataToExcel(String companyCode, Long projectId, List<AccountingDocumentDTO> documentDTOList) throws IOException {
        String fileName = "数据湖捞取结果-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        String path = "dataLake/" + UUID.randomUUID().toString().replace("-", "") + ".xlsx";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ExcelWriter excelWriter = EasyExcelFactory.write(byteArrayOutputStream)
                .registerWriteHandler(new AutoColumnWidthStrategy()).build()) {
            WriteSheet dataLakeSheet = EasyExcelFactory.writerSheet(0, "数据湖捞取结果")
                    .head(DataLakeExportDTO.class)
                    .registerWriteHandler(new CustomHeaderColorHandler(List.of(5, 6), 0))
                    .build();
            excelWriter.write(assembler.toDataLakeExportDTOS(documentDTOList), dataLakeSheet);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        return uploadFileCommandService.uploadExcel(inputStream, projectId, companyCode, fileName, path, UploadTypeEnum.DATALAKE);
    }

    @Transactional(rollbackFor = Exception.class)
    public CompareRunDTO createDataLakeCompare(DataLakeCompareCommand dataLakeCompareCommand) {
        List<UploadFile> resultUploadFiles = uploadFileQueryService.validAndGetFiles(
                dataLakeCompareCommand.getResultFileIds(), UploadTypeEnum.RESULT, dataLakeCompareCommand.getProjectId());
        List<UploadFile> datalakeUploadFiles = uploadFileQueryService.validAndGetFiles(
                dataLakeCompareCommand.getDataLakeFileIds(), UploadTypeEnum.DATALAKE, dataLakeCompareCommand.getProjectId());
        List<String> resultUploadFileNames = resultUploadFiles.stream().map(UploadFile::getName).toList();
        List<String> datalakeUploadFileNames = datalakeUploadFiles.stream().map(UploadFile::getName).toList();

        Map<String, List<UploadFile>> resultFileMap = resultUploadFiles.stream()
                .collect(Collectors.groupingBy(UploadFile::getCompanyCode));
        Map<String, List<UploadFile>> datalakeFileMap = datalakeUploadFiles.stream()
                .collect(Collectors.groupingBy(UploadFile::getCompanyCode));
        Set<String> companyCodeSet = Stream.concat(resultFileMap.keySet().stream(), datalakeFileMap.keySet().stream())
                .collect(Collectors.toSet());

        validateCompanyFiles(companyCodeSet, resultFileMap, datalakeFileMap);

        try {
            CompareRun compareRun = assembler.toDataLakeCompareRun(dataLakeCompareCommand, CompareRunStatusEnum.COMPARE_RUNNING,
                    Collections.emptyMap(), "", resultUploadFileNames, datalakeUploadFileNames);
            compareRunRepository.save(compareRun);

            List<CompareRunDetail> compareRunDetails = companyCodeSet.stream()
                    .map(companyCode -> assembler.toDatalakeCompareRunDetail(
                            compareRun.getId(), companyCode,
                            resultFileMap.getOrDefault(companyCode, Collections.emptyList()),
                            datalakeFileMap.getOrDefault(companyCode, Collections.emptyList()))).toList();

            if (CollUtil.isNotEmpty(compareRunDetails)) {
                compareRunDetailRepository.saveBatch(compareRunDetails);
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messageProducer.sendMessage(Constants.COMPARE_TASK,
                            Map.of(Constants.COMPARE_RUN_KEY, String.valueOf(compareRun.getId())));
                }
            });
            return assembler.toCompareRunDTO(compareRun);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建发票和数据湖对比任务异常{}", dataLakeCompareCommand, e);
            throw new BizException(ErrorCode.SYS_ERROR, e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public CompareRunDTO createTaxBureauCompare(TaxBureauCompareCommand compareCommand) {
        List<UploadFile> datalakeCompareFiles = uploadFileQueryService.validAndGetFiles(
                compareCommand.getDataLakeCompareFileIds(), UploadTypeEnum.DATALAKECOMPARE, compareCommand.getProjectId());
        List<UploadFile> taxBureauFiles = uploadFileQueryService.validAndGetFiles(
                compareCommand.getTaxBureauFileIds(), UploadTypeEnum.TAXBUREAU, compareCommand.getProjectId());
        List<String> datalakeCompareFileIds = datalakeCompareFiles.stream().map(UploadFile::getName).toList();
        List<String> taxBureauFileIds = taxBureauFiles.stream().map(UploadFile::getName).toList();

        Map<String, List<UploadFile>> datalakeCompareFileMap = datalakeCompareFiles.stream()
                .collect(Collectors.groupingBy(UploadFile::getCompanyCode));
        Map<String, List<UploadFile>> taxBureauFileMap = taxBureauFiles.stream()
                .collect(Collectors.groupingBy(UploadFile::getCompanyCode));
        Set<String> companyCodeSet = Stream.concat(datalakeCompareFileMap.keySet().stream(), taxBureauFileMap.keySet().stream())
                .collect(Collectors.toSet());

        validateCompanyFiles(companyCodeSet, datalakeCompareFileMap, taxBureauFileMap);

        try {
            CompareRun compareRun = assembler.toTaxBureauCompareRun(compareCommand, CompareRunStatusEnum.COMPARE_RUNNING,
                    Collections.emptyMap(), "", datalakeCompareFileIds, taxBureauFileIds);
            compareRunRepository.save(compareRun);

            List<CompareRunDetail> compareRunDetails = companyCodeSet.stream()
                    .map(companyCode -> assembler.toTaxBureauCompareRunDetail(
                            compareRun.getId(), companyCode,
                            datalakeCompareFileMap.getOrDefault(companyCode, Collections.emptyList()),
                            taxBureauFileMap.getOrDefault(companyCode, Collections.emptyList()))).toList();

            if (CollUtil.isNotEmpty(compareRunDetails)) {
                compareRunDetailRepository.saveBatch(compareRunDetails);
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messageProducer.sendMessage(Constants.COMPARE_TASK,
                            Map.of(Constants.COMPARE_RUN_KEY, String.valueOf(compareRun.getId())));
                }
            });
            return assembler.toCompareRunDTO(compareRun);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建税务局对比任务异常{}", compareCommand, e);
            throw new BizException(ErrorCode.SYS_ERROR, e.getMessage());
        }
    }

    private void validateCompanyFiles(Set<String> companyCodeSet, Map<String, List<UploadFile>> resultFileMap,
                                      Map<String, List<UploadFile>> datalakeFileMap) {
        List<String> missingResult = companyCodeSet.stream()
                .filter(companyCode -> CollectionUtils.isEmpty(resultFileMap.get(companyCode)))
                .toList();
        List<String> missingDataLake = companyCodeSet.stream()
                .filter(companyCode -> CollectionUtils.isEmpty(datalakeFileMap.get(companyCode)))
                .toList();

        if (!missingResult.isEmpty() || !missingDataLake.isEmpty()) {
            StringBuilder message = new StringBuilder();
            if (!missingResult.isEmpty()) {
                message.append("公司代码[").append(String.join(",", missingResult)).append("]缺少提取结果文件;");
            }
            if (!missingDataLake.isEmpty()) {
                message.append("公司代码[").append(String.join(",", missingDataLake)).append("]缺少数据湖文件;");
            }
            throw new BizException(ErrorCode.BAD_REQUEST, message.toString());
        }
    }

    /**
     * 开始对比
     * @param runId
     */
    public void startCompare(Long runId) {
        log.info("Compare started: [{}]", runId);
        CompareRun compareRun = compareRunRepository.getById(runId);
        if (compareRun == null || compareRun.getStatus() != CompareRunStatusEnum.COMPARE_RUNNING) {
            log.info("CompareRun not found or not running: [{}] [{}]", runId, compareRun != null ? compareRun.getStatus() : "");
            return;
        }
        List<CompareRunDetail> compareRunDetails = compareRunDetailRepository.lambdaQuery().eq(CompareRunDetail::getCompareRunId, compareRun.getId()).list();

        try {
            if (compareRun.getType() == CompareTypeEnum.DATALAKECOMPARE) {
                compareRunDetails.parallelStream().forEach(this::dataLakeCompare);
            } else if (compareRun.getType() == CompareTypeEnum.TAXBUREAUCOMPARE) {
                compareRunDetails.parallelStream().forEach(this::taxBureauCompare);
            }

            //更新详情表
            compareRunDetailRepository.updateBatchById(compareRunDetails);
            compareRun.success();
            compareRunRepository.updateById(compareRun, CompareRun::getStatus);

            if (compareRun.getType() == CompareTypeEnum.DATALAKECOMPARE) {
                try {
                    ApplicationContextUtils.getBean(InvoiceCommandService.class).resultUpload(compareRun.getProjectId(), runId);
                } catch (Exception e) {
                    log.error("第一次对比成功，但对比结果上传失败，projectId:[{}], compareRunId:[{}]", compareRun.getProjectId(), compareRun.getId(), e);
                }
            }
        } catch (Exception e) {
            compareRun.failed(e.getMessage());
            compareRunRepository.updateById(compareRun, CompareRun::getStatus, CompareRun::getError);
        }
    }

    public void dataLakeCompare(CompareRunDetail detail) {
        if (detail == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "对比详情不能为空");
        }
        List<UploadFile> resultFiles = uploadFileRepository.lambdaQuery().in(UploadFile::getId, detail.getResultFileIds()).list();
        List<UploadFile> dataLakeFiles = uploadFileRepository.lambdaQuery().in(UploadFile::getId, detail.getDataLakeFileIds()).list();
        if (CollUtil.isEmpty(resultFiles) || CollUtil.isEmpty(dataLakeFiles)) {
            throw new BizException(ErrorCode.BAD_REQUEST, detail.getCompanyCode() + "提取结果或数据湖文件缺失");
        }
        List<ResultExportDTO> resultExportDTOS = resultFiles.stream().map(uploadFile -> this.getList(
                ResultExportDTO.class, uploadFile.getPath())).flatMap(Collection::stream).toList();
        List<DataLakeExportDTO> dataLakeExportDTOS = dataLakeFiles.stream().map(uploadFile -> this.getList(
                DataLakeExportDTO.class, uploadFile.getPath())).flatMap(Collection::stream).toList();
        List<ResultDataLakeCompareDTO> resultDataLakeCompareDTOS = compareResultAndDataLake(resultExportDTOS, dataLakeExportDTOS);

        String path = "dataLakeCompare/" + UUID.randomUUID().toString().replace("-", "") + ".xlsx";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // 创建并注册处理器，传入数据行数
        try (ExcelWriter excelWriter = EasyExcelFactory.write(byteArrayOutputStream)
                .registerWriteHandler(new AutoColumnWidthStrategy()).build()) {
            WriteSheet matchedSheet = EasyExcelFactory.writerSheet(0, "会计凭证发票信息与发票PDF对比输出文件")
                    .head(ResultDataLakeCompareDTO.class)
                    .build();
            excelWriter.write(resultDataLakeCompareDTOS, matchedSheet);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        blobStorageRemote.upload(path, inputStream);
        detail.setCompareResult(path);
    }

    public void taxBureauCompare(CompareRunDetail detail) {
        if (detail == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "对比详情不能为空");
        }
        List<UploadFile> datalakeCompareFileIds = uploadFileRepository.lambdaQuery().in(UploadFile::getId, detail.getDataLakeCompareFileIds()).list();
        List<UploadFile> taxBureauFileIds = uploadFileRepository.lambdaQuery().in(UploadFile::getId, detail.getTaxBureauFileIds()).list();
        if (CollUtil.isEmpty(datalakeCompareFileIds) || CollUtil.isEmpty(taxBureauFileIds)) {
            throw new BizException(ErrorCode.BAD_REQUEST, detail.getCompanyCode() + "第一次对比文件或税务局文件缺失");
        }
        List<ResultDataLakeCompareDTO> resultDataLakeCompareDTOS = datalakeCompareFileIds.stream().map(uploadFile -> this.getList(
                ResultDataLakeCompareDTO.class, uploadFile.getPath())).flatMap(Collection::stream).toList();
        List<TaxBureauDTO> taxBureauDTOS = taxBureauFileIds.stream().map(uploadFile -> this.getList(
                TaxBureauDTO.class, uploadFile.getPath())).flatMap(Collection::stream).toList();
        List<TaxBureauDTO> matchedTaxBureaus = this.matchTaxBureau(taxBureauDTOS, resultDataLakeCompareDTOS);

        String path = "taxBureauCompare/" + UUID.randomUUID().toString().replace("-", "") + ".xlsx";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // 创建并注册处理器，传入数据行数
        try (ExcelWriter excelWriter = EasyExcelFactory.write(byteArrayOutputStream)
                .registerWriteHandler(new AutoColumnWidthStrategy()).build()) {
            WriteSheet matchedSheet = EasyExcelFactory.writerSheet(0, "发票对比输出文件与电子税务局导出模板")
                    .head(TaxBureauDTO.class)
                    .build();
            excelWriter.write(matchedTaxBureaus, matchedSheet);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        blobStorageRemote.upload(path, inputStream);
        detail.setCompareResult(path);
    }


    /**
     * 从excel-path中读取数据至dtos
     */
    private <T> List<T> getList(Class<T> clazz, String path) {
        if (CharSequenceUtil.isBlank(path) || !blobStorageRemote.exists(path)) {
            log.error("[{}]文件路径获取不到数据", path);
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "获取文件异常");
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        blobStorageRemote.loadStream(path, byteArrayOutputStream);

        return EasyExcelFactory.read(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))
                .head(clazz).sheet().doReadSync();
    }

    /**
     * 会计凭证发票信息与发票PDF对比输出文件(用发票号+税额匹配)
     *
     * @return
     */
    private List<ResultDataLakeCompareDTO> compareResultAndDataLake(List<ResultExportDTO> resultExportDTOS, List<DataLakeExportDTO> dataLakeExportDTOS) {
        List<ResultDataLakeCompareDTO> resultList = new ArrayList<>();
        //匹配上的数据（发票号+税额)
        Set<String> matchData = new HashSet<>();

        Map<String, ResultExportDTO> resultMap = resultExportDTOS.stream().collect(Collectors.toMap(key ->
                key.getInvoiceId() + "-" + this.processAmount(key.getTaxAmount()), Function.identity(), (x1, x2) -> x1));

        //先将能匹配上的数据结果组装
        dataLakeExportDTOS.stream().filter(dto -> CharSequenceUtil.isNotBlank(dto.getInvoiceId()) &&
                resultMap.containsKey(dto.getInvoiceId() + "-" + this.processAmount(dto.getTaxAmount()))).forEach(dto -> {
            matchData.add(dto.getInvoiceId() + "-" + this.processAmount(dto.getTaxAmount()));
            ResultExportDTO resultExportDTO = resultMap.get(dto.getInvoiceId() + "-" + this.processAmount(dto.getTaxAmount()));
            resultList.add(assembler.toResultDataLakeCompareDTO(dto, resultExportDTO, ResultDataLakeCompareDTO.MATCHED));
        });

        //按照顺序处理数据湖这未能匹配上结果中的数据
        dataLakeExportDTOS.stream().filter(dto -> CharSequenceUtil.isBlank(dto.getInvoiceId()) ||
                !resultMap.containsKey(dto.getInvoiceId() + "-" + this.processAmount(dto.getTaxAmount()))).forEach(dto -> {
            resultList.add(assembler.toResultDataLakeCompareDTO(dto, new ResultExportDTO(), ResultDataLakeCompareDTO.UNMATCHED));
        });

        //按照顺序处理提取结果没有匹配上的数据
        resultExportDTOS.stream().filter(dto -> !matchData.contains(dto.getInvoiceId() + "-" + this.processAmount(dto.getTaxAmount())))
                .forEach(dto -> resultList.add(assembler.toResultDataLakeCompareDTO(new DataLakeExportDTO(), dto, ResultDataLakeCompareDTO.UNMATCHED)));

        //最后组装合计行
        BigDecimal sumTaxAmount = resultList.stream().map(dto -> this.processAmount(dto.getTaxAmount()))
                .filter(CharSequenceUtil::isNotBlank).map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumRTaxAmount = resultList.stream().map(dto -> this.processAmount(dto.getRTaxAmount()))
                .filter(CharSequenceUtil::isNotBlank).map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumSubTotal = resultList.stream().map(dto -> this.processAmount(dto.getRSubTotal()))
                .filter(CharSequenceUtil::isNotBlank).map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);

        resultList.add(ResultDataLakeCompareDTO.getSumRow(sumTaxAmount.toString(), sumRTaxAmount.toString(), sumSubTotal.toString()));
        return resultList;
    }

    private String processAmount(String amount) {
        if (CharSequenceUtil.isNotBlank(amount) && amount.matches("^-?\\d+(\\.\\d+)?$")) {
            return String.valueOf(Double.valueOf(amount));
        }
        return "0";
    }

    /**
     * 发票对比输出文件与电子税务局导出模板：
     */
    private List<TaxBureauDTO> matchTaxBureau(List<TaxBureauDTO> taxBureauDTOS, List<ResultDataLakeCompareDTO> compareDTOS) {
        //获取发票提取结果与税务局数据匹配项的 发票号+金额
        List<String> matchedList = compareDTOS.stream().filter(dto -> ResultDataLakeCompareDTO.MATCHED
                .equals(dto.getMatchSuccess())).map(dto -> (CharSequenceUtil.isNotBlank(dto.getRInvoiceCode()) ?
                dto.getRInvoiceCode() + "-" : "") + dto.getInvoiceId() + "-" + this.processAmount(dto.getTaxAmount())).toList();
        List<TaxBureauDTO> resultList = taxBureauDTOS.stream().filter(dto -> matchedList.contains(
                (CharSequenceUtil.isEmpty(dto.getDigitalInvoiceNumber()) ? dto.getInvoiceCode() + "-" + dto.getInvoiceNumber() : dto.getDigitalInvoiceNumber()) + "-"
                        + this.processAmount(dto.getFaceTaxAmount()))).toList();
        resultList.forEach(dto -> dto.setIsSelected("是"));
        return resultList;
    }

    public void compareDelete(Long compareRunId, Long projectId) {
        compareRunRepository.lambdaUpdate()
                .eq(CompareRun::getId, compareRunId)
                .eq(CompareRun::getProjectId, projectId)
                .remove();

        List<CompareRunDetail> compareRunDetails = compareRunDetailRepository.lambdaQuery()
                .eq(CompareRunDetail::getCompareRunId, compareRunId)
                .list();

        compareRunDetails.forEach(detail -> blobStorageRemote.delete(detail.getCompareResult()));

        compareRunDetailRepository.lambdaUpdate()
                .eq(CompareRunDetail::getCompareRunId, compareRunId)
                .remove();
    }

    public void resultUpload(Long projectId, Long compareRunId) {
        CompareRun compareRun = Optional.ofNullable(compareRunRepository.lambdaQuery().eq(CompareRun::getId, compareRunId)
                .eq(CompareRun::getProjectId, projectId).one()).orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "未找到对比任务"));
        if (compareRun.getStatus() != CompareRunStatusEnum.COMPARE_SUCCESS) {
            throw new BizException(ErrorCode.BAD_REQUEST, "对比任务执行异常:" + compareRun.getError());
        }

        List<CompareRunDetail> compareRunDetails = compareRunDetailRepository.lambdaQuery().eq(CompareRunDetail::getCompareRunId, compareRunId).list();
        if (CollUtil.isEmpty(compareRunDetails)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "对比任务详情为空");
        }

        for (CompareRunDetail detail : compareRunDetails) {
            String fileName = "[" + detail.getCompanyCode() + "]数据湖对比结果-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobStorageRemote.loadStream(detail.getCompareResult(), outputStream);
            uploadFileCommandService.uploadExcel(projectId, detail.getCompanyCode(), fileName, detail.getCompareResult(),
                    UploadTypeEnum.DATALAKECOMPARE, (long) outputStream.size(), compareRun.getCreateBy(), compareRun.getCreateByName()
                    , LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        }
    }

    /**
     * 处理超时任务
     * @param compareRunId
     */
    @Transactional(rollbackFor = Exception.class)
    public void overTimeCompareTask(Long compareRunId) {
        log.warn("Compare Task overTime: [{}]", compareRunId);
        CompareRun compareRun = compareRunRepository.getById(compareRunId);
        if (compareRun == null || compareRun.getStatus() != CompareRunStatusEnum.COMPARE_RUNNING) {
            log.info("Compare Task overTime break ,CompareRun not found or status not running: [{}] [{}]",
                    compareRunId, compareRun != null ? compareRun.getStatus() : "");
            return;
        }
        compareRun.failed("compare task overTime");
        compareRunRepository.updateById(compareRun, CompareRun::getStatus, CompareRun::getError);
    }
}
