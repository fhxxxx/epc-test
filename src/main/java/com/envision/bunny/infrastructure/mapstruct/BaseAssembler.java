package com.envision.bunny.infrastructure.mapstruct;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author jingjing.dong
 * @since 2024/5/14-15:10
 */
public interface BaseAssembler {

    /**
     * 已经设置系统默认时区为北京时区
     * @param localDateTime 北京时间
     * @return Date
     */
    default Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

}
