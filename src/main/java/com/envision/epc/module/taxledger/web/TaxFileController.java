package com.envision.epc.module.taxledger.web;

import com.envision.epc.module.taxledger.application.service.TaxFileService;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.TaxFileRecord;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件管理接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/files")
public class TaxFileController {
    private final TaxFileService fileService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public TaxFileRecord upload(@RequestParam String companyCode,
                                @RequestParam String yearMonth,
                                @RequestParam FileCategoryEnum fileCategory,
                                @RequestPart MultipartFile file) throws IOException {
        return fileService.upload(companyCode, yearMonth, fileCategory, file);
    }

    /**
     * 查询文件列表
     */
    @GetMapping
    public List<TaxFileRecord> list(@RequestParam String companyCode, @RequestParam String yearMonth) {
        return fileService.list(companyCode, yearMonth);
    }

    /**
     * 下载文件
     */
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        fileService.download(id, response);
    }
}
