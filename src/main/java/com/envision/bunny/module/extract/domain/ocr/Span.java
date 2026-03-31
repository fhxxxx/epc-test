package com.envision.bunny.module.extract.domain.ocr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author wenjun.gu
 * @since 2025/10/17-13:31
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
public class Span {
    private int start;
    private int end;
}
