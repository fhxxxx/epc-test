package com.envision.bunny.demo.support.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.envision.bunny.infrastructure.crypto.jackson.FieldDecrypt;
import com.envision.bunny.infrastructure.crypto.jackson.FieldEncrypt;
import com.envision.bunny.infrastructure.crypto.param.DecryptAnnotation;
import com.envision.bunny.infrastructure.mask.DataMask;
import com.envision.bunny.infrastructure.mask.MaskEnum;
import com.envision.bunny.infrastructure.mybatis.AuditingEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;



/**
 * @author jingjing.dong
 * @since 2021/4/26-15:28
 */
@Setter
@Getter
@TableName("customer")
@ToString
public class Customer extends AuditingEntity {
    /**
     * ID
     */
    @ExcelProperty(value = "ID",index = 0)
    @NumberFormat("#")
    @TableId(value = "customer_id", type = IdType.ASSIGN_ID)
    protected Long customerId;
    /**
     * 名
     */
    @DataMask(function = MaskEnum.USERNAME)
    private String firstName;

    /**
     * 姓
     */
    @FieldEncrypt
    @FieldDecrypt
    @DecryptAnnotation
    private String lastName;

    /**
     * 分数
     */
    @FieldDecrypt
    @FieldEncrypt
    @Max(value = 6, message = "顾客分数{customer.score.max}")
    @DecryptAnnotation
    private Integer score;

    /**
     * 版本
     */
    @Version
    @JsonIgnore
    private Integer version;
}
