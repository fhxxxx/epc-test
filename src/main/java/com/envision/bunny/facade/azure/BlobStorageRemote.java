package com.envision.bunny.facade.azure;

import cn.hutool.core.text.CharSequenceUtil;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.ResponseBase;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.sas.SasProtocol;
import com.envision.extract.infrastructure.response.BizException;
import com.envision.extract.infrastructure.response.ErrorCode;
import com.envision.extract.module.project.domain.enums.ProjectTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author wenjun.gu
 * @since 2025/8/14-15:38
 */
@Slf4j
@Service
@EnableConfigurationProperties(BlobStorageConfig.class)
public class BlobStorageRemote {
    private final BlobStorageConfig config;
    private final BlobServiceClient client;
    private final BlobServiceAsyncClient asyncClient;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public BlobStorageRemote(BlobStorageConfig config, RedisTemplate<String, String> redisTemplate) {
        this.config = config;
        this.redisTemplate = redisTemplate;
        this.client = new BlobServiceClientBuilder()
                .endpoint(config.getEndpoint())
                .credential(new StorageSharedKeyCredential(config.getAccountName(), config.getAccountKey()))
                .buildClient();
        this.asyncClient = new BlobServiceClientBuilder()
                .endpoint(config.getEndpoint())
                .credential(new StorageSharedKeyCredential(config.getAccountName(), config.getAccountKey()))
                .buildAsyncClient();
    }

    public boolean exists(String filename, ProjectTypeEnum type) {
        BlobContainerClient blobContainer = client.getBlobContainerClient(config.getContainerNameByProject(type));
        return blobContainer.getBlobClient(filename).exists();
    }

    public void upload(String filename, InputStream data, ProjectTypeEnum type) {
        BlobContainerClient blobContainer = client.getBlobContainerClient(config.getContainerNameByProject(type));
        blobContainer.getBlobClient(filename).upload(data);
    }

    public InputStream loadStream(String filename, ProjectTypeEnum type) {
        BlobContainerClient blobContainer = client.getBlobContainerClient(config.getContainerNameByProject(type));
        return blobContainer.getBlobClient(filename).downloadContent().toStream();
    }

    public void loadStream(String filename, OutputStream outputStream, ProjectTypeEnum type) {
        BlobContainerClient blobContainer = client.getBlobContainerClient(config.getContainerNameByProject(type));
        blobContainer.getBlobClient(filename).downloadStream(outputStream);
    }

    public List<String> listBlobs(String prefix, ProjectTypeEnum type) {
        BlobContainerClient blobContainer = client.getBlobContainerClient(config.getContainerNameByProject(type));
        PagedIterable<BlobItem> blobItems = blobContainer.listBlobsByHierarchy(prefix);
        return blobItems.stream().map(BlobItem::getName).toList();
    }

    public long getBlobSize(String filename, ProjectTypeEnum type) {
        BlobContainerClient blobContainer = client.getBlobContainerClient(config.getContainerNameByProject(type));
        return blobContainer.getBlobClient(filename).getProperties().getBlobSize();
    }

    public void loadStream(String filename, long offset, long count, OutputStream responseStream, ProjectTypeEnum type) {
        try {
            BlobContainerAsyncClient blobContainer = asyncClient.getBlobContainerAsyncClient(config.getContainerNameByProject(type));
            BlobAsyncClient blobClient = blobContainer.getBlobAsyncClient(filename);
            blobClient.downloadStreamWithResponse(new BlobRange(offset, count), null, null, false)
                    .flatMapMany(ResponseBase::getValue)
                    .doOnNext(byteBuffer -> {
                        try {
                            byte[] bytes = new byte[byteBuffer.remaining()];
                            byteBuffer.get(bytes);
                            responseStream.write(bytes);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).then().block();
            responseStream.flush();
        } catch (Exception e) {
            log.warn("下载文件失败", e);
            throw new BizException(ErrorCode.IGNORE_ALARM, "下载文件失败");
        }
    }

    public void copy(String sourceFilename, String targetFilename, ProjectTypeEnum type) {
        BlobContainerClient blobContainer = client.getBlobContainerClient(config.getContainerNameByProject(type));
        BlobClient sourceBlobClient = blobContainer.getBlobClient(sourceFilename);
        BlobClient targetBlobClient = blobContainer.getBlobClient(targetFilename);
        SyncPoller<BlobCopyInfo, Void> poller = targetBlobClient.beginCopy(sourceBlobClient.getBlobUrl(), Duration.ofSeconds(2));
        poller.waitForCompletion();
    }

    public void delete(String filename, ProjectTypeEnum type) {
        BlobContainerClient blobContainer = client.getBlobContainerClient(config.getContainerNameByProject(type));
        blobContainer.getBlobClient(filename).deleteIfExists();
    }

    public String readImageAsBytes(String filePath, ProjectTypeEnum type) {
        // 先判断文件是否存在
        if (CharSequenceUtil.isBlank(filePath) || !exists(filePath, type)) {
            return "";
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             InputStream inputStream = getInputStream(filePath, type)) {

            // 将文件流读取到字节数组
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            return "";
        }
    }

    private InputStream getInputStream(String filePath, ProjectTypeEnum type) {
        BlobContainerClient blobContainer = client.getBlobContainerClient(config.getContainerNameByProject(type));
        return blobContainer.getBlobClient(filePath).openInputStream();
    }

    public String generateSasUrl(String filename, ProjectTypeEnum type) {
        BlobContainerClient blobContainer = client.getBlobContainerClient(config.getContainerNameByProject(type));
        BlobClient blobClient = blobContainer.getBlobClient(filename);

        String hashedKey = DigestUtils.md5DigestAsHex(config.getAccountKey().getBytes(StandardCharsets.UTF_8));
        String cacheKey = String.format(type.getAzureToken(), config.getAccountName(), hashedKey);
        String cachedToken = redisTemplate.opsForValue().get(cacheKey);
        Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);

        if (cachedToken != null && ttl > 3600) {
            return blobClient.getBlobUrl() + "?" + cachedToken;
        }

        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(2);
        BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiryTime, permissions)
                .setProtocol(SasProtocol.HTTPS_ONLY);

        String sasToken = blobContainer.generateSas(sasValues);
        redisTemplate.opsForValue().set(cacheKey, sasToken, 110, TimeUnit.MINUTES);

        return blobClient.getBlobUrl() + "?" + sasToken;
    }
}
