package com.envision.bunny.infrastructure.mybatis.encrypt;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author jingjing.dong
 * @since 2021/6/6-17:53
 */
@MappedTypes(AESEncrypt.class)
@Slf4j(topic = "encrypt")
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    static final String KEY = "SDFASDF!@#ASDF";
    static final String IV = "234234SDFAAF!@%$#";
    static final AES aes = new AES(Mode.CBC, Padding.PKCS5Padding, CharSequenceUtil.bytes(KEY, StandardCharsets.UTF_8),
            CharSequenceUtil.bytes(IV,StandardCharsets.UTF_8));
    /**
     * 用于定义在Mybatis设置参数时该如何把Java类型的参数转换为对应的数据库类型
     *
     * @param ps        当前的PreparedStatement对象
     * @param i         当前参数的位置
     * @param parameter 当前参数的Java对象
     * @param jdbcType  当前参数的数据库类型
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        // 只要 parameter 非空都进行加密
        log.info("setNonNullParameter index <{}>, param <{}> ", i, parameter);
        ps.setString(i, aes.encryptHex(parameter,StandardCharsets.UTF_8));
    }

    /**
     * 用于在Mybatis获取数据结果集时如何把数据库类型转换为对应的Java类型
     *
     * @param rs         当前的结果集
     * @param columnName 当前的字段名称
     * @return 转换后的Java对象
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String r = rs.getString(columnName);
        return r == null ? null : aes.decryptStr(r);
    }

    /**
     * 用于在Mybatis通过字段位置获取字段数据时把数据库类型转换为对应的Java类型
     *
     * @param rs          当前的结果集
     * @param columnIndex 当前字段的位置
     * @return 转换后的Java对象
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String r = rs.getString(columnIndex);
        return r == null ? null : aes.decryptStr(r);
    }

    /**
     * 用于Mybatis在调用存储过程后把数据库类型的数据转换为对应的Java类型
     *
     * @param cs          当前的CallableStatement执行后的CallableStatement
     * @param columnIndex 当前输出参数的位置
     * @return 转换后的Java对象
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String r = cs.getString(columnIndex);
        // 兼容待修复的数据
        return r == null ? null : aes.decryptStr(r);
    }
}