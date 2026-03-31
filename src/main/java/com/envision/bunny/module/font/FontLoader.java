package com.envision.bunny.module.font;

import com.aspose.words.FontSettings;
import com.envision.extract.facade.azure.BlobStorageRemote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2025/09/18-15:47
 */
@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FontLoader {
    private final BlobStorageRemote blobService;

    @Value("${custom.font.efs-font}")
    private String localFontPath;
    @Value("${custom.font.blob-font}")
    private String blobFontPath;

    @PostConstruct
    public void init() {
        try {
            List<String> blobFiles = blobService.listBlobs(blobFontPath);
            for (String blobPath : blobFiles) {
                String fileName = blobPath.substring(blobFontPath.length());
                File localFile = new File(localFontPath + fileName);

                if (localFile.exists()) {
                    continue;
                }
                File parentDir = localFile.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                try (OutputStream os = new FileOutputStream(localFile)) {
                    blobService.loadStream(blobPath, os);
                    log.info("下载完成:[{}]", fileName);
                } catch (IOException e) {
                    log.error("下载失败:[{}]", fileName, e);
                }
            }
            FontSettings.getDefaultInstance().setFontsFolder(localFontPath, false);
            log.info("Aspose 字体加载成功！");
        } catch (Exception e) {
            log.error("初始化字体加载失败",e);
        }
    }
}
