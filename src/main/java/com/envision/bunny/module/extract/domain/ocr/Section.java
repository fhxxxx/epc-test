package com.envision.bunny.module.extract.domain.ocr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/10/17-9:51
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
public class Section {
    private int index;
    List<Span> spans;
    private List<String> elements;
}
