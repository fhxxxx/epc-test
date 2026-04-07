package com.envision.epc.module.extract.domain.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/26-11:50
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Paragraph {
    private Integer index;
    private List<Span> spans;
    private JsonNode boundingRegions;
    private String role;
    private Integer page;
}
