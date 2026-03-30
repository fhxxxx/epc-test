package com.envision.bunny.infrastructure.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.envision.bunny.infrastructure.mybatis.plugin.MyDataPermHandler;
import com.envision.bunny.infrastructure.mybatis.plugin.MyDynamicTableNameHandler;
import com.envision.bunny.infrastructure.mybatis.plugin.MyTenantHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author jingjing.dong
 * @since 2021/4/15-17:38
 */
@Component
//@MapperScan("按需修改")
public class MybatisPlusConfig {
    @Value("${mybatis-plus.global-config.db-config.workerId}")
    Long workerId;
    @Value("${custom.mybatis-plus-plugin.tenant.includes}")
    List<String> tenantIncludes;
    @Value("${custom.mybatis-plus-plugin.tenant.excludes}")
    List<String> tenantExcludes;
    @Value("${custom.mybatis-plus-plugin.data-change-recorder.includes}")
    List<String> dataChangeRecorderIncludes;
    @Value("${custom.mybatis-plus-plugin.data-change-recorder.excludes}")
    List<String> dataChangeRecorderExcludes;
    @Value("${custom.mybatis-plus-plugin.data-perm.dept}")
    List<String> deptDataPermIncludes;
    @Value("${custom.mybatis-plus-plugin.data-perm.menu}")
    List<String> menuDataPermIncludes;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();

        /*
         mybatisPlusInterceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new MyTenantHandler(tenantIncludes, tenantExcludes)));
         mybatisPlusInterceptor.addInnerInterceptor(new DataPermissionInterceptor(new MyDataPermHandler(deptDataPermIncludes, menuDataPermIncludes)));
         mybatisPlusInterceptor.addInnerInterceptor(new DynamicTableNameInnerInterceptor(new MyDynamicTableNameHandler()));
         乐观锁
        */

        mybatisPlusInterceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 分页
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        /* mybatisPlusInterceptor.addInnerInterceptor(new MyDataChangeRecorder(dataChangeRecorderIncludes, dataChangeRecorderExcludes)); */
        return mybatisPlusInterceptor;
    }

    @Bean
    public IdentifierGenerator idGenerator() {
        return new MyIdGenerator(workerId);
    }

    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> configuration.addInterceptor(new MySqlInterceptor());
    }
}
