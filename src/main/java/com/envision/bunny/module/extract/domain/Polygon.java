package com.envision.bunny.module.extract.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

/**
 * @author gangxiang.guan
 * @date 2025/10/31 10:01
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Polygon {


    /**
     * width : 8.2639
     * height : 11.6944
     * polygon : [1.0863,5.3167,1.0812,4.7892,1.3578,4.7865,1.3672,5.314]
     * pageNumber : 1
     */

    private Double width;
    private Double height;
    private Integer pageNumber;
    private List<Double> polygon;
}
