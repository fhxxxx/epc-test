package com.envision.bunny.module.extract.domain.ocr;

import lombok.*;

/**
 * @author wenjun.gu
 * @since 2025/8/26-11:50
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Word {
    private Integer index;
    private String content;
    private Span span;
    private Polygon polygon;
    private Integer page;
//    private Double width;
//    private Double height;
}
