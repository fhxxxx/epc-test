package com.envision.epc.infrastructure.security.wecom;

import lombok.experimental.UtilityClass;

/**
 * @author jingjing.dong
 * @since 2021/4/8-14:45
 */
@UtilityClass
class Constants {
    static final String REDIRECT_PATH ="/login/wecom/auth";
    static final String CODE_PARAM_NAME ="code";
    static final String STATE_PARAM_NAME ="state";
}
