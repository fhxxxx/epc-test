package com.envision.bunny.demo.integration.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jingjing.dong
 * @since 2021/5/6-11:44
 */
@Setter
@Getter
public class Account {
    @JsonIgnore
    @ExcelProperty(index = 0)
    String id;
    @ExcelProperty(index = 1)
    String name;
}
