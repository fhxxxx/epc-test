package com.envision.epc.module.extract.application;

import com.envision.epc.module.extract.application.command.ExtractCommand;
import com.envision.epc.module.extract.application.dtos.*;
import com.envision.epc.module.extract.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author wenjun.gu
 * @since 2025/8/14-15:14
 */
@Mapper(componentModel = "spring")
public abstract class ExtractRunAssembler {

    public abstract ExtractRun fromExtractCommand(ExtractCommand command, Integer currentVersion);

    @Mappings({
            @Mapping(target = "extractConfig", source = "command.extractConfig")
    })
    public abstract ExtractRunVersion fromExtractCommand(ExtractCommand command, Integer version, ExtractRunStatusEnum status);

    @Mappings({
            @Mapping(target = "extractConfig", source = "command.extractConfig")
    })
    public abstract ExtractRunVersion fromExtractCommand(ExtractCommand command, Long extractRunId, Integer version, ExtractRunStatusEnum status);

    public abstract ExtractRunVersion buildExtractRunVersion(ExtractCommand command, Integer version, ExtractRunStatusEnum status);

//    public abstract ExtractResult fromExtractCommand(ExtractCommand command, Integer i);

    public abstract OcrTask buildOcrTask(Long projectId, Integer version, Long fileId, String hash, int position, Integer startPage, Integer endPage, OcrTaskStatusEnum status);

    @Mappings({
            @Mapping(target = "createBy", source = "createBy"),
            @Mapping(target = "updateBy", source = "createBy"),
            @Mapping(target = "createTime", source = "createTime"),
            @Mapping(target = "updateTime", source = "createTime"),
            @Mapping(target = "createByName", source = "createByName"),
            @Mapping(target = "updateByName", source = "createByName")
    })
    public abstract OcrTask buildOcrTask(Long projectId, Long extractRunId, Integer version, Long fileId, String hash,
                                         int position, Integer startPage, Integer endPage, OcrTaskStatusEnum status,
                                         String createBy, String createByName, LocalDateTime createTime);

    public abstract ExtractRunVersionDTO toExtractRunDTO(ExtractRun extractRun);

    @Mappings({
            @Mapping(target = "createBy", source = "createBy"),
            @Mapping(target = "updateBy", source = "createBy"),
            @Mapping(target = "createTime", source = "createTime"),
            @Mapping(target = "updateTime", source = "createTime"),
            @Mapping(target = "createByName", source = "createByName"),
            @Mapping(target = "updateByName", source = "createByName")
    })
    public abstract ExtractTask buildExtractResult(Long projectId, Long extractRunId, Integer version, Long fileId, int position
            , Integer startPage, Integer endPage, String createBy, String createByName, LocalDateTime createTime);

    public abstract ExtractRunPageDTO toExtractRunPageDTO(ExtractRun extractRun);

    @Mappings({
            @Mapping(target = "id", source = "extractRunVersion.id"),
            @Mapping(target = "extractRunVersionId", source = "extractRunVersion.id")
    })
    public abstract ExtractRunPageDTO toExtractRunPageDTO(ExtractRunVersion extractRunVersion);

    @Mappings({
            @Mapping(target = "id", source = "extractRunVersion.id"),
            @Mapping(target = "extractRunVersionId", source = "extractRunVersion.id"),
            @Mapping(target = "fileList", expression = "java(this.toFiles(extractRunVersion, fileMap))"),
            @Mapping(target = "createByName", source = "extractRunVersion.createByName"),
            @Mapping(target = "updateByName", source = "extractRunVersion.updateByName"),
            @Mapping(target = "companyCodeList", expression = "java(this.getCompanyCodeList(extractRunVersion, fileMap))")
    })
    public abstract ExtractRunPageDTO toExtractRunPageDTO(ExtractRunVersion extractRunVersion, Map<Long, List<ExtractRunFile>> fileMap);

