package com.envision.bunny.infrastructure.security.aad;

import com.envision.bunny.infrastructure.security.handler.CustomAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;

/**
 * 主要是获取参数进行组装，然后交由ProviderManager进行真正的校验
 *
 * @author jingjing.dong
 * @since 2021/3/31-18:53
 */
@Slf4j
public class AadAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    public AadAuthenticationFilter() {
        super((HttpServletRequest request) ->
                HttpMethod.GET.matches(request.getMethod())
                        && PATH_MATCHER.match(Constants.REDIRECT_PATH, request.getRequestURI()));
    }


    @Override
    public Authentication attemptAuthentication(HttpServletRequest httpServletRequest,
                                                HttpServletResponse httpServletResponse) throws AuthenticationException {
        String code = httpServletRequest.getParameter(Constants.CODE_PARAM_NAME);
        String state = httpServletRequest.getParameter(Constants.STATE_PARAM_NAME);
        //从这里直接获取到请求的requestUrl，在配置token req的请求体时需要使用到
        CustomAuthenticationToken authRequest = new CustomAuthenticationToken(code, state);
        this.setDetails(httpServletRequest, authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    /**
     * remote IP 等信息，完全参考了SpringSecurity的实现
     */
    protected void setDetails(HttpServletRequest request, CustomAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }

}
