package com.envision.bunny.module.user.application;

import com.envision.extract.infrastructure.mybatis.BasicPaging;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/12-16:57
 */
@Setter
@Getter
@ToString
public class UserKeywordQuery extends BasicPaging {
    private String username;
}
