package com.envision.epc.infrastructure.mybatis.plugin;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.envision.epc.infrastructure.security.SecurityUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 可以通过Table来限定哪些表需要数据权限
 *
 * @author jingjing.dong
 * @since 2024/4/17-22:10
 */
@Slf4j
public class MyDataPermHandler implements MultiDataPermissionHandler {
    private final List<String> deptIncludes;
    private final List<String> menuIncludes;

    public MyDataPermHandler(List<String> deptIncludes, List<String> menuIncludes) {
        this.deptIncludes = deptIncludes;
        this.menuIncludes = menuIncludes;
    }

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        boolean containsDept = !deptIncludes.isEmpty() && deptIncludes.contains(table.getName());
        boolean containsMenu = !menuIncludes.isEmpty() && menuIncludes.contains(table.getName());
        if (containsDept && containsMenu) {
            return new AndExpression(getDeptSqlSegment(), getMenuSqlSegment());
        } else if (containsDept) {
            return getDeptSqlSegment();
        } else if (containsMenu) {
            return getMenuSqlSegment();
        }
        return null;
    }

    /**
     * 拼接需要在业务 SQL 中额外追加的dept数据权限 SQL
     * 可以使用注解中的alias 也可以 不使用，示例是使用
     *
     * @return dept数据权限 SQL
     */
    private Expression getDeptSqlSegment() {
        if (authentication()) {
            return null;
        }
        // 模拟从Security Current User得到部门列表
        List<String> deptUserList = Lists.newArrayList("1", "2");
        ExpressionList<StringValue> expressionList = new ExpressionList<>(
                deptUserList.stream()
                        .map(StringValue::new)
                        .collect(Collectors.toList()));
        return new InExpression(new Column("dept_id"), expressionList);
    }

    /**
     * 拼接需要在业务 SQL 中额外追加的menu数据权限 SQL
     * 可以使用注解中的alias 也可以 不使用，示例是使用
     *
     * @return menu数据权限 SQL
     */
    private Expression getMenuSqlSegment() {
        if (authentication()) {
            return null;
        }
        // 模拟从Security Current User得到部门列表
        List<String> menuUserList = Lists.newArrayList("3", "4");
        ExpressionList<StringValue> expressionList = new ExpressionList<>(
                menuUserList.stream()
                        .map(StringValue::new)
                        .collect(Collectors.toList()));
        return new InExpression(new Column("menu_id"), expressionList);
    }

    private boolean authentication() {
        final String username = SecurityUtils.getCurrentUsername();
        return CharSequenceUtil.equalsIgnoreCase(username, "admin");
    }
}
