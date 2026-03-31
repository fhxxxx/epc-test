package com.envision.bunny.module.user.web;

import com.envision.extract.infrastructure.mybatis.BasicPagination;
import com.envision.extract.infrastructure.util.HttpSessionUtils;
import com.envision.extract.module.user.application.UserCommandService;
import com.envision.extract.module.user.application.UserKeywordQuery;
import com.envision.extract.module.user.application.UserQueryService;
import com.envision.extract.module.user.application.dto.UserDto;
import com.envision.extract.module.user.domain.User;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.envision.extract.infrastructure.security.SecurityUtils.CSRF_HEADER_NAME;
import static com.envision.extract.infrastructure.security.SecurityUtils.CSRF_SESSION_NAME;


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
    @GetMapping(value = "/current", produces = "application/json")
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

    /**
     * 分页查询在职员工列表
     * @param keywordQuery 查询参数
     * @return 员工列表
     */
    @GetMapping("/list")
    public BasicPagination<UserDto> queryUser(UserKeywordQuery keywordQuery){
        return userQueryService.queryUser(keywordQuery);
    }
}
