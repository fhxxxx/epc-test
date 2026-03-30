package com.envision.bunny.demo.integration.doccenter;

import com.envision.bunny.facade.document.BASE64DecodedMultipartFile;
import com.envision.bunny.facade.document.DocCenterRemote;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Base64;

/**
 * 文档中心测试
 *
 * @author jingjing.dong
 * @since 2021/6/19-16:33
 */
@RestController
@RequestMapping("/doc")
public class DocCenterTest {
    @Autowired
    DocCenterRemote remote;

    /**
     * 上传文件
     *
     * @param type 文件类型|offer
     * @param file 文件附件
     */
    @PostMapping("/upload")
    public String test(String type, MultipartFile file) {
        return remote.upload(type, file.getResource());
    }

    /**
     * 下载
     *
     * @param docId    文档中心ID|E627ECB8DFDD4B05B85DDB5F5191613B
     * @param response 返回体
     * @return 下载的文件
     */
    @GetMapping("/download/{docId}")
    public StreamingResponseBody test2(@PathVariable String docId, HttpServletResponse response) {
        return remote.download(docId, response);
    }

    /**
     * 上传Base64内容文件
     *
     * @param type      文件类型|offer
     * @param name      文件名|刘峰
     * @param base64Str Base64编码的附件内容|6L+Z5piv5LiA5Liq5rWL6K+V55So5L6L
     */
    @PostMapping("/upload-base64")
    public String test3(String type, String name, String base64Str) {
        final BASE64DecodedMultipartFile base64DecodedMultipartFile = new BASE64DecodedMultipartFile(Base64.getDecoder().decode(base64Str),
                name + ".pdf", "pdf");
        return remote.upload(type, base64DecodedMultipartFile.getResource());
    }


}