    @Mappings({
            @Mapping(target = "id", source = "extractRunVersion.id"),
            @Mapping(target = "extractRunVersionId", source = "extractRunVersion.id"),
            @Mapping(target = "extractRunFiles", expression = "java(this.toExtractRunFileDTOs(extractRunFiles, extractResults, extractTaskResults))"),
            @Mapping(target = "companyCodeList", expression = "java(extractRunFiles.stream().map(ExtractRunFile::getCompanyCode).collect(java.util.stream.Collectors.toSet()))")
    })
    public abstract ExtractRunVersionDTO toExtractRunDTO(ExtractRunVersion extractRunVersion, List<ExtractRunFile> extractRunFiles,
                                                         List<ExtractTask> extractResults, List<ExtractTaskResult> extractTaskResults);

    public List<ExtractRunFileDTO> toExtractRunFileDTOs(List<ExtractRunFile> extractRunFiles, List<ExtractTask> extractTasks,
                                                        List<ExtractTaskResult> extractTaskResults) {
        //提取结果map
        Map<Long, List<ExtractTaskResult>> extractTaskResultMap = extractTaskResults.stream().collect(Collectors.groupingBy(ExtractTaskResult::getExtractTaskId));
        List<ExtractRunFileDTO> extractRunFileDTOs = new ArrayList<>();
        Map<Long, List<ExtractTask>> collect = extractTasks.stream()
                .collect(Collectors.groupingBy(ExtractTask::getFileId, Collectors.toList()));
        for (ExtractRunFile extractRunFile : extractRunFiles) {
            ExtractRunFileDTO extractRunFileDTO = new ExtractRunFileDTO();
            extractRunFileDTO.setFileId(extractRunFile.getFileId());
            extractRunFileDTO.setFileName(extractRunFile.getFileName());
            extractRunFileDTO.setCompanyCode(extractRunFile.getCompanyCode());
            extractRunFileDTO.setStatus(extractRunFile.getStatus());
            extractRunFileDTO.setError(extractRunFile.getError());
            List<ExtractTaskDTO> extractTaskDTOs = new ArrayList<>();
            for (ExtractTask extractTask : collect.getOrDefault(extractRunFile.getFileId(), List.of())) {
                ExtractTaskDTO extractTaskDTO = new ExtractTaskDTO();
                extractTaskDTO.setId(extractTask.getId());
                extractTaskDTO.setPosition(extractTask.getPosition());
                extractTaskDTO.setStartPage(extractTask.getStartPage());
                extractTaskDTO.setEndPage(extractTask.getEndPage());

                List<ExtractTaskResult> taskResults = extractTaskResultMap.getOrDefault(extractTask.getId(), new ArrayList<>());
                //单个字段结果
                List<ExtractTaskResult> singleResults = taskResults.stream().filter(result -> result.getType() == ParameterTypeEnum.SINGLE).toList();
                extractTaskDTO.setSingleFieldResults(this.buildExtractTaskResultDtoList(singleResults));
                //组合字段
                Map<String, List<List<ExtractTaskResultDto>>> compositeMap = taskResults.stream()
                        .filter(result -> result.getType() == ParameterTypeEnum.COMPOSITE)
                        .collect(Collectors.groupingBy(ExtractTaskResult::getCompositeName, Collectors.collectingAndThen(
                                Collectors.groupingBy(ExtractTaskResult::getCompositeIndex),
                                map -> new TreeMap<>(map).values().stream().map(this::buildExtractTaskResultDtoList).toList())
                        ));

                extractTaskDTO.setCompositeFieldResults(compositeMap);
                extractTaskDTOs.add(extractTaskDTO);
            }
            extractRunFileDTO.setExtractTasks(extractTaskDTOs);
            extractRunFileDTOs.add(extractRunFileDTO);
        }
        return extractRunFileDTOs;
    }

    public abstract ExtractRunFile buildOcrMergedResult(Long projectId, Long extractRunId, Integer version, Long fileId, String result);

    public abstract ExtractRunFile buildExtractRunVersionFile(Long projectId, Integer version, String companyCode, String hash, Long fileId, String fileName, ExtractRunStatusEnum status);

    public abstract ExtractRunFile buildExtractRunVersionFile(Long projectId, Long extractRunId, Integer version, String companyCode, String hash, Long fileId, String fileName, ExtractRunStatusEnum status);

