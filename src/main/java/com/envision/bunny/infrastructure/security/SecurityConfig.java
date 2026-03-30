package com.envision.bunny.infrastructure.security;

import com.envision.bunny.infrastructure.security.aad.AadAuthenticationFilter;
import com.envision.bunny.infrastructure.security.handler.*;
import com.envision.bunny.infrastructure.security.okta.OktaAuthenticationFilter;
import com.envision.bunny.infrastructure.security.wecom.WecomAuthenticationFilter;
import com.envision.bunny.infrastructure.util.ApplicationContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.web.cors.CorsUtils;


import java.io.IOException;

import static com.envision.bunny.infrastructure.security.SecurityUtils.CSRF_SESSION_NAME;

/**
 * SpringSecurity的配置类，因为加上了验证码登陆，所以要在配置的地方进行配置；
 * 加上自己客制化的各种Handler，因为普遍的采用前后端分离，所以全部改为返回统一的数据结构；由前端做跳转；
 * 默认加上Okta相关的登陆，如果不需要Okta集成，去掉相关的类即可；
 *
 * @author jingjing.dong
 * @since 2021/4/8-14:31
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /**
     * 拒绝请求处理,默认的处理方法会打印很多error异常，发很多的告警，这里改为只返回400
     */
    @Bean
    public RequestRejectedHandler requestRejectedHandler() {
        return new RequestRejectedHandler() {
            @Override
            public void handle(HttpServletRequest request,
                               HttpServletResponse response,
                               RequestRejectedException ex) throws IOException {
                // 不记录 ERROR，只返回 400
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        };
    }

    @Bean
    @Order(0)
    SecurityFilterChain resources(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/**", "/static/**", "/download/**", "/telnet/check")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .requestCache(RequestCacheConfigurer::disable)
//                .securityContext(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(1)
    @Profile({"dev", "uat", "qa", "prod", "local"})
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        final HttpSessionCsrfTokenRepository cusCsrfTokenRepository = new HttpSessionCsrfTokenRepository();
        cusCsrfTokenRepository.setSessionAttributeName(CSRF_SESSION_NAME);
        http.headers(headers -> headers
                        // CSP（真正的 XSS 防护）
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self'; " +
                                                "object-src 'none'; " +
                                                "base-uri 'self'"
                                )
                        )
                        // 防止 MIME sniffing
                        .contentTypeOptions(Customizer.withDefaults())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login/**").permitAll()
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        /*path: /ip/whitelist 限制仅白名单访问 whitelistIp 配置在properties中
                        // .requestMatchers("/ip/whitelist").hasIpAddress(whitelistIp) // IP白名单*/
                         .anyRequest().authenticated() //替换↓的写法后，仅上面放开的URL可以未登陆进行访问，其他会被拦截至首页
                        // .anyRequest().permitAll() //为了测试先放开所有的请求，生产中替换为↑一行的写法
                ).cors(Customizer.withDefaults())//跨域配置  ALB网关已经做了跨域配置 因此此处可以去掉
                .formLogin(form -> form
                        .loginProcessingUrl("/login/local")
                        //.defaultSuccessUrl("your default login url") 定义默认的登陆界面 与 SuccessHandler冲突
                        .successHandler(new CustomAuthenticationSuccessHandler())
                        .failureHandler(new CustomAuthenticationFailureHandler()))
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.csrfTokenRepository(cusCsrfTokenRepository))
//                .csrf(AbstractHttpConfigurer::disable) //不做csrf_token校验
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(new CustomAccessDeniedHandler())
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint()))
                .logout(logout -> logout
                        .permitAll()
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        //.logoutUrl("/login/logout") //定义默认的注销界面
                        .logoutSuccessHandler(new CustomLogoutHandler()))
                .addFilterAfter(getOktaAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(getWecomAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .securityContext(securityContext ->
                        securityContext.requireExplicitSave(false)
                );
        return http.build();
    }

    @Bean
    @Order(2)
    @Profile("anonymous")
    SecurityFilterChain filterChainLocal(HttpSecurity http) throws Exception {
        final HttpSessionCsrfTokenRepository cusCsrfTokenRepository = new HttpSessionCsrfTokenRepository();
        cusCsrfTokenRepository.setSessionAttributeName(CSRF_SESSION_NAME);
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login/**").permitAll()
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        /*path: /ip/whitelist 限制仅白名单访问 whitelistIp 配置在properties中
                        // .requestMatchers("/ip/whitelist").hasIpAddress(whitelistIp) // IP白名单*/
                        // .anyRequest().permitAll() /替换↓的写法后，仅上面放开的URL可以未登陆进行访问，其他会被拦截至首页
                        .anyRequest().permitAll() //为了测试先放开所有的请求，生产中替换为↑一行的写法
                ).cors(Customizer.withDefaults())//跨域配置  ALB网关已经做了跨域配置 因此此处可以去掉
                .formLogin(form -> form
                        .loginProcessingUrl("/login/local")
                        //.defaultSuccessUrl("your default login url") 定义默认的登陆界面 与 SuccessHandler冲突
                        .successHandler(new CustomAuthenticationSuccessHandler())
                        .failureHandler(new CustomAuthenticationFailureHandler()))
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .permitAll()
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        //.logoutUrl("/login/logout") //定义默认的注销界面
                        .logoutSuccessHandler(new CustomLogoutHandler()))
                .addFilterAfter(getAadAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(getOktaAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(getWecomAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private AadAuthenticationFilter getAadAuthenticationFilter() {
        final AuthenticationProvider aadAuthProvider = (AuthenticationProvider) ApplicationContextUtils.getBean("aadAuthProvider");
        AadAuthenticationFilter aadAuthenticationFilter = new AadAuthenticationFilter();
        aadAuthenticationFilter.setAuthenticationManager(new ProviderManager(aadAuthProvider));
        aadAuthenticationFilter.setAuthenticationSuccessHandler(new CustomAuthenticationSuccessHandler());
        return aadAuthenticationFilter;
    }

    private OktaAuthenticationFilter getOktaAuthenticationFilter() {
        final AuthenticationProvider oktaAuthProvider = (AuthenticationProvider) ApplicationContextUtils.getBean("oktaAuthProvider");
        OktaAuthenticationFilter oktaAuthenticationFilter = new OktaAuthenticationFilter();
        oktaAuthenticationFilter.setAuthenticationManager(new ProviderManager(oktaAuthProvider));
        oktaAuthenticationFilter.setAuthenticationSuccessHandler(new CustomAuthenticationSuccessHandler());
        return oktaAuthenticationFilter;
    }

    private WecomAuthenticationFilter getWecomAuthenticationFilter() {
        final AuthenticationProvider wecomAuthProvider = (AuthenticationProvider) ApplicationContextUtils.getBean("wecomAuthProvider");
        WecomAuthenticationFilter wecomAuthenticationFilter = new WecomAuthenticationFilter();
        wecomAuthenticationFilter.setAuthenticationManager(new ProviderManager(wecomAuthProvider));
        wecomAuthenticationFilter.setAuthenticationSuccessHandler(new CustomAuthenticationSuccessHandler());
        return wecomAuthenticationFilter;
    }


}
