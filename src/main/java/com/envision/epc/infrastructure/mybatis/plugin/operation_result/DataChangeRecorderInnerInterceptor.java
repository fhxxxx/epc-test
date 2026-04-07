package com.envision.epc.infrastructure.mybatis.plugin.operation_result;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.parser.JsqlParserGlobal;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.Data;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.RowConstructor;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 数据变动记录插件
 * 默认会生成一条log，格式：
 * ----------------------INSERT LOG------------------------------
 * </p>
 * <p>
 * {
 * "tableName": "h2user",
 * "operation": "insert",
 * "recordStatus": "true",
 * "changedData": [
 * {
 * "LAST_UPDATED_DT": "null->2022-08-22 18:49:16.512",
 * "TEST_ID": "null->1561666810058739714",
 * "AGE": "null->THREE"
 * }
 * ],
 * "cost(ms)": 0
 * }
 * </p>
 * <p>
 * * ----------------------UPDATE LOG------------------------------
 * <p>
 * {
 * "tableName": "h2user",
 * "operation": "update",
 * "recordStatus": "true",
 * "changedData": [
 * {
 * "TEST_ID": "102",
 * "AGE": "2->THREE",
 * "FIRSTNAME": "DOU.HAO->{\"json\":\"abc\"}",
 * "LAST_UPDATED_DT": "null->2022-08-22 18:49:16.512"
 * }
 * ],
 * "cost(ms)": 0
 * }
 * </p>
 *
 * @author yuxiaobin
 * @date 2022-8-21
 */
public class DataChangeRecorderInnerInterceptor implements InnerInterceptor {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    @SuppressWarnings("unused")
    public static final String IGNORED_TABLE_COLUMN_PROPERTIES = "ignoredTableColumns";

