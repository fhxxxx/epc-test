package com.envision.epc.module.extract.web;

import cn.hutool.core.collection.CollUtil;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.idempotent.Idempotent;
import com.envision.epc.infrastructure.log.AvoidLog;
import com.envision.epc.infrastructure.mybatis.BasicPagination;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.extract.application.InvoiceCommandService;
import com.envision.epc.module.extract.application.InvoiceQueryService;
import com.envision.epc.module.extract.application.command.DataLakeCompareCommand;
import com.envision.epc.module.extract.application.command.TaxBureauCompareCommand;
import com.envision.epc.module.extract.application.dtos.CompareRunDTO;
import com.envision.epc.module.extract.application.query.CompareRunQuery;
import com.envision.epc.module.extract.application.query.InvoiceQuery;
import com.envision.epc.module.extract.application.validations.ValidInvoice;
import com.envision.epc.module.validation.ValidProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 会计凭证发票
 *
 * @author gangxiang.guan
 * @date 2025/9/26 11:13
 */
@Validated
@RestController
@RequestMapping("/invoice")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InvoiceController {
    private final InvoiceCommandService invoiceCommandService;
    private final InvoiceQueryService invoiceQueryService;
    private final BlobStorageRemote blobStorageRemote;

    /**
     * 根据入参查询数据湖接口，并将返回数据导出为excel
     */
    @PostMapping("/datalake-export")
    public void dataLakeExport(@RequestBody @ValidInvoice InvoiceQuery query) {
        List<String> errors = invoiceCommandService.queryFromDataLakeBatch(query);
        if (CollUtil.isNotEmpty(errors)) {
            if (errors.size() == query.getCompanyCodeList().size()) {
                throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, String.join(";", errors));
            } else {
                throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "捞取成功，部分公司代码捞取异常[" + String.join(";", errors) + "]");
            }
        }
    }

    /**
     * 创建发票提取和数据湖数据对比任务
     */
    @Idempotent(prefix = "taxBureauCompare", spelKey = "#{#dataLakeCompareCommand.assembleRedisKey() + '-' + T(com.envision.epc.infrastructure.security.SecurityUtils).getCurrentUserCode()}", expireSeconds = 5)
    @PostMapping("/datalake-compare")
    public CompareRunDTO dataLakeCompare(@RequestBody @Valid DataLakeCompareCommand dataLakeCompareCommand) {
        return invoiceCommandService.createDataLakeCompare(dataLakeCompareCommand);
    }

    /**
     * 将数据湖对比结果生成excel并上传至文件管理中去
     *
     */
    @AvoidLog
    @GetMapping("/compare/{compareRunId}/result-upload")
    public void resultUpload(@ValidProject Long projectId, @PathVariable(value = "compareRunId") Long compareRunId) {
        invoiceCommandService.resultUpload(projectId, compareRunId);
    }

    /**
     * 创建第一步对比结论与税务局文件对比任务
     */
    @Idempotent(prefix = "taxBureauCompare", spelKey = "#{#taxBureauCompareCommand.assembleRedisKey() + '-' + T(com.envision.epc.infrastructure.security.SecurityUtils).getCurrentUserCode()}", expireSeconds = 5)
    @PostMapping("/tax-bureau-compare")
    public CompareRunDTO taxBureauCompare(@RequestBody @Valid TaxBureauCompareCommand taxBureauCompareCommand) {
        return invoiceCommandService.createTaxBureauCompare(taxBureauCompareCommand);
    }

    /**
     * 分页查询任务
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @AvoidLog
    @GetMapping("/compare")
    public BasicPagination<CompareRunDTO> queryByKeyword(@Valid CompareRunQuery query) {
        return invoiceQueryService.queryList(query);
    }

    /**
     * 删除对比任务
     */
    @DeleteMapping("/compare/{compareRunId}")
    public void compareDelete(@PathVariable(value = "compareRunId") Long compareRunId, @ValidProject Long projectId) {
        invoiceCommandService.compareDelete(compareRunId, projectId);
    }

    /**
     * 对比任务结果导出
     */
    @GetMapping("/compare/{compareRunId}/download")
    public void compareResultDownload(@PathVariable(value = "compareRunId") Long compareRunId, @ValidProject Long projectId,
                                      @RequestParam String companyCode, HttpServletResponse response) throws IOException {
        invoiceQueryService.resultDownload(compareRunId, projectId, companyCode, response);
    }


    /**
     * 测试文件接口
     *
     */
    @GetMapping("get-excel")
    public void getresultJsonDetail(String fileName, HttpServletResponse response) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Expose-Headers", "Accept-Ranges, Content-Range, Content-Length");
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Keep-Alive", "timeout=5");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        blobStorageRemote.loadStream(fileName, response.getOutputStream());
    }

    @GetMapping("get-json")
    public JsonNode getresultJsonDetail(String fileName) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobStorageRemote.loadStream(fileName, outputStream);
        String s = StreamUtils.copyToString(outputStream, StandardCharsets.UTF_8);
        outputStream.close();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(s);
    }
}
