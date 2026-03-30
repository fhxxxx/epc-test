package com.envision.bunny.demo.capability.filetype;

import com.envision.bunny.infrastructure.filter.upload.UploadFileType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件类型校验
 *
 * @author chaoyue.zhao1
 * @since 2025/12/26-11:13
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class UploadFileTypeTest {
    /**
     * 上传文件
     * @param file 文件
     * @param name 姓名
     * @author jingjing.dong
     * @since 2021/5/7 20:47
     */
    @PostMapping("/upload")
    @UploadFileType(fileType = "png,pdf")
    public void syncProduction(MultipartFile file, String name) {
        System.out.println("update load a file original named " + file.getOriginalFilename());
        System.out.println("update load a file and renamed :[{" + name + "}]");
    }
}
