package com.envision.bunny.infrastructure.response;

import com.envision.bunny.infrastructure.util.JsonUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * 统一返回Response为Echo类型
 * @author jingjing.dong
 * @since 2021/3/21-16:44
 */
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> aClass) {
        return !(returnType.getGenericParameterType().equals(Echo.class) || returnType.getGenericParameterType().equals(StreamingResponseBody.class)
    || returnType.getGenericParameterType().equals(Resource.class));
    }
    /**
     * 统一返回类型为Echo
     * @return 包装后的结果
     */
    @Override
    public Object beforeBodyWrite(Object data, MethodParameter returnType, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        // String类型不能直接包装，所以要进行些特别的处理
        if (returnType.getGenericParameterType().equals(String.class)) {
            // 将数据包装在BaseResult里后，再转换为json字符串响应给前端
            return JsonUtils.toJsonStr(Echo.success(data));
        } else {
            // 将原本的数据包装在ResultVO里
            return Echo.success(data);
        }
    }
}
