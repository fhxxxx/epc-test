package com.envision.epc.infrastructure.mybatis.plugin;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.parser.JsqlParserGlobal;
import com.envision.epc.infrastructure.mybatis.plugin.operation_result.DataChangeRecorderInnerInterceptor;
import com.envision.epc.infrastructure.mybatis.plugin.operation_result.OperationResultRepository;
import com.envision.epc.infrastructure.util.ApplicationContextUtils;
import lombok.SneakyThrows;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.core.task.TaskExecutor;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * @author jingjing.dong
 * @since 2024/4/20-15:35
 */
public class MyDataChangeRecorder extends DataChangeRecorderInnerInterceptor {
    private final List<String> includes;
    private final List<String> excludes;

    public MyDataChangeRecorder(List<String> includes, List<String> excludes) {
        this.includes = includes;
        this.excludes = excludes;
        final Properties properties = new Properties();
        properties.setProperty(IGNORED_TABLE_COLUMN_PROPERTIES, "*.create_time,update_time,create_by,update_by,create_by_name,update_by_name");
        super.setProperties(properties);
        super.setBatchUpdateLimit(1000);
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
        MappedStatement ms = mpSh.mappedStatement();
        final BoundSql boundSql = mpSh.boundSql();
        SqlCommandType sct = ms.getSqlCommandType();
        if (noNeedProcess(boundSql.getSql())) {
            return;
        }
        if (sct == SqlCommandType.INSERT || sct == SqlCommandType.UPDATE || sct == SqlCommandType.DELETE) {
            PluginUtils.MPBoundSql mpBs = mpSh.mPBoundSql();
            OperationResult operationResult;
            long startTs = System.currentTimeMillis();
            try {
                Statement statement = JsqlParserGlobal.parse(mpBs.sql());
                if (statement instanceof Insert) {
                    operationResult = processInsert((Insert) statement, mpSh.boundSql());
                } else if (statement instanceof Update) {
                    operationResult = processUpdate((Update) statement, ms, boundSql, connection);
                } else if (statement instanceof Delete) {
                    operationResult = processDelete((Delete) statement, ms, boundSql, connection);
                } else {
                    logger.info("other operation sql={}", mpBs.sql());
                    return;
                }
            } catch (Exception e) {
                if (e instanceof DataUpdateLimitationException) {
                    throw (DataUpdateLimitationException) e;
                }
                logger.error("Unexpected error for mappedStatement={}, sql={}", ms.getId(), mpBs.sql(), e);
                return;
            }
            long costThis = System.currentTimeMillis() - startTs;
            if (operationResult != null) {
                operationResult.setCost(costThis);
                dealOperationResult(operationResult);
            }
        }
    }

    /**
     * 判断哪些SQL不需要处理
     * 默认INSERT/UPDATE/DELETE语句
     * 自己适配性的更改为哪些表不需要处理，哪些表需要处理
     *
     * @param sql 传入的SQL语句
     * @return 是否需要处理
     */
    @SneakyThrows
    public boolean noNeedProcess(String sql) {
        Statement statement = JsqlParserGlobal.parse(sql);
        String tableName = null;
        if (statement instanceof Insert) {
            final Insert statement1 = (Insert) statement;
            tableName = statement1.getTable().getName();
        } else if (statement instanceof Update) {
            final Update statement1 = (Update) statement;
            tableName = statement1.getTable().getName();
        } else if (statement instanceof Delete) {
            final Delete statement1 = (Delete) statement;
            tableName = statement1.getTable().getName();
        } else {
            return true;
        }
        if (includes.isEmpty()) {
            return excludes.contains(tableName);
        } else {
            return !includes.contains(tableName);
        }
    }

    /**
     * 处理数据更新结果，默认打印
     *
     * @param operationResult 数据对比结果
     */
    @Override
    protected void dealOperationResult(OperationResult operationResult) {
        final String changedData = operationResult.getChangedData();
        if (CharSequenceUtil.isEmpty(changedData) || CharSequenceUtil.equals("[]", changedData)) {
            return;
        }
        final TaskExecutor taskExecutor = ApplicationContextUtils.getBean("taskExecutor", TaskExecutor.class);
        final CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
            final OperationResultRepository repository = ApplicationContextUtils.getBean(OperationResultRepository.class);
            repository.save(BeanUtil.copyProperties(operationResult, com.envision.epc.infrastructure.mybatis.plugin.operation_result.OperationResult.class));
        }, taskExecutor);
        completableFuture.exceptionally(e -> {
            logger.error(e.getMessage());
            return null;
        });
    }

}
