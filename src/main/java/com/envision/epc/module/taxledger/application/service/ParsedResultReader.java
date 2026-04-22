package com.envision.epc.module.taxledger.application.service;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ParsedResultReader {
    private final BlobStorageRemote blobStorageRemote;
    private final ObjectMapper objectMapper;

    public <T> T readParsedData(String parseResultBlobPath, Class<T> type) {
        JsonNode root = readJson(parseResultBlobPath);
        JsonNode dataNode = root.path("result").path("data");
        return convertNode(dataNode, type);
    }

    public <T> List<T> readParsedList(String parseResultBlobPath, Class<T> itemType) {
        JsonNode root = readJson(parseResultBlobPath);
        JsonNode dataNode = root.path("result").path("data");
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
        return convertNode(dataNode, listType);
    }

    public <T> T readNodeOutputData(String nodeOutputBlobPath, Class<T> type) {
        JsonNode root = readJson(nodeOutputBlobPath);
        JsonNode dataNode = root.path("outputData");
        return convertNode(dataNode, type);
    }

    private JsonNode readJson(String blobPath) {
        if (blobPath == null || blobPath.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "blob path is blank");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            blobStorageRemote.loadStream(blobPath, output);
            return objectMapper.readTree(output.toByteArray());
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "read blob json failed: " + e.getMessage());
        }
    }

    private <T> T convertNode(JsonNode node, Class<T> type) {
        try {
            return objectMapper.convertValue(node, type);
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "convert json failed: " + e.getMessage());
        }
    }

    private <T> T convertNode(JsonNode node, JavaType type) {
        try {
            return objectMapper.convertValue(node, type);
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "convert json list failed: " + e.getMessage());
        }
    }
}
