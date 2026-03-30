package com.envision.bunny.infrastructure.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;

// 代码自动生成器
public class Generator {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/moving?useSSL=false&useUnicode=true&characterEncoding=utf-8&rewriteBatchedStatements=true&autoReconnect=true&serverTimezone=Asia/Shanghai";
        String projectPath = System.getProperty("user.dir") +"/src/main/java";
        FastAutoGenerator.create(url, "robert", "123456")
                .globalConfig(builder -> {
                    builder.author("jingjing.dong")// 设置作者
                            .disableOpenDir()
                            .dateType(DateType.ONLY_DATE)
                            .outputDir(projectPath); // 指定输出目录
                })
                .packageConfig(builder -> {
                    builder.parent("com.envision.bunny.infrastructure.mybatis.plugin") // 设置父包名
                            .moduleName("operation_result") // 设置父包模块名
                            .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath)); // 设置mapperXml生成路径
                })
                // 设置需要生成的表名
                .strategyConfig(builder -> builder.addInclude("operation_result")
                        .entityBuilder().enableLombok() // 添加Lombok支持
                        .disableSerialVersionUID().enableRemoveIsPrefix()
                        .enableTableFieldAnnotation() // 开启生成实体时生成字段注解
                        .superClass(AuditingEntity.class)
                        .logicDeleteColumnName("is_deleted") //逻辑删除字段名(数据库)
                        .logicDeletePropertyName("deleted") //逻辑删除属性名(实体)
                        .versionColumnName("version") // 乐观锁字段名(数据库)
                        .versionPropertyName("version") // 乐观锁属性名(实体)
                        .idType(IdType.ASSIGN_ID)
                        .addIgnoreColumns("create_time","create_by","update_time","update_by")
/*                          .addTableFills(new Property("createTime", FieldFill.INSERT))
                        .addTableFills(new Property("updateTime", FieldFill.INSERT_UPDATE))
                        .addTableFills(new Property("createBy", FieldFill.INSERT))
                        .addTableFills(new Property("updateBy", FieldFill.INSERT_UPDATE))*/
                        .naming(NamingStrategy.underline_to_camel)
                        .columnNaming(NamingStrategy.underline_to_camel)
                        .controllerBuilder().enableRestStyle()//开启生成@RestController 控制器
                        .formatFileName("%sController") //格式化文件名称
                        .serviceBuilder()
                        .superServiceClass(IService.class)
                        .superServiceImplClass(ServiceImpl.class)
                        .formatServiceFileName("%sRepository")
                        .formatServiceImplFileName("%sRepositoryImpl")
                        .mapperBuilder()
                        .mapperAnnotation(org.apache.ibatis.annotations.Mapper.class)// 开启 @Mapper 注解
                        .formatMapperFileName("%sMapper")
                        .formatXmlFileName("%sMapper")
                        .build())
                // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }
}