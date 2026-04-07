package com.envision.epc.infrastructure.security;

import cn.hutool.core.collection.ListUtil;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.infrastructure.security.handler.CustomAuthenticationToken;
import com.envision.epc.module.user.domain.User;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.Optional;

/**
 * @author jingjing.dong
 * @since 2023/5/27-18:38
 */
@Slf4j
@UtilityClass
public class SecurityUtils {
    public static final AnonymousAuthenticationToken BACKGROUND_AUTHENTICATION_TOKEN = new AnonymousAuthenticationToken("BACKGROUND",
            "BACKGROUND_TASK", ListUtil.of(new SimpleGrantedAuthority("ROLE_anonymous")));
    public static final String CSRF_SESSION_NAME = "X-CSRF-TOKEN";
    public static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

    public static boolean isNotAuthenticated(Authentication authentication) {
        return !isAuthenticated(authentication);
    }

    public static boolean isAuthenticated(Authentication authentication) {
        if (Objects.isNull(authentication)) {
            return false;
        }
        return (authentication instanceof CustomAuthenticationToken);
    }

    public static boolean isNotAuthenticated() {
        return !isAuthenticated();
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.isNull(authentication)) {
            return false;
        }
        return (authentication instanceof CustomAuthenticationToken);
    }

    public static User getCurrentUser() {
        final Optional<User> currentUserOpt = getCurrentUserOpt();
        if (currentUserOpt.isPresent()){
            return currentUserOpt.get();
        }
        throw new BizException(ErrorCode.AUTH_UNAUTHENTICATED);
    }

    public static Optional<User> getCurrentUserOpt() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.warn("当前登录状态过期");
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof User) {
            return Optional.of((User) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    public static String getCurrentUsername() {
        final User currentUser = getCurrentUser();
        return currentUser.getUsername();
    }

    public static String getCurrentUserCode() {
        final User currentUser = getCurrentUser();
        return currentUser.getUserCode();
    }

    public static long getCurrentUserId() {
        final User currentUser = getCurrentUser();
        return currentUser.getId();
    }


    public static void setChangedUser(User changedUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        final CustomAuthenticationToken authentication = (CustomAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        authentication.setUser(changedUser);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

}