    private final Map<String, Set<String>> ignoredTableColumns = new ConcurrentHashMap<>();
    //全部表的这些字段名，INSERT/UPDATE都忽略，delete暂时保留
    private final Set<String> ignoreAllColumns = new HashSet<>();
    //批量更新上限, 默认一次最多1000条
    private int BATCH_UPDATE_LIMIT = 1000;
    private boolean batchUpdateLimitationOpened = false;
    //表名->批量更新上限
    private final Map<String, Integer> BATCH_UPDATE_LIMIT_MAP = new ConcurrentHashMap<>();

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
        MappedStatement ms = mpSh.mappedStatement();
        final BoundSql boundSql = mpSh.boundSql();
        SqlCommandType sct = ms.getSqlCommandType();
        if (sct == SqlCommandType.INSERT || sct == SqlCommandType.UPDATE || sct == SqlCommandType.DELETE) {
            PluginUtils.MPBoundSql mpBs = mpSh.mPBoundSql();
            DataChangeRecorderInnerInterceptor.OperationResult operationResult;
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
                if (e instanceof DataChangeRecorderInnerInterceptor.DataUpdateLimitationException) {
                    throw (DataChangeRecorderInnerInterceptor.DataUpdateLimitationException) e;
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
     * 判断哪些SQL需要处理
     * 默认INSERT/UPDATE/DELETE语句
     *
     * @param sql
     * @return
     */
    protected boolean allowProcess(String sql) {
        String sqlTrim = sql.trim().toUpperCase();
        return sqlTrim.startsWith("INSERT") || sqlTrim.startsWith("UPDATE") || sqlTrim.startsWith("DELETE");
    }

    /**
     * 处理数据更新结果，默认打印
     *
     * @param operationResult
     */
    protected void dealOperationResult(DataChangeRecorderInnerInterceptor.OperationResult operationResult) {
        logger.info("{}", operationResult);
    }

    public DataChangeRecorderInnerInterceptor.OperationResult processInsert(Insert insertStmt, BoundSql boundSql) {
        DataChangeRecorderInnerInterceptor.OperationResult result = new DataChangeRecorderInnerInterceptor.OperationResult();
        result.setOperation("insert");
        result.setTableName(insertStmt.getTable().getName());
        result.setRecordStatus(true);
        Map<String, Object> updatedColumnDatas = getUpdatedColumnDatas(result.getTableName(), boundSql, insertStmt);
        result.buildDataStr(compareAndGetUpdatedColumnDatas(result.getTableName(), null, updatedColumnDatas));
        return result;
    }

    public DataChangeRecorderInnerInterceptor.OperationResult processUpdate(Update updateStmt, MappedStatement mappedStatement, BoundSql boundSql, Connection connection) {
        Expression where = updateStmt.getWhere();
        PlainSelect selectBody = new PlainSelect();
        Table table = updateStmt.getTable();
        final Set<String> ignoredColumns = ignoredTableColumns.get(table.getName().toUpperCase());
        if (ignoredColumns != null) {
            if (ignoredColumns.stream().anyMatch("*"::equals)) {
                DataChangeRecorderInnerInterceptor.OperationResult result = new DataChangeRecorderInnerInterceptor.OperationResult();
                result.setOperation("update");
                result.setTableName(table.getName() + ":*");
                result.setRecordStatus(false);
                return result;
            }
        }
        selectBody.setFromItem(table);
        List<Column> updateColumns = new ArrayList<>();
        for (UpdateSet updateSet : updateStmt.getUpdateSets()) {
            updateColumns.addAll(updateSet.getColumns());
        }
        DataChangeRecorderInnerInterceptor.Columns2SelectItemsResult buildColumns2SelectItems = buildColumns2SelectItems(table.getName(), updateColumns);
        selectBody.setSelectItems(buildColumns2SelectItems.getSelectItems());
        selectBody.setWhere(where);
        SelectItem<PlainSelect> plainSelectSelectItem = new SelectItem<>(selectBody);

        BoundSql boundSql4Select = new BoundSql(mappedStatement.getConfiguration(), plainSelectSelectItem.toString(),
                prepareParameterMapping4Select(boundSql.getParameterMappings(), updateStmt),
                boundSql.getParameterObject());
        PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
        Map<String, Object> additionalParameters = mpBoundSql.additionalParameters();
        if (additionalParameters != null && !additionalParameters.isEmpty()) {
            for (Map.Entry<String, Object> ety : additionalParameters.entrySet()) {
                boundSql4Select.setAdditionalParameter(ety.getKey(), ety.getValue());
            }
        }
        Map<String, Object> updatedColumnDatas = getUpdatedColumnDatas(table.getName(), boundSql, updateStmt);
        DataChangeRecorderInnerInterceptor.OriginalDataObj originalData = buildOriginalObjectData(updatedColumnDatas, selectBody, buildColumns2SelectItems.getPk(), mappedStatement, boundSql4Select, connection);
        DataChangeRecorderInnerInterceptor.OperationResult result = new DataChangeRecorderInnerInterceptor.OperationResult();
        result.setOperation("update");
        result.setTableName(table.getName());
        result.setRecordStatus(true);
        result.buildDataStr(compareAndGetUpdatedColumnDatas(result.getTableName(), originalData, updatedColumnDatas));
        return result;
    }

    private TableInfo getTableInfoByTableName(String tableName) {
        for (TableInfo tableInfo : TableInfoHelper.getTableInfos()) {
            if (tableName.equalsIgnoreCase(tableInfo.getTableName())) {
                return tableInfo;
            }
        }
        return null;
    }

    /**
     * 将update SET部分的jdbc参数去除
     *
     * @param originalMappingList 这里只会包含JdbcParameter参数
     * @param updateStmt
     * @return
     */
    private List<ParameterMapping> prepareParameterMapping4Select(List<ParameterMapping> originalMappingList, Update updateStmt) {
        List<Expression> updateValueExpressions = new ArrayList<>();
        for (UpdateSet updateSet : updateStmt.getUpdateSets()) {
            updateValueExpressions.addAll(updateSet.getValues());
        }
        int removeParamCount = 0;
        for (Expression expression : updateValueExpressions) {
            if (expression instanceof JdbcParameter) {
                ++removeParamCount;
            }
        }
        return originalMappingList.subList(removeParamCount, originalMappingList.size());
    }

    protected Map<String, Object> getUpdatedColumnDatas(String tableName, BoundSql updateSql, Statement statement) {
        Map<String, Object> columnNameValMap = new HashMap<>(updateSql.getParameterMappings().size());
        Map<Integer, String> columnSetIndexMap = new HashMap<>(updateSql.getParameterMappings().size());
        List<Column> selectItemsFromUpdateSql = new ArrayList<>();
        if (statement instanceof Update) {
            processUpdateStatement((Update) statement, updateSql, columnNameValMap, columnSetIndexMap, selectItemsFromUpdateSql, tableName);
        } else if (statement instanceof Insert) {
            Insert insert = (Insert) statement;
            selectItemsFromUpdateSql.addAll(insert.getColumns());
            columnNameValMap.putAll(detectInsertColumnValuesNonJdbcParameters(insert));
        }
        Map<String, String> relatedColumnsUpperCaseWithoutUnderline = buildRelatedColumnsMap(selectItemsFromUpdateSql);
        MetaObject metaObject = SystemMetaObject.forObject(updateSql.getParameterObject());
        int index = 0;
        for (ParameterMapping parameterMapping : updateSql.getParameterMappings()) {
            String propertyName = parameterMapping.getProperty();
            if (propertyName.startsWith("ew.paramNameValuePairs")) {
                ++index;
                continue;
            }
            String[] arr = propertyName.split("\\.");
            String propertyNameTrim = arr[arr.length - 1].replace("_", "").toUpperCase();
            index = getIndex(tableName, columnNameValMap, columnSetIndexMap, relatedColumnsUpperCaseWithoutUnderline, metaObject, index, parameterMapping, propertyName, propertyNameTrim);
        }
        dealWithUpdateWrapper(columnSetIndexMap, columnNameValMap, updateSql);
        return columnNameValMap;
    }

    private int getIndex(String tableName, Map<String, Object> columnNameValMap, Map<Integer, String> columnSetIndexMap, Map<String, String> relatedColumnsUpperCaseWithoutUnderline, MetaObject metaObject, int index, ParameterMapping parameterMapping, String propertyName, String propertyNameTrim) {
        try {
            final String columnName = columnSetIndexMap.getOrDefault(index++, getColumnNameByProperty(propertyNameTrim, tableName));
            if (relatedColumnsUpperCaseWithoutUnderline.containsKey(propertyNameTrim)) {
                extracted(columnNameValMap, relatedColumnsUpperCaseWithoutUnderline, metaObject, propertyName, propertyNameTrim, columnName);
            } else {
                if (columnName != null) {
                    columnNameValMap.put(columnName, String.valueOf(metaObject.getValue(propertyName)));
                }
            }
        } catch (Exception e) {
            logger.warn("get value error,propertyName:{},parameterMapping:{}", propertyName, parameterMapping);
        }
        return index;
    }

    private void extracted(Map<String, Object> columnNameValMap, Map<String, String> relatedColumnsUpperCaseWithoutUnderline, MetaObject metaObject, String propertyName, String propertyNameTrim, String columnName) {
        final String colkey = relatedColumnsUpperCaseWithoutUnderline.get(propertyNameTrim);
        Object valObj = metaObject.getValue(propertyName);
        if (valObj instanceof IEnum) {
            valObj = ((IEnum<?>) valObj).getValue();
        } else if (valObj instanceof Enum) {
            valObj = getEnumValue((Enum) valObj);
        }
        if (columnNameValMap.containsKey(colkey)) {
            columnNameValMap.put(relatedColumnsUpperCaseWithoutUnderline.get(propertyNameTrim), String.valueOf(columnNameValMap.get(colkey)).replace("?", valObj == null ? "" : valObj.toString()));
        }
        if (columnName != null && !columnNameValMap.containsKey(columnName)) {
            columnNameValMap.put(columnName, valObj);
        }
    }

    private void processUpdateStatement(Update updateStmt, BoundSql updateSql, Map<String, Object> columnNameValMap,
                                        Map<Integer, String> columnSetIndexMap, List<Column> selectItemsFromUpdateSql, String tableName) {
        int index = 0;
        for (UpdateSet updateSet : updateStmt.getUpdateSets()) {
            selectItemsFromUpdateSql.addAll(updateSet.getColumns());
            final ExpressionList<Expression> updateList = (ExpressionList<Expression>) updateSet.getValues();
            for (int i = 0; i < updateList.size(); ++i) {
                processUpdateSet(updateSet, updateList, i, columnNameValMap, columnSetIndexMap, index++, tableName);
            }
        }
    }

    private void processUpdateSet(UpdateSet updateSet, ExpressionList<Expression> updateList, int i,
                                  Map<String, Object> columnNameValMap, Map<Integer, String> columnSetIndexMap,
                                  int index, String tableName) {
        Expression updateExps = updateList.get(i);
        if (!(updateExps instanceof JdbcParameter)) {
            String columnName = updateSet.getColumns().get(i).getColumnName().toUpperCase();
            columnNameValMap.put(columnName, updateExps.toString());
        }
        columnSetIndexMap.put(index, updateSet.getColumns().get(i).getColumnName().toUpperCase());
    }

    private Map<String, String> buildRelatedColumnsMap(List<Column> selectItemsFromUpdateSql) {
        Map<String, String> relatedColumnsUpperCaseWithoutUnderline = new HashMap<>(selectItemsFromUpdateSql.size());
        for (Column item : selectItemsFromUpdateSql) {
            relatedColumnsUpperCaseWithoutUnderline.put(item.getColumnName().replaceAll("[._\\-$]", "").toUpperCase(), item.getColumnName().toUpperCase());
        }
        return relatedColumnsUpperCaseWithoutUnderline;
    }


    /**
     * @param originalDataObj
     * @return
     */
    private List<DataChangeRecorderInnerInterceptor.DataChangedRecord> compareAndGetUpdatedColumnDatas(String tableName, DataChangeRecorderInnerInterceptor.OriginalDataObj originalDataObj, Map<String, Object> columnNameValMap) {
        final Set<String> ignoredColumns = ignoredTableColumns.get(tableName.toUpperCase());
        if (originalDataObj == null || originalDataObj.isEmpty()) {
            DataChangeRecorderInnerInterceptor.DataChangedRecord oneRecord = new DataChangeRecorderInnerInterceptor.DataChangedRecord();
            List<DataChangeRecorderInnerInterceptor.DataColumnChangeResult> updateColumns = new ArrayList<>(columnNameValMap.size());
            for (Map.Entry<String, Object> ety : columnNameValMap.entrySet()) {
                String columnName = ety.getKey();
                if ((ignoredColumns == null || !ignoredColumns.contains(columnName)) && !ignoreAllColumns.contains(columnName)) {
                    updateColumns.add(DataChangeRecorderInnerInterceptor.DataColumnChangeResult.constrcutByUpdateVal(columnName, ety.getValue()));
                }
            }
            oneRecord.setUpdatedColumns(updateColumns);
//            oneRecord.setUpdatedColumns(Collections.EMPTY_LIST);
            return Collections.singletonList(oneRecord);
        }
        List<DataChangeRecorderInnerInterceptor.DataChangedRecord> originalDataList = originalDataObj.getOriginalDataObj();
        List<DataChangeRecorderInnerInterceptor.DataChangedRecord> updateDataList = new ArrayList<>(originalDataList.size());
        for (DataChangeRecorderInnerInterceptor.DataChangedRecord originalData : originalDataList) {
            if (originalData.hasUpdate(columnNameValMap, ignoredColumns, ignoreAllColumns)) {
                updateDataList.add(originalData);
            }
        }
        return updateDataList;
    }

    private Object getEnumValue(Enum enumVal) {
        Optional<String> enumValueFieldName = MybatisEnumTypeHandler.findEnumValueFieldName(enumVal.getClass());
        if (enumValueFieldName.isPresent()) {
            return SystemMetaObject.forObject(enumVal).getValue(enumValueFieldName.get());
        }
        return enumVal;

    }

    @SuppressWarnings("rawtypes")
    private void dealWithUpdateWrapper(Map<Integer, String> columnSetIndexMap, Map<String, Object> columnNameValMap, BoundSql updateSql) {
        if (columnSetIndexMap.size() <= columnNameValMap.size()) {
            return;
        }
        MetaObject mpgenVal = SystemMetaObject.forObject(updateSql.getParameterObject());
        if (!mpgenVal.hasGetter(Constants.WRAPPER)) {
            return;
        }
        Object ew = mpgenVal.getValue(Constants.WRAPPER);
        if (ew instanceof UpdateWrapper || ew instanceof LambdaUpdateWrapper) {
            processSqlSet((AbstractWrapper) ew, columnSetIndexMap, columnNameValMap);
        }
    }

    private void processSqlSet(AbstractWrapper ew, Map<Integer, String> columnSetIndexMap, Map<String, Object> columnNameValMap) {
        String sqlSet = (ew instanceof UpdateWrapper) ? ((UpdateWrapper) ew).getSqlSet() : ((LambdaUpdateWrapper) ew).getSqlSet();
        if (sqlSet == null) {
            return;
        }

        MetaObject ewMeta = SystemMetaObject.forObject(ew);
        Map paramNameValuePairs = (Map) ewMeta.getValue("paramNameValuePairs");

        String[] setItems = sqlSet.split(",");
        for (String setItem : setItems) {
            processSetItem(setItem.trim(), columnSetIndexMap, columnNameValMap, paramNameValuePairs);
        }
    }

    private void processSetItem(String setItem, Map<Integer, String> columnSetIndexMap, Map<String, Object> columnNameValMap, Map paramNameValuePairs) {
        String[] nameAndValuePair = setItem.split("=", 2);
        if (nameAndValuePair.length == 2) {
            String setColName = nameAndValuePair[0].trim().toUpperCase();
            String setColVal = nameAndValuePair[1].trim();

            if (columnSetIndexMap.containsValue(setColName)) {
                String mpGenKey = getMpGenKey(setColVal);
                Object setVal = paramNameValuePairs.get(mpGenKey);

                if (setVal instanceof IEnum) {
                    columnNameValMap.put(setColName, String.valueOf(((IEnum<?>) setVal).getValue()));
                } else {
                    columnNameValMap.put(setColName, String.valueOf(setVal));
                }
            }
        }
    }

    private String getMpGenKey(String setColVal) {
        String[] mpGenKeyArray = setColVal.split("\\.");
        return mpGenKeyArray[mpGenKeyArray.length - 1].replace("}", "");
    }

    private Map<String, String> detectInsertColumnValuesNonJdbcParameters(Insert insert) {
        Map<String, String> columnNameValMap = new HashMap<>(4);
        final Select select = insert.getSelect();
        List<Column> columns = insert.getColumns();
        if (select instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) select;
            final List<Select> selects = setOperationList.getSelects();
            if (CollectionUtils.isEmpty(selects)) {
                return columnNameValMap;
            }
            final Select selectBody = selects.get(0);
            if (!(selectBody instanceof Values)) {
                return columnNameValMap;
            }
            Values valuesStatement = (Values) selectBody;
            if (valuesStatement.getExpressions() instanceof ExpressionList) {
                extracted(columnNameValMap, columns, valuesStatement);
            }
        }
        return columnNameValMap;
    }

    private void extracted(Map<String, String> columnNameValMap, List<Column> columns, Values valuesStatement) {
        ExpressionList expressionList = valuesStatement.getExpressions();
        List<Expression> expressions = expressionList;
        for (Expression expression : expressions) {
            if (expression instanceof RowConstructor) {
                final ExpressionList exprList = ((RowConstructor) expression);
                final List<Expression> insertExpList = exprList;
                for (int i = 0; i < insertExpList.size(); ++i) {
                    Expression e = insertExpList.get(i);
                    if (!(e instanceof JdbcParameter)) {
                        final String columnName = columns.get(i).getColumnName();
                        final String val = e.toString();
                        columnNameValMap.put(columnName, val);
                    }
                }
            }
        }
    }

    private String getColumnNameByProperty(String propertyName, String tableName) {
        for (TableInfo tableInfo : TableInfoHelper.getTableInfos()) {
            if (tableName.equalsIgnoreCase(tableInfo.getTableName())) {
                final List<TableFieldInfo> fieldList = tableInfo.getFieldList();
                if (CollectionUtils.isEmpty(fieldList)) {
                    return propertyName;
                }
                for (TableFieldInfo tableFieldInfo : fieldList) {
                    if (propertyName.equalsIgnoreCase(tableFieldInfo.getProperty())) {
                        return tableFieldInfo.getColumn().toUpperCase();
                    }
                }
                return propertyName;
            }
        }
        return propertyName;
    }


    private Map<String, Object> buildParameterObjectMap(BoundSql boundSql) {
        MetaObject metaObject = PluginUtils.getMetaObject(boundSql.getParameterObject());
        Map<String, Object> propertyValMap = new HashMap<>(boundSql.getParameterMappings().size());
        for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
            String propertyName = parameterMapping.getProperty();
            if (propertyName.startsWith("ew.paramNameValuePairs")) {
                continue;
            }
            Object propertyValue = metaObject.getValue(propertyName);
            propertyValMap.put(propertyName, propertyValue);
        }
        return propertyValMap;

    }