    @Mappings({
            @Mapping(target = "extractRunVersionId", source = "extractRunVersion.id"),
            @Mapping(target = "createByName", source = "extractRunVersion.createByName"),
            @Mapping(target = "updateByName", source = "extractRunVersion.updateByName"),
            @Mapping(target = "extractConfig.name", source = "extractRunVersion.extractConfig.name")
    })
    public abstract ExtractRunVersionDTO toExtractRunDTO(ExtractRunVersion extractRunVersion, List<ExtractRunFile> extractRunFiles);

    public abstract ExtractTaskResult buildExtractTaskResult(ParameterTypeEnum type, String compositeName, Long compositeIndex,
                                                             String primitiveName, String content, List<Polygon> polygons, String paraRange);

    public abstract ExtractTaskResult buildExtractTaskResult(Long projectId, Long extractRunId, Long extractTaskId, Integer version,
                                                             ParameterTypeEnum type, String compositeName, Long compositeIndex,
                                                             String primitiveName, String content);

    @Mappings({
            @Mapping(target = "projectId", source = "projectId"),
            @Mapping(target = "extractRunId", source = "extractRunId"),
            @Mapping(target = "extractTaskId", source = "extractTaskId"),
            @Mapping(target = "version", source = "version"),
            @Mapping(target = "createBy", source = "createBy"),
            @Mapping(target = "updateBy", source = "createBy"),
            @Mapping(target = "createTime", source = "createTime"),
            @Mapping(target = "updateTime", source = "createTime"),
            @Mapping(target = "createByName", source = "createByName"),
            @Mapping(target = "updateByName", source = "createByName")
    })
    public abstract ExtractTaskResult assembleExtractTaskResult(ExtractTaskResult extractTaskResult, Long projectId,
                                                                Long extractRunId, Long extractTaskId, Integer version,
                                                                String createBy, String createByName, LocalDateTime createTime);


    public abstract List<ExtractTaskResultDto> buildExtractTaskResultDtoList(List<ExtractTaskResult> extractTaskResults);

    public List<ExtractRunPageDTO.File> toFiles(ExtractRunVersion extractRunVersion, Map<Long, List<ExtractRunFile>> fileMap) {
        List<ExtractRunFile> fileList = fileMap.getOrDefault(extractRunVersion.getExtractRunId(), new ArrayList<>());
        return fileList.stream().map(file -> new ExtractRunPageDTO.File(file.getFileId(), file.getFileName())).toList();
    }

    public List<String> getCompanyCodeList(ExtractRunVersion extractRunVersion, Map<Long, List<ExtractRunFile>> fileMap) {
        List<ExtractRunFile> fileList = fileMap.getOrDefault(extractRunVersion.getExtractRunId(), new ArrayList<>());
        return fileList.stream().map(ExtractRunFile::getCompanyCode).distinct().toList();
    }

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "name", source = "name")
    })
    public abstract List<ExtractConfigDTO> toExtractConfigDTOList(List<ExtractConfig> extractConfigList);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    public abstract ExtractConfigDTO toExtractConfigDTO(ExtractConfig extractConfig);

    public ResultExportDTO toResultExportDTOList(List<ExtractTaskResultDto> taskResultDtos) {
        Map<String, String> contentMap = taskResultDtos.stream().collect(Collectors.toMap(ExtractTaskResultDto::getPrimitiveName
                , ExtractTaskResultDto::getContent, (x1, x2) -> x1));
        ResultExportDTO resultExportDTO = new ResultExportDTO();
        resultExportDTO.setInvoiceId(contentMap.getOrDefault("发票号", ""));
        resultExportDTO.setTaxAmount(contentMap.getOrDefault("税额", ""));
        resultExportDTO.setInvoiceCode(contentMap.getOrDefault("发票代码", ""));
        resultExportDTO.setVendorTaxId(contentMap.getOrDefault("销售方纳税人识别号", ""));
        resultExportDTO.setVendorName(contentMap.getOrDefault("销售方纳税人名称", ""));
        resultExportDTO.setSubTotal(contentMap.getOrDefault("金额", ""));
        return resultExportDTO;
    }


}
