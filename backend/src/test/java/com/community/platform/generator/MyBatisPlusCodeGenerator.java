package com.community.platform.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * MyBatis-Plus 代码生成器（Spring Boot 3 + MP 3.5.x）
 *
 * 使用方式：
 * 1) 本地创建数据库 community_service
 * 2) 执行项目根目录的 data.sql 创建表结构
 * 3) 修改下方 DB_* 配置（或设置同名环境变量 DB_URL/DB_USERNAME/DB_PASSWORD）
 * 4) 直接运行 main 方法生成代码到：
 *    - src/main/java/com/community/platform/generated/...
 *    - src/main/resources/mapper/...
 */
public class MyBatisPlusCodeGenerator {

    private static String envOrDefault(String key, String defaultValue) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }

    public static void main(String[] args) {
        String dbUrl = envOrDefault(
                "DB_URL",
                "jdbc:mysql://localhost:3306/community_service?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
        );
        String dbUsername = envOrDefault("DB_USERNAME", "");
        // 默认密码按你当前本地环境设置；可通过环境变量覆盖
        String dbPassword = envOrDefault("DB_PASSWORD", "");

        if (dbUsername.isBlank() || dbPassword.isBlank()) {
            throw new IllegalStateException("Please set DB_USERNAME and DB_PASSWORD before running the generator.");
        }

        String backendDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString();
        String javaOutDir = Paths.get(backendDir, "src", "main", "java").toString();
        String resourcesOutDir = Paths.get(backendDir, "src", "main", "resources").toString();

        Map<OutputFile, String> pathInfo = new HashMap<>();
        pathInfo.put(OutputFile.xml, Paths.get(resourcesOutDir, "mapper").toString());

        FastAutoGenerator
                .create(dbUrl, dbUsername, dbPassword)
                .globalConfig(builder -> builder
                        .author("bishe")
                        .disableOpenDir()
                        .outputDir(javaOutDir)
                )
                .packageConfig(builder -> builder
                        .parent("com.community.platform")
                        .moduleName("generated")
                        .pathInfo(pathInfo)
                )
                .strategyConfig(builder -> {
                    builder.addInclude("sys_user", "service_request", "service_claim", "service_evaluation");
                    builder.entityBuilder()
                            .enableLombok()
                            .enableTableFieldAnnotation();
                    builder.mapperBuilder()
                            .enableMapperAnnotation();
                    builder.serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl");
                    builder.controllerBuilder()
                            .enableRestStyle();
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}

