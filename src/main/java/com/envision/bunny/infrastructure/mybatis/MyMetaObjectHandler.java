package com.envision.bunny.infrastructure.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.envision.bunny.infrastructure.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.security.core.context.SecurityContextHolder.getContext;

/**
 * @author jingjing.dong
 * @since 2020/9/15-18:48
 */
@Slf4j(topic = "mybatis-auto-fill")
@Component // 一定不要忘记把处理器加到IOC容器中！
public class MyMetaObjectHandler implements MetaObjectHandler {
    // 插入时的填充策略
    @Override
    public void insertFill(MetaObject metaObject) {
        String currentUserCode = getCurrentUserCode();
        String currentUserName = getCurrentUserName();
        this.setFieldValByName("createTime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("uuid", UUID.randomUUID().toString(), metaObject);
        this.setFieldValByName("createBy", currentUserCode, metaObject);
        this.setFieldValByName("createByName", currentUserName, metaObject);
        this.setFieldValByName("updateBy", currentUserCode, metaObject);
        this.setFieldValByName("updateByName", currentUserName, metaObject);
        this.setFieldValByName("deleted", false, metaObject);
    }

    // 更新时的填充策略
    @Override
    public void updateFill(MetaObject metaObject) {
        String currentUserCode = getCurrentUserCode();
        String currentUserName = getCurrentUserName();
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("updateBy", currentUserCode, metaObject);
        this.setFieldValByName("updateByName", currentUserName, metaObject);
    }

    private String getCurrentUserCode() {
        String currentUserCode;
        Authentication authentication = getContext().getAuthentication();
        if (SecurityUtils.isAuthenticated()) {
            currentUserCode = SecurityUtils.getCurrentUserCode();
        } else if (Objects.isNull(authentication)) {
            currentUserCode = "ghost";
        } else {
            currentUserCode = authentication.getName();
        }
        return currentUserCode;
    }

    private String getCurrentUserName() {
        String currentUserName;
        Authentication authentication = getContext().getAuthentication();
        if (SecurityUtils.isAuthenticated()) {
            currentUserName = SecurityUtils.getCurrentUsername();
        } else if (Objects.isNull(authentication)) {
            currentUserName = "ghost";
        } else {
            currentUserName = authentication.getName();
        }
        return currentUserName;
    }
}