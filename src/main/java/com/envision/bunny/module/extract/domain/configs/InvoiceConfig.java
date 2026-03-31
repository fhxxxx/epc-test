package com.envision.bunny.module.extract.domain.configs;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.envision.extract.facade.dify.DifyRemoteService;
import com.envision.extract.infrastructure.mybatis.MyIdGenerator;
import com.envision.extract.infrastructure.util.JsonUtils;
import com.envision.extract.module.extract.domain.*;
import com.envision.extract.module.extract.domain.ocr.AliyunOcrResult;
import com.envision.extract.module.extract.domain.ocr.SliceRect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 发票提取
 *
 * @author gangxiang.guan
 * @date 2025/10/16 11:56
 */
@Slf4j
public class InvoiceConfig extends ExtractConfig {

    private static final List<String> EPC_SPECIAL_PROCESS_FIELDS = List.of("发票号", "销售方纳税人识别号", "购买方纳税人识别号");


    @Override
    public List<ExtractTaskResult> extract(OcrTask ocrTask, String content, DifyRemoteService difyRemoteService, MyIdGenerator generator) throws JsonProcessingException {
        //阿里云模型遇到空页的话，会直接报错不会返回提取结果，兼容一下返回空数据
        if (CharSequenceUtil.isBlank(content)) {
            return generateEmptyResults();
        } else {
            return this.assembleResult(content, ocrTask.getPosition());
        }
    }

    /**
     * 根据识别结果与配置，组装参数
     *
     * @return
     */
    private List<ExtractTaskResult> assembleResult(String markdownResult, Integer pageNumber) {
        JsonNode node = JsonUtils.toJsonNode(markdownResult);
        List<ParameterConfig> singleConfigList = this.getParameterConfigs().stream()
                .filter(x -> x.getType() == ParameterTypeEnum.SINGLE).toList();
        List<ExtractTaskResult> resultList = new ArrayList<>();
        double width = node.get("width").asDouble(0d);
        double height = node.get("height").asDouble(0d);
        int angle = node.get("angle").asInt(0);
        //计算偏移量
        SliceRect sliceRect = JsonUtils.toObjValue(node.get("sliceRect"), SliceRect.class);

        if (node.has("prism_keyValueInfo")) {
            List<AliyunOcrResult> aliyunOcrResults = JsonUtils.toObjValues((ArrayNode) node.get("prism_keyValueInfo"), AliyunOcrResult.class);

            if (CollUtil.isNotEmpty(aliyunOcrResults)) {
                Map<String, AliyunOcrResult> resultMap = aliyunOcrResults.stream().collect(Collectors.toMap(
                        AliyunOcrResult::getKey, Function.identity(), (x1, x2) -> x1));

                //根据配置捞字段
                for (ParameterConfig singleConfig : singleConfigList) {
                    PrimitiveConfig primitiveConfig = singleConfig.getPrimitiveConfigs().get(0);
                    String content = "";
                    List<Polygon> polygons = new ArrayList<>();
                    if (resultMap.containsKey(primitiveConfig.getKey()) && CollUtil.isNotEmpty(resultMap.get(primitiveConfig.getKey()).getValuePos())) {
                        AliyunOcrResult aliyunOcrResult = resultMap.get(primitiveConfig.getKey());
                        content = aliyunOcrResult.getValue();

                        Polygon polygon = new Polygon();
                        polygon.setWidth(width);
                        polygon.setHeight(height);
                        List<Double> polygonList = aliyunOcrResult.getValuePos().stream().map(valuePosBean ->
                                        List.of(valuePosBean.getX(), valuePosBean.getY()))
                                .flatMap(Collection::stream).toList();
                        List<Double> doubles = rotateBack(polygonList, angle, sliceRect.getInnerWidth(), sliceRect.getInnerHeight());
                        for (int i = 0; i < doubles.size(); i = i + 2) {
                            doubles.set(i, doubles.get(i) + sliceRect.getWidthOffset());
                            doubles.set(i + 1, doubles.get(i + 1) + sliceRect.getHeightOffset());
                        }
                        polygon.setPolygon(doubles);
                        polygon.setPageNumber(pageNumber);
                        polygons = List.of(polygon);
                    }

                    resultList.add(new ExtractTaskResult(ParameterTypeEnum.SINGLE, primitiveConfig.getName(), content, polygons));
                }
            }
        }
        return this.specialProcess(resultList);
    }

