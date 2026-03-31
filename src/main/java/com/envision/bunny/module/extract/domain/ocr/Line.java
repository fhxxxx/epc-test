package com.envision.bunny.module.extract.domain.ocr;

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
public class Line {
    private Integer index;
    private List<Span> spans;
    private Integer page;
}
