package com.envision.epc.module.extract.web;

import com.envision.epc.facade.azure.BlobStorageRemote;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提取任务
 *
 * @author wenjun.gu
 * @since 2025/8/12-16:46
 */
@Validated
@RestController
@RequestMapping("/extract-runs")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ExtractRunController {
    private final BlobStorageRemote blobStorageRemote;

    /**
     * 测试azure的blobtoken
     *
     * @return
     *
     */
    @GetMapping("/test/fileToken")
    public String getExtract(String fileName) {
        return blobStorageRemote.generateSasUrl(fileName);
    }


}
