package com.envision.epc.module.extract.domain.ocr;

import lombok.Data;

/**
 * 阿里云ocr偏移量
 */
@Data
public class SliceRect {

    /**
     * x0 : 417
     * y0 : 356
     * x1 : 3639
     * y1 : 355
     * x2 : 3649
     * y2 : 2448
     * x3 : 419
     * y3 : 2448
     */

    private Double x0;
    private Double y0;
    private Double x1;
    private Double y1;
    private Double x2;
    private Double y2;
    private Double x3;
    private Double y3;

    public Double getWidthOffset() {
        return x0 == null ? 0D : x0;
    }

    public Double getHeightOffset() {
        return y0 == null ? 0D : y0;
    }

    public Double getInnerWidth() {
        double max = Math.max(Math.max(Math.max(x0, x1), x2), x3);
        double min = Math.min(Math.min(Math.min(x0, x1), x2), x3);
        return max - min;
    }

    public Double getInnerHeight() {
        double max = Math.max(Math.max(Math.max(y0, y1), y2), y3);
        double min = Math.min(Math.min(Math.min(y0, y1), y2), y3);
        return max - min;
    }
}
