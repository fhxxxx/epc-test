package com.envision.bunny.demo.integration.es.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jingjing.dong
 * @since 2024/1/14-9:52
 */
@Getter
@Setter
@TableName("faq_list")
public class Faq {
    /**
     * ID
     *
     * @mock 143545
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * BOT ID
     *
     * @mock 143545
     */
    @TableField(value = "bot_id")
    private Long botId;

    /**
     * 问题
     */
    private String question;

    /**
     * 答案
     */
    private String answer;
}
