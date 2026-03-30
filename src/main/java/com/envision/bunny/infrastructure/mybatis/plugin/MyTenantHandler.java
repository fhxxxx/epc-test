package com.envision.bunny.infrastructure.mybatis.plugin;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.envision.bunny.infrastructure.security.SecurityUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.List;

/**
 * @author jingjing.dong
 * @since 2024/4/16-21:45
 */
public class MyTenantHandler implements TenantLineHandler {
    private final List<String> includes;
    private final List<String> excludes;

    public MyTenantHandler(List<String> includes,List<String> excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }

    @Override
    public Expression getTenantId() {
        final String username = SecurityUtils.getCurrentUsername();
        if (CharSequenceUtil.equals("59930",username)) {
            return new LongValue(59930);
        }
        return new LongValue(1);
    }

    /**
     * 获取租户字段名,默认字段名叫: tenant_id
     *
     * @return 租户字段名
     */
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    /**
     * 根据表名判断是否忽略拼接多租户条件
     * <p>
     * 默认都要进行解析并拼接多租户条件
     * 优先判断includes,如果为空，再根据excludes判断
     * @param tableName 表名
     * @return 是否忽略, true:表示忽略，false:需要解析并拼接多租户条件
     */
    @Override
    public boolean ignoreTable(String tableName) {
        if (includes.isEmpty()){
            return excludes.contains(tableName);
        } else {
            return !includes.contains(tableName);
        }
    }
}

