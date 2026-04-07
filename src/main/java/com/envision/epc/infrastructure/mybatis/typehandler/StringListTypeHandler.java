package com.envision.epc.infrastructure.mybatis.typehandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wenjun.gu
 * @since 2022/1/17-12:08
 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes({List.class})
public class StringListTypeHandler implements TypeHandler<List<String>> {

    @Override
    public void setParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        String result = dealListToOneStr(parameter);
        ps.setString(i, result);
    }

    private String dealListToOneStr(List<String> parameter) {
        if (parameter == null || parameter.size() <= 0) {
            return null;
        }
        return String.join(",", parameter);
    }

    @Override
    public List<String> getResult(ResultSet rs, String columnName) throws SQLException {
        if (StringUtils.isBlank(rs.getString(columnName))) {
            return new ArrayList<>();
        }
        return Arrays.stream(rs.getString(columnName).split(",")).collect(Collectors.toList());
    }

    @Override
    public List<String> getResult(ResultSet rs, int columnIndex) throws SQLException {
        if (StringUtils.isBlank(rs.getString(columnIndex))) {
            return new ArrayList<>();
        }
        return Arrays.stream(rs.getString(columnIndex).split(",")).collect(Collectors.toList());
    }

    @Override
    public List<String> getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String result = cs.getString(columnIndex);
        if (StringUtils.isBlank(result)) {
            return new ArrayList<>();
        }
        return Arrays.stream(result.split(",")).collect(Collectors.toList());
    }
}
