package com.envision.bunny.module.extract.application;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.envision.extract.facade.azure.BlobStorageRemote;
import com.envision.extract.infrastructure.mybatis.BasicPagination;
import com.envision.extract.infrastructure.response.BizException;
import com.envision.extract.infrastructure.response.ErrorCode;
import com.envision.extract.module.extract.application.dtos.CompareRunDTO;
import com.envision.extract.module.extract.application.query.CompareRunQuery;
import com.envision.extract.module.extract.application.validations.CompareValidation;
import com.envision.extract.module.extract.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author gangxiang.guan
 * @date 2025/9/26 11:18
 */
@Slf4j
@Service
@EnableConfigurationProperties(CompareValidation.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InvoiceQueryService {
    private final InvoiceAssembler assembler;
    private final CompareRunRepository compareRunRepository;
    private final CompareRunDetailRepository compareRunDetailRepository;
    private final BlobStorageRemote blobStorageRemote;

    public BasicPagination<CompareRunDTO> queryList(CompareRunQuery query) {
        IPage<CompareRun> page = compareRunRepository.getByPage(query.getProjectId(), new Page<>(query.getPageNum(), query.getPageSize()));
        
        // 如果没有数据，直接返回空分页
        if (CollUtil.isEmpty(page.getRecords())) {
            return BasicPagination.of(page, assembler::toCompareRunDTO);
        }
        
        // 批量查询所有相关的 CompareRunDetail
        List<CompareRunDetail> allDetails = compareRunDetailRepository.lambdaQuery()
                .in(CompareRunDetail::getCompareRunId, page.getRecords().stream().map(CompareRun::getId).toList()).list();
        
        // 按 compareRunId 分组
        Map<Long, List<CompareRunDetail>> detailsMap = allDetails.stream()
                .collect(Collectors.groupingBy(CompareRunDetail::getCompareRunId));
        
        return BasicPagination.of(page, compareRun -> assembler.toCompareRunDTO(compareRun,
                detailsMap.getOrDefault(compareRun.getId(), Collections.emptyList())));
    }

    public void resultDownload(Long compareRunId, Long projectId, String companyCode, HttpServletResponse response) throws IOException {
        CompareRun compareRun = Optional.ofNullable(compareRunRepository.lambdaQuery().eq(CompareRun::getId, compareRunId)
                .eq(CompareRun::getProjectId, projectId).one()).orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "未找到对比任务"));
        if (compareRun.getStatus() != CompareRunStatusEnum.COMPARE_SUCCESS) {
            throw new BizException(ErrorCode.BAD_REQUEST, "对比任务执行异常:" + compareRun.getError());
        }

        CompareRunDetail compareRunDetail = Optional.ofNullable(compareRunDetailRepository.lambdaQuery().eq(CompareRunDetail::getCompareRunId, compareRunId)
                .eq(CompareRunDetail::getCompanyCode, companyCode).one()).orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "对比任务中无该公司代码"));

        if (CharSequenceUtil.isBlank(compareRunDetail.getCompareResult()) || !blobStorageRemote.exists(compareRunDetail.getCompareResult())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "未找到对比结果文件路径");
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("[" + companyCode + "]" + (compareRun.getType() == CompareTypeEnum.DATALAKECOMPARE ? "数据湖对比结果" : "税务局对比结果"),
                StandardCharsets.UTF_8).replace("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + "_" + System.currentTimeMillis() + ".xlsx");
        blobStorageRemote.loadStream(compareRunDetail.getCompareResult(), response.getOutputStream());
    }
}
