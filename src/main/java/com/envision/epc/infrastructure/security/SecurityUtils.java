package com.envision.epc.infrastructure.security;

import com.envision.epc.infrastructure.security.handler.CustomAuthenticationToken;
import com.envision.epc.module.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.Optional;

/**
 * @author jingjing.dong
 * @since 2023/5/27-18:38
 */
@Slf4j
public class SecurityUtils {

    private SecurityUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static final String CSRF_SESSION_NAME = "X-CSRF-TOKEN";

    public static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

    public static boolean isNotAuthenticated(Authentication authentication) {
        if (Objects.isNull(authentication)) {
            return true;
        }
        return authentication instanceof AnonymousAuthenticationToken;
    }

    public static boolean isAuthenticated(Authentication authentication) {
        if (Objects.isNull(authentication)) {
            return false;
        }
        return !(authentication instanceof AnonymousAuthenticationToken);
    }

    public static boolean isNotAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.isNull(authentication)) {
            return true;
        }
        return authentication instanceof AnonymousAuthenticationToken;
    }

    public static boolean isAuthenticated() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (Objects.isNull(authentication)) {
//            return false;
//        }
//        return !(authentication instanceof AnonymousAuthenticationToken);
        return true;
    }

    public static User getCurrentUser() {
//        final Optional<User> currentUserOpt = getCurrentUserOpt();
        User user = new User();//todo
        user.setUsername("wenjun.gu");
        user.setAccount("wenjun.gu");
        user.setUserCode("20002541");
        return user;
//        if (currentUserOpt.isPresent()){
//            return currentUserOpt.get();
//        }
//        throw new BizException(ErrorCode.AUTH_UNAUTHENTICATED);
    }

    public static Optional<User> getCurrentUserOpt() {
//        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null) {
//            log.warn("当前登录状态过期");
//            return Optional.empty();
//        }
//        if (authentication.getPrincipal() instanceof User) {
//            return Optional.of((User) authentication.getPrincipal());
//        }
        return Optional.ofNullable(getCurrentUser());
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
