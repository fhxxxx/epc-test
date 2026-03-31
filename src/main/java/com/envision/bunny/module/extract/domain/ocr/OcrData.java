package com.envision.bunny.module.extract.domain.ocr;

import lombok.*;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/27-12:19
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OcrData {
    private List<Word> words;
    private List<Line> lines;
    private List<Paragraph> paragraphs;
    private List<Section> sections;
    private String paraTaggedContent;
    private String wordTaggedContent;
}
