package com.envision.bunny.module.user.web;

import com.envision.bunny.infrastructure.util.HttpSessionUtils;
import com.envision.bunny.module.user.application.UserCommandService;
import com.envision.bunny.module.user.application.UserQueryService;
import com.envision.bunny.module.user.domain.User;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

import static com.envision.bunny.infrastructure.security.SecurityUtils.CSRF_HEADER_NAME;
import static com.envision.bunny.infrastructure.security.SecurityUtils.CSRF_SESSION_NAME;


/**
 * 系统用户
 *
 * @author yakun.meng
 * @since 2024-05-09
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserCommandService userCommandService;
    @Resource
    private UserQueryService userQueryService;

    /**
     * 切换语言
     * @param locale zh_CN,en_US
     */
    @PutMapping("/locale")
    public void switchLocale(@RequestParam("locale") String locale) {
        userCommandService.switchLocale(locale);
    }


    /**
     * 当前登录用户
     * @return 用户信息
     */
    @GetMapping("/current")
    public User getCurrentUserInfo(HttpServletResponse response) {
        final CsrfToken token = HttpSessionUtils.get(CSRF_SESSION_NAME);
        if (token != null) {
            response.addHeader(CSRF_HEADER_NAME, token.getToken());
        }
        return userQueryService.getCurrentUserInfo();
    }

    /**
     * 查询所有群组列表
     * @param fuzzySearch 搜索字符串
     * @return 群组明细列表
     */
    @GetMapping
    public List<User> list(@RequestParam String fuzzySearch) {
        return userQueryService.list(fuzzySearch);
    }

    /**
     * 人员同步
     */
    @GetMapping("/sync")
    public void syncFromDataLake(HttpServletResponse response) {
        userCommandService.syncWithDataLake();
    }

    /**
     * 人员同步
     */
    @GetMapping("/sync-all")
    public void syncAllFromDataLake(HttpServletResponse response) {
        userCommandService.syncAllUser();
    }
}
