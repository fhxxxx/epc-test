package com.envision.bunny.infrastructure.log;

import lombok.Builder;
import lombok.Data;

/**
 * @author jingjing.dong
 * @since 2021/3/17-18:50
 */
@Data
@Builder
class SysLog {
    /**
     * 请求URL
     */
    String url;
    /**
     * 请求参数
     */
    String args;
    /**
     * 请求类型
     */
    String httpMethod;
    /**
     * 耗时(毫秒)
     */
    long time;
    /**
     * 方法名
     */
    String methodName;
    @Override
    public String toString(){
        return String.format("请求URL[%s],类型[%s],方法名[%s],参数%s",url,httpMethod,methodName,args);
    }
}