    /**
     * 旋转坐标反变换
     *
     * @param rotatedPoints 旋转后的坐标点（长度8，四个点x,y）
     * @param angle         顺时针旋转角度
     * @param orgWidth      原图宽度
     * @param orgHeight     原图高度
     * @return 原图坐标点（长度8，四个点x,y）
     */
    public static List<Double> rotateBack(List<Double> rotatedPoints, int angle, double orgWidth, double orgHeight) {
        List<Double> originalPoints = new ArrayList<>(8);

        // 计算旋转后的宽高
        double rotatedWidth = orgWidth;
        double rotatedHeight = orgHeight;
        if (angle == 90 || angle == 270) {
            rotatedWidth = orgHeight;
            rotatedHeight = orgWidth;
        }

        // 特殊角度优化
        int normalizedAngle = ((angle % 360) + 360) % 360; // 转为0~359的正角度

        if (normalizedAngle == 0) { // 0°
            originalPoints.addAll(rotatedPoints);

        } else if (normalizedAngle == 90) { // 顺时针 90°
            for (int i = 0; i < rotatedPoints.size(); i += 2) {
                double x = rotatedPoints.get(i);
                double y = rotatedPoints.get(i + 1);
                double origX = rotatedHeight - y;
                double origY = x;
                originalPoints.add(origX);
                originalPoints.add(origY);
            }

        } else if (normalizedAngle == 180) { // 顺时针 180°
            for (int i = 0; i < rotatedPoints.size(); i += 2) {
                double x = rotatedPoints.get(i);
                double y = rotatedPoints.get(i + 1);
                double origX = orgWidth - x;
                double origY = orgHeight - y;
                originalPoints.add(origX);
                originalPoints.add(origY);
            }

        } else if (normalizedAngle == 270) { // 顺时针 270°
            for (int i = 0; i < rotatedPoints.size(); i += 2) {
                double x = rotatedPoints.get(i);
                double y = rotatedPoints.get(i + 1);
                double origX = y;
                double origY = orgHeight - x;
                originalPoints.add(origX);
                originalPoints.add(origY);
            }

        } else {
            // 通用角度（用三角函数）
            double rad = Math.toRadians(-normalizedAngle); // 逆旋转
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);

            double cx = rotatedWidth / 2.0;
            double cy = rotatedHeight / 2.0;

            for (int i = 0; i < rotatedPoints.size(); i += 2) {
                double x = rotatedPoints.get(i);
                double y = rotatedPoints.get(i + 1);

                double dx = x - cx;
                double dy = y - cy;

                double origX = cos * dx - sin * dy + cx;
                double origY = sin * dx + cos * dy + cy;

                originalPoints.add(origX);
                originalPoints.add(origY);
            }
        }

        return originalPoints;
    }

    @Override
    protected List<ExtractTaskResult> specialProcess(List<ExtractTaskResult> results) {
        results.stream().filter(result -> EPC_SPECIAL_PROCESS_FIELDS.contains(result.getPrimitiveName())
                && result.getContent().startsWith(":")).forEach(result ->
                result.setContent(result.getContent().replaceFirst(":", "")));
        Map<String, ExtractTaskResult> extractTaskResultMap = results.stream().collect(Collectors.toMap(
                ExtractTaskResult::getPrimitiveName, Function.identity(), (x1, x2) -> x1));
        Map<String, String> contentMap = results.stream().collect(Collectors.toMap(
                ExtractTaskResult::getPrimitiveName, ExtractTaskResult::getContent, (x1, x2) -> x1));
        BigDecimal subTotal = transferToDecimal(contentMap.getOrDefault("金额", "0"));
        BigDecimal totalTax = transferToDecimal(contentMap.getOrDefault("税额", "0"));

        extractTaskResultMap.getOrDefault("税额", new ExtractTaskResult()).setContent(totalTax.toString());
        extractTaskResultMap.getOrDefault("金额", new ExtractTaskResult()).setContent(subTotal.toString());

        return results;
    }

    private BigDecimal transferToDecimal(String content) {
        try {
            return new BigDecimal(content);
        } catch (Exception e) {
            return new BigDecimal(0);
        }
    }

    private List<ExtractTaskResult> generateEmptyResults() {
        List<ParameterConfig> singleConfigList = this.getParameterConfigs().stream()
                .filter(x -> x.getType() == ParameterTypeEnum.SINGLE).toList();

        return singleConfigList.stream().map(config -> new ExtractTaskResult(ParameterTypeEnum.SINGLE,
                config.getPrimitiveConfigs().get(0).getName(), "", new ArrayList<>())).toList();
    }
}
