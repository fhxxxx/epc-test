package com.envision.bunny.facade.document;

import com.envision.bunny.facade.platform.PlatformRemote;
import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.util.RestClientUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;

/**
 * @author jingjing.dong
 * @since 2021/6/19-16:27
 */
@Component
@EnableConfigurationProperties(DocCenterConfig.class)
public class DocCenterRemote {
    @Autowired
    PlatformRemote platformRemote;
    @Autowired
    DocCenterConfig config;
    @Autowired
    RestClient restClient;

    /**
     * 根据文档类型，选择策略，上传文档
     *
     * @param docType  文档类型
     * @param resource 上传的资源
     * @return 文档中心ID
     */
    public String upload(String docType, Resource resource) {
        String token = platformRemote.getToken(config.getService());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("X-ENP-AUTH", token);
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("policyId", config.getPolicy().get(docType));
        params.add("file", resource);
        params.add("onBehalfOf", "SYS10064");
        DocUploadResp docUploadResp = restClient.post()
                .uri(config.getUploadUrl())
                .body(params)
                .headers(h -> h.addAll(headers))
                .retrieve().body(DocUploadResp.class);
        return Objects.requireNonNull(docUploadResp).getId();
    }

    public Resource download(String docId) {
        String token = platformRemote.getToken(config.getService());
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-ENP-AUTH", token);
        return RestClientUtils.getWithHeaders(config.getDownloadUrl(), headers, Resource.class, docId);
    }

    public StreamingResponseBody download(String docId, HttpServletResponse response) {
        final Resource resource = this.download(docId);
        try {
            String fileName = URLEncoder.encode(Objects.requireNonNull(resource.getFilename()), "UTF-8");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName);
        } catch (UnsupportedEncodingException e) {
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "下载文件失败");
        }
        return outputStream -> FileCopyUtils.copy(resource.getInputStream(), outputStream);
    }
}
