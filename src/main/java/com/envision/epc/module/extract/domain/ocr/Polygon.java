package com.envision.epc.module.extract.domain.ocr;

import lombok.*;

/**
 * @author wenjun.gu
 * @since 2025/8/26-11:53
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Polygon {
    private Double x1;
    private Double y1;
    private Double x2;
    private Double y2;
    private Double x3;
    private Double y3;
    private Double x4;
    private Double y4;
}
