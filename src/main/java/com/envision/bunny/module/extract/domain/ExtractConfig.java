package com.envision.bunny.module.extract.domain;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.envision.extract.facade.dify.DifyRemoteService;
import com.envision.extract.infrastructure.mybatis.MyIdGenerator;
import com.envision.extract.infrastructure.util.JsonUtils;
import com.envision.extract.module.extract.domain.ocr.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:10
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ExtractConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String instruction;

    /**
     * 参数配置
     */
    private List<ParameterConfig> parameterConfigs;
    /**
     * 每次提取页数
     * @mock 10
     */
    private Integer pagesPerExtraction;

    public List<ExtractTaskResult> extract(OcrTask ocrTask, String content, DifyRemoteService difyRemoteService, MyIdGenerator generator) throws JsonProcessingException {
        JsonNode jsonNode = JsonUtils.toJsonNode(content);
        OcrData ocrData = prepareData(jsonNode);
        String extractSchema = this.toExtractSchema(false);
        String result = difyRemoteService.extract(instruction == null ? "" : instruction, extractSchema, ocrData.getParaTaggedContent());
        return this.updateResult(result, ocrData, generator);
    }

    @Getter
    @Setter
    @ToString
    static class Event {
        int priority;
        Object object;

        Event(int priority, Object object) {
            this.priority = priority;
            this.object = object;
        }
    }

    protected OcrData prepareData(JsonNode ocrResult) {
        List<Paragraph> paragraphs = new ArrayList<>();
        List<Line> lines = new ArrayList<>();
        List<Word> words = new ArrayList<>();
        List<Section> sections = new ArrayList<>();
        String paraTaggedContent = "";
        String wordTaggedContent = "";

        JsonNode analyzeResult = ocrResult.get("analyzeResult");
        JsonNode contentNode = null;
        if (analyzeResult != null) {
            contentNode = analyzeResult.get("content");
        }

        if (analyzeResult != null && contentNode != null) {
            Map<Integer, List<Event>> startMap = new HashMap<>();
            Map<Integer, List<Event>> endMap = new HashMap<>();
            Map<Integer, double[]> pageSizeMap = new HashMap<>();

            int wordIndex = 0;
            int lineIndex = 0;
            int paragraphIndex = 0;
            int sectionIndex = 0;

            JsonNode pages = analyzeResult.get("pages");
            if (pages != null) {
                for (JsonNode page : pages) {
                    int pageNumber = page.get("pageNumber").asInt();
                    double width = page.get("width").asDouble();
                    double height = page.get("height").asDouble();
                    pageSizeMap.put(pageNumber, new double[]{width, height});

                    JsonNode wordsNode = page.get("words");
                    if (wordsNode != null) {
                        for (JsonNode wordNode : wordsNode) {
                            JsonNode polygonNode = wordNode.get("polygon");
                            com.envision.extract.module.extract.domain.ocr.Polygon polygon = new com.envision.extract.module.extract.domain.ocr.Polygon(
                                    polygonNode.get(0).asDouble(), polygonNode.get(1).asDouble(),
                                    polygonNode.get(2).asDouble(), polygonNode.get(3).asDouble(),
                                    polygonNode.get(4).asDouble(), polygonNode.get(5).asDouble(),
                                    polygonNode.get(6).asDouble(), polygonNode.get(7).asDouble()
                            );
                            int start = wordNode.get("span").get("offset").asInt();
                            int end = start + wordNode.get("span").get("length").asInt();
                            Span span = new Span(start, end);
                            Word word = new Word(wordIndex++, wordNode.get("content").asText(), span, polygon, pageNumber);
                            words.add(word);
                            List<Event> startList = startMap.getOrDefault(start, new ArrayList<>());
                            List<Event> endList = endMap.getOrDefault(end, new ArrayList<>());
                            startList.add(new Event(2, word));
                            endList.add(new Event(1, word));
                            if (!startMap.containsKey(start)) {
                                startMap.put(start, startList);
                            }
                            if (!endMap.containsKey(end)) {
                                endMap.put(end, endList);
                            }
                        }
                    }

                    JsonNode linesNode = page.get("lines");
                    if (linesNode != null) {
                        for (JsonNode lineNode : linesNode) {
                            List<Span> spans = new ArrayList<>();
                            for (JsonNode span : lineNode.get("spans")) {
                                int start = span.get("offset").asInt();
                                int end = start + span.get("length").asInt();
                                spans.add(new Span(start, end));
                            }
                            Line line = new Line(lineIndex++, spans, pageNumber);
                            lines.add(line);
                            for (Span span : spans) {
                                List<Event> startList = startMap.getOrDefault(span.getStart(), new ArrayList<>());
                                List<Event> endList = endMap.getOrDefault(span.getEnd(), new ArrayList<>());
                                startList.add(new Event(3, line));
                                endList.add(new Event(3, line));
                                if (!startMap.containsKey(span.getStart())) {
                                    startMap.put(span.getStart(), startList);
                                }
                                if (!endMap.containsKey(span.getEnd())) {
                                    endMap.put(span.getEnd(), endList);
                                }
                            }
                        }
                    }
                }
            }

            JsonNode paragraphsNode = analyzeResult.get("paragraphs");
            if (paragraphsNode != null) {
                for (JsonNode paragraphNode : paragraphsNode) {
                    List<Span> spans = new ArrayList<>();
                    for (JsonNode span : paragraphNode.get("spans")) {
                        int start = span.get("offset").asInt();
                        int end = start + span.get("length").asInt();
                        spans.add(new Span(start, end));
                    }
                    ObjectNode boundingRegion = (ObjectNode) paragraphNode.get("boundingRegions").get(0);
                    Integer pageNumber = boundingRegion.get("pageNumber").asInt();
                    double[] doubles = pageSizeMap.get(pageNumber);
                    if (doubles != null) {
                        boundingRegion.put("width", doubles[0]);
                        boundingRegion.put("height", doubles[1]);
                    }
                    JsonNode roleNode = paragraphNode.get("role");

                    Paragraph paragraph = new Paragraph(paragraphIndex++, spans, boundingRegion, roleNode == null ? null : roleNode.asText(), pageNumber);
                    paragraphs.add(paragraph);
                    for (Span span : spans) {
                        List<Event> startList = startMap.getOrDefault(span.getStart(), new ArrayList<>());
                        List<Event> endList = endMap.getOrDefault(span.getEnd(), new ArrayList<>());
                        startList.add(new Event(1, paragraph));
                        endList.add(new Event(2, paragraph));
                        if (!startMap.containsKey(span.getStart())) {
                            startMap.put(span.getStart(), startList);
                        }
                        if (!endMap.containsKey(span.getEnd())) {
                            endMap.put(span.getEnd(), endList);
                        }
                    }
                }
            }

            JsonNode sectionsNode = analyzeResult.get("sections");
            if (sectionsNode != null) {
                for (JsonNode sectionNode : sectionsNode) {
                    List<Span> spans = new ArrayList<>();
                    List<String> elements = new ArrayList<>();
                    for (JsonNode span : sectionNode.get("spans")) {
                        int start = span.get("offset").asInt();
                        int end = start + span.get("length").asInt();
                        spans.add(new Span(start, end));
                    }
                    for (JsonNode element : sectionNode.get("elements")) {
                        elements.add(element.asText());
                    }
                    Section section = new Section(sectionIndex++, spans, elements);
                    sections.add(section);
                    for (Span span : spans) {
                        List<Event> startList = startMap.getOrDefault(span.getStart(), new ArrayList<>());
                        List<Event> endList = endMap.getOrDefault(span.getEnd(), new ArrayList<>());
                        startList.add(new Event(1, section));
                        endList.add(new Event(2, section));
                        if (!startMap.containsKey(span.getStart())) {
                            startMap.put(span.getStart(), startList);
                        }
                        if (!endMap.containsKey(span.getEnd())) {
                            endMap.put(span.getEnd(), endList);
                        }
                    }
                }
            }

            String content = contentNode.asText();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                if (endMap.containsKey(i)) {
                    List<Event> events = endMap.get(i);
                    events.sort(Comparator.comparingInt(Event::getPriority));
                    for (Event event : events) {
                        int index = i;
                        if (event.getObject() instanceof Word word) {
//                            String tag = String.format("[%s]", word.getIndex());
//                            sb.append(tag);
                            Span span = word.getSpan();
                            span.setEnd(sb.length());
                        }  else if (event.getObject() instanceof Line line) {
                            line.getSpans().stream()
                                    .filter(span -> span.getEnd() == index)
                                    .forEach(span -> span.setEnd(sb.length()));
                        } else if (event.getObject() instanceof Paragraph paragraph) {
                            String tag = String.format("[/p%s]", paragraph.getIndex());
                            sb.append(tag);
                            paragraph.getSpans().stream()
                                    .filter(span -> span.getEnd() == index)
                                    .forEach(span -> span.setEnd(sb.length()));
                        } else if (event.getObject() instanceof Section section) {
                            section.getSpans().stream()
                                    .filter(span -> span.getEnd() == index)
                                    .forEach(span -> span.setEnd(sb.length()));
                        }
                    }
                }

                if (startMap.containsKey(i)) {
                    List<Event> events = startMap.get(i);
                    events.sort(Comparator.comparingInt(Event::getPriority));
                    for (Event event : events) {
                        int index = i;
                        if (event.getObject() instanceof Word word) {
                            Span span = word.getSpan();
                            span.setStart(sb.length());
                        } else if (event.getObject() instanceof Line line) {
                            line.getSpans().stream()
                                    .filter(span -> span.getStart() == index)
                                    .forEach(span -> span.setStart(sb.length()));
                        } else if (event.getObject() instanceof Paragraph paragraph) {
                            paragraph.getSpans().stream()
                                    .filter(span -> span.getStart() == index)
                                    .forEach(span -> span.setStart(sb.length()));
                            String tag = String.format("[p%s]", paragraph.getIndex());
                            sb.append(tag);
                        } else if (event.getObject() instanceof Section section) {
                            section.getSpans().stream()
                                    .filter(span -> span.getStart() == index)
                                    .forEach(span -> span.setStart(sb.length()));
                        }
                    }
                }

                sb.append(content.charAt(i));
            }
            paraTaggedContent = sb.toString();
        }
        return new OcrData(words, lines, paragraphs, sections, paraTaggedContent, wordTaggedContent);
    }

    protected List<ExtractTaskResult> updateResult(String result, OcrData ocrData, MyIdGenerator generator) throws JsonProcessingException {
        List<ExtractTaskResult> resultList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(result);

        for (ParameterConfig parameterConfig : this.getParameterConfigs()) {
            if (parameterConfig.getType() == ParameterTypeEnum.SINGLE) {
                //组装result
                ExtractTaskResult singleFieldResult = this.assembleFieldResult(node, parameterConfig.getPrimitiveConfigs().get(0)
                        , ocrData, ParameterTypeEnum.SINGLE, null, null);
                resultList.add(singleFieldResult);
            } else {
                String parameterConfigName = parameterConfig.getName();
                if (node.has(parameterConfigName)) {
                    JsonNode parameterNodes = node.get(parameterConfigName);
                    for (JsonNode parameterNode : parameterNodes) {
                        //生成一个indexId用于分组
                        Number compositeIndex = generator.nextId(null);
                        for (PrimitiveConfig primitiveConfig : parameterConfig.getPrimitiveConfigs()) {
                            ExtractTaskResult compositeFieldResult = this.assembleFieldResult(parameterNode, primitiveConfig,
                                    ocrData, ParameterTypeEnum.COMPOSITE, parameterConfigName, compositeIndex.longValue());
                            resultList.add(compositeFieldResult);
                        }
                    }
                }
            }
        }
        return this.specialProcess(resultList);
    }

    /**
     * 组装单个字段提取结果
     * 注意：该方法不仅会返回一个result，还会对JsonNode对象操作，往里塞入一个polygons
     *
     * @param node            字段的提取结果json
     * @param primitiveConfig 字段配置
     * @param ocrData         ocr结果
     * @return
     */
    protected ExtractTaskResult assembleFieldResult(JsonNode node, PrimitiveConfig primitiveConfig, OcrData ocrData,
                                                  ParameterTypeEnum type, String compositeName, Long compositeIndex) {
        ObjectMapper mapper = new ObjectMapper();
        String name = primitiveConfig.getName();
        String paraRange = "";
        JsonNode jsonNode = mapper.createObjectNode();
        List<Polygon> polygons = new ArrayList<>();
        if (node.has(name) && node.get(name).has("para_range")) {
            paraRange = node.get(name).get("para_range").asText();
            jsonNode = node.get(name);
            List<String> pageRanges = Arrays.stream(paraRange.split(",")).toList();
            for (String pageRange : pageRanges) {
                String[] split;
                if (pageRange.contains("-")) {
                    split = pageRange.split("-");
                } else {
                    split = new String[]{pageRange, pageRange};
                }
                if (split.length == 2 && this.validateIndex(split[0], ocrData.getParagraphs().size()) &&
                        this.validateIndex(split[1], ocrData.getParagraphs().size())) {
                    polygons.addAll(ocrData.getParagraphs().stream().skip(Long.parseLong(split[0]))
                            .limit(Long.parseLong(split[1]) + 1 - Long.parseLong(split[0]))
                            .map(Paragraph::getBoundingRegions).map(json -> JsonUtils.toObjValue(json, Polygon.class)).toList());
                }
            }
        }
        //提取结果中返回json里的content对象有可能是NullNode,加一个判空
        String content = "";
        if (jsonNode != null && jsonNode.get("content") != null && !jsonNode.get("content").isNull()) {
            content = jsonNode.get("content").asText();
        }
        return new ExtractTaskResult(null, null, null, null,
                type, compositeName, compositeIndex, name, content, polygons, paraRange);
    }

    protected Boolean validateIndex(String indexStr, int capacity) {
        if (capacity < 1 || CharSequenceUtil.isEmpty(indexStr) || !StringUtils.isNumericSpace(indexStr)) {
            return false;
        }
        int index = Integer.parseInt(indexStr);
        return index >= 0 && index <= capacity - 1;
    }

    protected String toExtractSchema(boolean withSchema) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode parentNode = mapper.createObjectNode();
        ObjectNode childNode = mapper.createObjectNode();
        ObjectNode propertyNode = mapper.createObjectNode();
        ArrayNode requireNode = mapper.createArrayNode();

        parentNode.put("name", "extract_content");
        parentNode.set("schema", childNode);
        childNode.put("type", "object");
        childNode.set("properties", propertyNode);
