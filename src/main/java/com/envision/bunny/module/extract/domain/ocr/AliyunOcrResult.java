package com.envision.bunny.module.extract.domain.ocr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 阿里云提取结果
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AliyunOcrResult {


    /**
     * key : invoiceAmountPreTax
     * keyProb : 99
     * value : 3182432.68
     * valuePos : [{"x":2441,"y":3087},{"x":2793,"y":3086},{"x":2793,"y":3156},{"x":2442,"y":3158}]
     * valueProb : 99
     */

    private String key;
    private Integer keyProb;
    private String value;
    private Integer valueProb;
    private List<ValuePosBean> valuePos;

    @Data
    public static class ValuePosBean {
        /**
         * x : 2441
         * y : 3087
         */

        private Double x;
        private Double y;
    }
}
