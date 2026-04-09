package com.envision.epc.module.font;

import com.envision.epc.facade.azure.BlobStorageRemote;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Sync fonts from blob storage to local path.
 * Aspose Words/PDF is removed; this loader only keeps the file sync capability.
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
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                try (OutputStream os = new FileOutputStream(localFile)) {
                    blobService.loadStream(blobPath, os);
                    log.info("Font downloaded: {}", fileName);
                } catch (IOException e) {
                    log.error("Font download failed: {}", fileName, e);
                }
            }
            log.info("Font files synced to local path: {}", localFontPath);
        } catch (Exception e) {
            log.error("Font initialization failed", e);
        }
    }
}

