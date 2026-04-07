package com.envision.epc.module.extract.domain;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author wenjun.gu
 * @since 2025/9/1-15:26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_compare_run", autoResultMap = true)
public class CompareRun extends AuditingEntity {
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 类型
     */
    private CompareTypeEnum type;
    /**
     * 抽取状态
     */
    private CompareRunStatusEnum status;
    /**
     * 错误信息
     */
    private String error;

    public void success() {
        this.status = CompareRunStatusEnum.COMPARE_SUCCESS;
    }

    public void failed(String error) {
        this.status = CompareRunStatusEnum.COMPARE_FAILED;
        if (CharSequenceUtil.isNotBlank(error) && error.length() > 2000) {
            error = error.substring(0, 2000);
        }
        this.error = error;
    }


}
