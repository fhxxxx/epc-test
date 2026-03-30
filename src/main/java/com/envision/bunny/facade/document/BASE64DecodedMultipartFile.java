package com.envision.bunny.facade.document;

import com.google.common.collect.ImmutableMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Base64;
import java.util.Map;

/**
 * @author jingjing.dong
 * @since 2021/8/25-12:28
 */
public class BASE64DecodedMultipartFile implements MultipartFile {

    private final byte[] imgContent;
    private final String name;
    private final String contentType;

    private static final Map<String, String> typeMap = ImmutableMap.of("png", "image/png", "jpg", "image/jpg", "pdf",
            "application/pdf", "doc", "text/html", "docx", "text/html");

    public BASE64DecodedMultipartFile(byte[] imgContent, String name, String type) {
        this.imgContent = imgContent;
        this.name = name.split(";")[0];
        this.contentType = typeMap.get(type);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return name;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return imgContent == null || imgContent.length == 0;
    }

    @Override
    public long getSize() {
        return imgContent.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return imgContent;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(imgContent);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try(final FileOutputStream fileOutputStream = new FileOutputStream(dest)) {
            fileOutputStream.write(imgContent);
        }
    }

    public static MultipartFile base64ToMultipart(String base64, String name, String type) {
        return new BASE64DecodedMultipartFile(Base64.getDecoder().decode(base64), name, type);
    }

}