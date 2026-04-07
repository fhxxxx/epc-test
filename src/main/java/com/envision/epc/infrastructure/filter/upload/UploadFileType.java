package com.envision.epc.infrastructure.filter.upload;

import java.lang.annotation.*;

/**
 * @author jingjing.dong
 * @since 2021/9/26-10:03
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UploadFileType {
    //注解自定义上传的FileType
    String[] fileType() default "";
}
