package com.envision.bunny.infrastructure.mybatis.plugin;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.envision.bunny.infrastructure.security.SecurityUtils;
import com.envision.bunny.module.user.domain.User;

import java.util.Optional;

/**
 * @author jingjing.dong
 * @since 2024/4/18-16:48
 */
public class MyDynamicTableNameHandler implements TableNameHandler {
    @Override
    public String dynamicTableName(String sql, String tableName) {
        final Optional<User> currentUserOpt = SecurityUtils.getCurrentUserOpt();
        if (!currentUserOpt.isPresent()) {
            return tableName;
        }
        String username = currentUserOpt.get().getUsername();
        final int i = RandomUtil.randomInt(3);
        if (CharSequenceUtil.equals(username, "59930") && CharSequenceUtil.equals("customer", tableName)) {
            return "customer1";
        }
        return tableName;
    }
}