//        childNode.set("required", requireNode);

        for (ParameterConfig parameterConfig : parameterConfigs) {
            if (parameterConfig.getType() == ParameterTypeEnum.SINGLE) {
                handleSingle(parameterConfig, propertyNode, requireNode, mapper);
            } else {
                handleComposite(parameterConfig, propertyNode, requireNode, mapper);
            }
        }
        if (withSchema) {
            return parentNode.toString();
        }
        return childNode.toString();
    }

    protected void handleSingle(ParameterConfig config, ObjectNode propertyNode, ArrayNode requireNode, ObjectMapper mapper) {
        PrimitiveConfig primitiveConfig = config.getPrimitiveConfigs().get(0);

        if (primitiveConfig.isGenerate()) {
            return;
        }

        String propertyName = primitiveConfig.getName();
        ObjectNode contentSchema = createContentSchema(primitiveConfig, mapper);
        ObjectNode indexSchema = mapper.createObjectNode();
        indexSchema.put("type", "string");
        indexSchema.put("description", "format: 1-1,2-5");

        ObjectNode paramSchema = mapper.createObjectNode();
        ObjectNode properties = mapper.createObjectNode();
        properties.set("content", contentSchema);
        properties.set("para_range", indexSchema);
        paramSchema.put("type", "object");
        paramSchema.set("properties", properties);

        // 如果字段是必需的，则将其添加到required数组中
//        ArrayNode required = mapper.createArrayNode();
//        if (primitiveConfig.isRequired()) {
//            required.add("content");
//            required.add("para_range");
//            requireNode.add(propertyName);
//        }
//        paramSchema.set("required", required);

        // 将构建好的 paramSchema 添加到 node 中
        propertyNode.set(propertyName, paramSchema);
    }

    protected ObjectNode createContentSchema(PrimitiveConfig config, ObjectMapper mapper) {
        ObjectNode contentSchema = mapper.createObjectNode();
        contentSchema.put("type", config.getType().toString().toLowerCase());
        contentSchema.put("description", config.getDescription());
        if (config.getOptions() != null && !config.getOptions().isEmpty()) {
            ArrayNode arrayNode = mapper.createArrayNode();
            for (Object option : config.getOptions()) {
                if (option instanceof String opt) {
                    arrayNode.add(opt);
                } else if (option instanceof Integer opt) {
                    arrayNode.add(opt);
                } else if (option instanceof Double opt) {
                    arrayNode.add(opt);
                }
                contentSchema.set("enum", arrayNode);
            }
        }
        return contentSchema;
    }

    protected void handleComposite(ParameterConfig configs, ObjectNode propertyNode, ArrayNode requireNode, ObjectMapper mapper) {
        String arrayName = configs.getName();
        List<PrimitiveConfig> types = configs.getPrimitiveConfigs().stream().filter(config -> !config.isGenerate()).toList();

        if (CollectionUtil.isEmpty(types)) {
            return;
        }

        ObjectNode arraySchema = mapper.createObjectNode();
        arraySchema.put("type", "array");

        ObjectNode itemsSchema = mapper.createObjectNode();
        itemsSchema.put("type", "object");

        ObjectNode itemsProperties = mapper.createObjectNode();
//        ArrayNode itemsRequiredArray = mapper.createArrayNode();

        for (PrimitiveConfig prim : types) {
            ObjectNode fieldObject = mapper.createObjectNode();
            fieldObject.put("type", "object");

            ObjectNode fieldProperties = mapper.createObjectNode();
            ObjectNode contentSchema = createContentSchema(prim, mapper);
            ObjectNode indexSchema = mapper.createObjectNode();
            indexSchema.put("type", "string");
            indexSchema.put("description", "format: 1-1,2-5");

            fieldProperties.set("content", contentSchema);
            fieldProperties.set("para_range", indexSchema);
            fieldObject.set("properties", fieldProperties);

//            ArrayNode required = mapper.createArrayNode();

//            if (prim.isRequired()) {
//                required.add("content");
//                required.add("para_range");
//                itemsRequiredArray.add(prim.getName());
//            }
//            fieldObject.set("required", required);


            itemsProperties.set(prim.getName(), fieldObject);
        }

        itemsSchema.set("properties", itemsProperties);
//        itemsSchema.set("required", itemsRequiredArray);

        arraySchema.set("items", itemsSchema);
        propertyNode.set(arrayName, arraySchema);

        requireNode.add(arrayName);
    }

    /**
     * 提取结果后置处理
     *
     * @param results
     * @return
     */
    protected List<ExtractTaskResult> specialProcess(List<ExtractTaskResult> results) {
        return results;
    }
}