    private String buildOriginalData(Select selectStmt, MappedStatement mappedStatement, BoundSql boundSql, Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(selectStmt.toString())) {
            DefaultParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, boundSql.getParameterObject(), boundSql);
            parameterHandler.setParameters(statement);
            ResultSet resultSet = statement.executeQuery();
            final ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            StringBuilder sb = new StringBuilder("[");
            int count = 0;
            while (resultSet.next()) {
                ++count;
                if (checkTableBatchLimitExceeded(selectStmt, count)) {
                    logger.error("batch delete limit exceed: count={}, BATCH_UPDATE_LIMIT={}", count, BATCH_UPDATE_LIMIT);
                    throw DataChangeRecorderInnerInterceptor.DataUpdateLimitationException.DEFAULT;
                }
                sb.append("{");
                for (int i = 1; i <= columnCount; ++i) {
                    sb.append("\"").append(metaData.getColumnName(i)).append("\":\"");
                    Object res = resultSet.getObject(i);
                    if (res instanceof Clob) {
                        sb.append(DataChangeRecorderInnerInterceptor.DataColumnChangeResult.convertClob((Clob) res));
                    } else {
                        sb.append(res);
                    }
                    sb.append("\",");
                }
                sb.replace(sb.length() - 1, sb.length(), "}");
            }
            sb.append("]");
            resultSet.close();
            return sb.toString();
        } catch (Exception e) {
            if (e instanceof DataChangeRecorderInnerInterceptor.DataUpdateLimitationException) {
                throw (DataChangeRecorderInnerInterceptor.DataUpdateLimitationException) e;
            }
            logger.error("try to get record tobe deleted for selectStmt={}", selectStmt, e);
            return "failed to get original data";
        }
    }

    private DataChangeRecorderInnerInterceptor.OriginalDataObj buildOriginalObjectData(Map<String, Object> updatedColumnDatas, Select selectStmt, Column pk, MappedStatement mappedStatement, BoundSql boundSql, Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(selectStmt.toString())) {
            DefaultParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, boundSql.getParameterObject(), boundSql);
            parameterHandler.setParameters(statement);
            ResultSet resultSet = statement.executeQuery();
            List<DataChangeRecorderInnerInterceptor.DataChangedRecord> originalObjectDatas = new LinkedList<>();
            int count = 0;

            while (resultSet.next()) {
                ++count;
                if (checkTableBatchLimitExceeded(selectStmt, count)) {
                    logger.error("batch update limit exceed: count={}, BATCH_UPDATE_LIMIT={}", count, BATCH_UPDATE_LIMIT);
                    throw DataChangeRecorderInnerInterceptor.DataUpdateLimitationException.DEFAULT;
                }
                originalObjectDatas.add(prepareOriginalDataObj(updatedColumnDatas, resultSet, pk));
            }
            DataChangeRecorderInnerInterceptor.OriginalDataObj result = new DataChangeRecorderInnerInterceptor.OriginalDataObj();
            result.setOriginalDataObj(originalObjectDatas);
            resultSet.close();
            return result;
        } catch (Exception e) {
            if (e instanceof DataChangeRecorderInnerInterceptor.DataUpdateLimitationException) {
                throw (DataChangeRecorderInnerInterceptor.DataUpdateLimitationException) e;
            }
            logger.error("try to get record tobe updated for selectStmt={}", selectStmt, e);
            return new DataChangeRecorderInnerInterceptor.OriginalDataObj();
        }
    }

    /**
     * 防止出现全表批量更新
     * 默认一次更新不超过1000条
     *
     * @param selectStmt
     * @param count
     * @return
     */
    private boolean checkTableBatchLimitExceeded(Select selectStmt, int count) {
        if (!batchUpdateLimitationOpened) {
            return false;
        }
        final PlainSelect selectBody = (PlainSelect) selectStmt;
        final FromItem fromItem = selectBody.getFromItem();
        if (fromItem instanceof Table) {
            Table fromTable = (Table) fromItem;
            final String tableName = fromTable.getName().toUpperCase();
            if (!BATCH_UPDATE_LIMIT_MAP.containsKey(tableName)) {
                if (count > BATCH_UPDATE_LIMIT) {
                    logger.error("batch update limit exceed for tableName={}, BATCH_UPDATE_LIMIT={}, count={}",
                            tableName, BATCH_UPDATE_LIMIT, count);
                    return true;
                }
                return false;
            }
            final Integer limit = BATCH_UPDATE_LIMIT_MAP.get(tableName);
            if (count > limit) {
                logger.error("batch update limit exceed for configured tableName={}, BATCH_UPDATE_LIMIT={}, count={}",
                        tableName, limit, count);
                return true;
            }
            return false;
        }
        return count > BATCH_UPDATE_LIMIT;
    }


    /**
     * get records : include related column with original data in DB
     *
     * @param resultSet
     * @param pk
     * @return
     * @throws SQLException
     */
    private DataChangeRecorderInnerInterceptor.DataChangedRecord prepareOriginalDataObj(Map<String, Object> updatedColumnDatas, ResultSet resultSet, Column pk) throws SQLException {
        final ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<DataChangeRecorderInnerInterceptor.DataColumnChangeResult> originalColumnDatas = new LinkedList<>();
        DataChangeRecorderInnerInterceptor.DataColumnChangeResult pkval = null;
        for (int i = 1; i <= columnCount; ++i) {
            String columnName = metaData.getColumnName(i).toUpperCase();
            DataChangeRecorderInnerInterceptor.DataColumnChangeResult col;
            Object updateVal = updatedColumnDatas.get(columnName);
            if (updateVal != null && updateVal.getClass().getCanonicalName().startsWith("java.")) {
                col = DataChangeRecorderInnerInterceptor.DataColumnChangeResult.constrcutByOriginalVal(columnName, resultSet.getObject(i, updateVal.getClass()));
            } else {
                col = DataChangeRecorderInnerInterceptor.DataColumnChangeResult.constrcutByOriginalVal(columnName, resultSet.getObject(i));
            }
            if (pk != null && columnName.equalsIgnoreCase(pk.getColumnName())) {
                pkval = col;
            } else {
                originalColumnDatas.add(col);
            }
        }
        DataChangeRecorderInnerInterceptor.DataChangedRecord changedRecord = new DataChangeRecorderInnerInterceptor.DataChangedRecord();
        changedRecord.setOriginalColumnDatas(originalColumnDatas);
        if (pkval != null) {
            changedRecord.setPkColumnName(pkval.getColumnName());
            changedRecord.setPkColumnVal(pkval.getOriginalValue());
        }
        return changedRecord;
    }


    private DataChangeRecorderInnerInterceptor.Columns2SelectItemsResult buildColumns2SelectItems(String tableName, List<Column> columns) {
        if (columns == null || columns.isEmpty()) {
            return DataChangeRecorderInnerInterceptor.Columns2SelectItemsResult.build(Collections.singletonList(new SelectItem<>(new AllColumns())), 0);
        }
        List<SelectItem<?>> selectItems = new ArrayList<>(columns.size());
        for (Column column : columns) {
            selectItems.add(new SelectItem<>(column));
        }
        TableInfo tableInfo = getTableInfoByTableName(tableName);
        if (tableInfo == null) {
            return DataChangeRecorderInnerInterceptor.Columns2SelectItemsResult.build(selectItems, 0);
        }
        Column pk = new Column(tableInfo.getKeyColumn());
        selectItems.add(new SelectItem<>(pk));
        DataChangeRecorderInnerInterceptor.Columns2SelectItemsResult result = DataChangeRecorderInnerInterceptor.Columns2SelectItemsResult.build(selectItems, 1);
        result.setPk(pk);
        return result;
    }

    private String buildParameterObject(BoundSql boundSql) {
        Object paramObj = boundSql.getParameterObject();
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (paramObj instanceof Map) {
            Map<String, Object> paramMap = (Map<String, Object>) paramObj;
            int index = 1;
            boolean hasParamIndex = false;
            String key;
            while (paramMap.containsKey((key = "param" + index))) {
                Object paramIndex = paramMap.get(key);
                sb.append("\"").append(key).append("\"").append(":").append("\"").append(paramIndex).append("\"").append(",");
                hasParamIndex = true;
                ++index;
            }
            if (hasParamIndex) {
                sb.delete(sb.length() - 1, sb.length());
                sb.append("}");
                return sb.toString();
            }
            for (Map.Entry<String, Object> ety : paramMap.entrySet()) {
                sb.append("\"").append(ety.getKey()).append("\"").append(":").append("\"").append(ety.getValue()).append("\"").append(",");
            }
            sb.delete(sb.length() - 1, sb.length());
            sb.append("}");
            return sb.toString();
        }
        sb.append("param:").append(paramObj);
        sb.append("}");
        return sb.toString();
    }

    public DataChangeRecorderInnerInterceptor.OperationResult processDelete(Delete deleteStmt, MappedStatement mappedStatement, BoundSql boundSql, Connection connection) {
        Table table = deleteStmt.getTable();
        Expression where = deleteStmt.getWhere();
        PlainSelect selectBody = new PlainSelect();
        selectBody.setFromItem(table);
        selectBody.setSelectItems(Collections.singletonList(new SelectItem<>((new AllColumns()))));
        selectBody.setWhere(where);
        String originalData = buildOriginalData(selectBody, mappedStatement, boundSql, connection);
        DataChangeRecorderInnerInterceptor.OperationResult result = new DataChangeRecorderInnerInterceptor.OperationResult();
        result.setOperation("delete");
        result.setTableName(table.getName());
        result.setRecordStatus(originalData.startsWith("["));
        result.setChangedData(originalData);
        return result;
    }

    /**
     * 设置批量更新记录条数上限
     *
     * @param limit
     * @return
     */
    public DataChangeRecorderInnerInterceptor setBatchUpdateLimit(int limit) {
        this.BATCH_UPDATE_LIMIT = limit;
        return this;
    }

    public DataChangeRecorderInnerInterceptor openBatchUpdateLimitation() {
        this.batchUpdateLimitationOpened = true;
        return this;
    }

    public DataChangeRecorderInnerInterceptor configTableLimitation(String tableName, int limit) {
        this.BATCH_UPDATE_LIMIT_MAP.put(tableName.toUpperCase(), limit);
        return this;
    }

    /**
     * ignoredColumns = TABLE_NAME1.COLUMN1,COLUMN2; TABLE2.COLUMN1,COLUMN2; TABLE3.*; *.COLUMN1,COLUMN2
     * 多个表用分号分隔
     * TABLE_NAME1.COLUMN1,COLUMN2 : 表示忽略这个表的这2个字段
     * TABLE3.*: 表示忽略这张表的INSERT/UPDATE，delete暂时还保留
     * *.COLUMN1,COLUMN2:表示所有表的这个2个字段名都忽略
     *
     * @param properties
     */
    @Override
    public void setProperties(Properties properties) {

        String ignoredTableColumns = properties.getProperty(IGNORED_TABLE_COLUMN_PROPERTIES);
        if (ignoredTableColumns == null || ignoredTableColumns.trim().isEmpty()) {
            return;
        }
        String[] array = ignoredTableColumns.split(";");
        for (String table : array) {
            int index = table.indexOf(".");
            if (index == -1) {
                logger.warn("invalid data={} for ignoredColumns, format should be TABLE_NAME1.COLUMN1,COLUMN2; TABLE2.COLUMN1,COLUMN2;", table);
                continue;
            }
            String tableName = table.substring(0, index).trim().toUpperCase();
            String[] columnArray = table.substring(index + 1).split(",");
            Set<String> columnSet = new HashSet<>(columnArray.length);
            for (String column : columnArray) {
                column = column.trim().toUpperCase();
                if (column.isEmpty()) {
                    continue;
                }
                columnSet.add(column);
            }
            if ("*".equals(tableName)) {
                ignoreAllColumns.addAll(columnSet);
            } else {
                this.ignoredTableColumns.put(tableName, columnSet);
            }
        }
    }

    @Data
    public static class OperationResult {

        private String operation;
        private boolean recordStatus;
        private String tableName;
        private String changedData;
        /**
         * cost for this plugin, ms
         */
        private long cost;

        public void buildDataStr(List<DataChangeRecorderInnerInterceptor.DataChangedRecord> records) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (DataChangeRecorderInnerInterceptor.DataChangedRecord r : records) {
                sb.append(r.generateUpdatedDataStr()).append(",");
            }
            if (sb.length() == 1) {
                sb.append("]");
                changedData = sb.toString();
                return;
            }
            sb.replace(sb.length() - 1, sb.length(), "]");
            changedData = sb.toString();
        }

        @Override
        public String toString() {
            return "{" +
                    "\"tableName\":\"" + tableName + "\"," +
                    "\"operation\":\"" + operation + "\"," +
                    "\"recordStatus\":\"" + recordStatus + "\"," +
                    "\"changedData\":" + changedData + "," +
                    "\"cost(ms)\":" + cost + "}";
        }
    }

    @Data
    public static class Columns2SelectItemsResult {

        private Column pk;
        /**
         * all column with additional columns: ID, etc.
         */
        private List<SelectItem<?>> selectItems;
        /**
         * newly added column count from meta data.
         */
        private int additionalItemCount;

        public static DataChangeRecorderInnerInterceptor.Columns2SelectItemsResult build(List<SelectItem<?>> selectItems, int additionalItemCount) {
            DataChangeRecorderInnerInterceptor.Columns2SelectItemsResult result = new DataChangeRecorderInnerInterceptor.Columns2SelectItemsResult();
            result.setSelectItems(selectItems);
            result.setAdditionalItemCount(additionalItemCount);
            return result;
        }
    }

    @Data
    public static class OriginalDataObj {

        private List<DataChangeRecorderInnerInterceptor.DataChangedRecord> originalDataObj;

        public boolean isEmpty() {
            return originalDataObj == null || originalDataObj.isEmpty();
        }

    }

    @Data
    public static class DataColumnChangeResult {

        private String columnName;
        private Object originalValue;
        private Object updateValue;

        @SuppressWarnings("rawtypes")
        public boolean isDataChanged(Object updateValue) {
            if (!Objects.equals(originalValue, updateValue)) {
                if (originalValue instanceof Clob) {
                    String originalStr = convertClob((Clob) originalValue);
                    setOriginalValue(originalStr);
                    return !originalStr.equals(updateValue);
                }
                if ((ObjectUtil.equals(originalValue, "1") && ObjectUtil.equals(updateValue, "true")) || (ObjectUtil.equals(originalValue, "0") && ObjectUtil.equals(updateValue, "false"))) {
                    return false;
                }
                if (originalValue instanceof Comparable) {
                    Comparable original = (Comparable) originalValue;
                    Comparable update = (Comparable) updateValue;
                    try {
                        return update == null || original.compareTo(update) != 0;
                    } catch (Exception e) {
                        return true;
                    }
                }
                return true;
            }
            return false;
        }

        public static String convertClob(Clob clobObj) {
            try {
                return clobObj.getSubString(0, (int) clobObj.length());
            } catch (Exception e) {
                try (Reader is = clobObj.getCharacterStream()) {
                    char[] chars = new char[64];
                    int readChars;
                    StringBuilder sb = new StringBuilder();
                    while ((readChars = is.read(chars)) != -1) {
                        sb.append(chars, 0, readChars);
                    }
                    return sb.toString();
                } catch (Exception e2) {
                    //ignored
                    return "unknown clobObj";
                }
            }
        }

        public static DataChangeRecorderInnerInterceptor.DataColumnChangeResult constrcutByUpdateVal(String columnName, Object updateValue) {
            DataChangeRecorderInnerInterceptor.DataColumnChangeResult res = new DataChangeRecorderInnerInterceptor.DataColumnChangeResult();
            res.setColumnName(columnName);
            res.setUpdateValue(updateValue);
            return res;
        }

        public static DataChangeRecorderInnerInterceptor.DataColumnChangeResult constrcutByOriginalVal(String columnName, Object originalValue) {
            DataChangeRecorderInnerInterceptor.DataColumnChangeResult res = new DataChangeRecorderInnerInterceptor.DataColumnChangeResult();
            res.setColumnName(columnName);
            res.setOriginalValue(originalValue);
            return res;
        }

        public String generateDataStr() {
            StringBuilder sb = new StringBuilder();
            sb.append("\"").append(columnName).append("\"").append(":").append("\"").append(convertDoubleQuotes(originalValue)).append("->").append(convertDoubleQuotes(updateValue)).append("\"").append(",");
            return sb.toString();
        }

        public String convertDoubleQuotes(Object obj) {
            if (obj == null) {
                return null;
            }
            return obj.toString().replace("\"", "\\\"");
        }
    }

    @Data
    public static class DataChangedRecord {

        private String pkColumnName;
        private Object pkColumnVal;
        private List<DataChangeRecorderInnerInterceptor.DataColumnChangeResult> originalColumnDatas;
        private List<DataChangeRecorderInnerInterceptor.DataColumnChangeResult> updatedColumns;

        public boolean hasUpdate(Map<String, Object> columnNameValMap, Set<String> ignoredColumns, Set<String> ignoreAllColumns) {
            if (originalColumnDatas == null) {
                return true;
            }
            boolean hasUpdate = false;
            updatedColumns = new ArrayList<>(originalColumnDatas.size());
            for (DataChangeRecorderInnerInterceptor.DataColumnChangeResult originalColumn : originalColumnDatas) {
                final String columnName = originalColumn.getColumnName().toUpperCase();
                if (ignoredColumns != null && ignoredColumns.contains(columnName) || ignoreAllColumns.contains(columnName)) {
                    continue;
                }
                Object updatedValue = columnNameValMap.get(columnName);
                if (originalColumn.isDataChanged(updatedValue)) {
                    hasUpdate = true;
                    originalColumn.setUpdateValue(updatedValue);
                    updatedColumns.add(originalColumn);
                }
            }
            return hasUpdate;
        }

        public String generateUpdatedDataStr() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            if (pkColumnName != null) {
                sb.append("\"").append(pkColumnName).append("\"").append(":").append("\"").append(convertDoubleQuotes(pkColumnVal)).append("\"").append(",");
            }
            for (DataChangeRecorderInnerInterceptor.DataColumnChangeResult update : updatedColumns) {
                sb.append(update.generateDataStr());
            }
            sb.replace(sb.length() - 1, sb.length(), "}");
            return sb.toString();
        }

        public String convertDoubleQuotes(Object obj) {
            if (obj == null) {
                return null;
            }
            return obj.toString().replace("\"", "\\\"");
        }
    }

    public static class DataUpdateLimitationException extends MybatisPlusException {

        public DataUpdateLimitationException(String message) {
            super(message);
        }

        public static DataChangeRecorderInnerInterceptor.DataUpdateLimitationException DEFAULT = new DataChangeRecorderInnerInterceptor.DataUpdateLimitationException("本次操作 因超过系统安全阈值 被拦截，如需继续，请联系管理员!");
    }
}
