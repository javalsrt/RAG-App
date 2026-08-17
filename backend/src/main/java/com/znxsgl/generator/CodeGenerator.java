package com.znxsgl.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;

/**
 * MyBatis-Plus 代码生成器（开发工具，不参与应用启动）
 *
 * 使用方法：
 * 1. 在 application.yml 中确认数据库连接信息；
 * 2. 修改下方 CONFIG 中的 schema、需要生成的表名、输出包路径；
 * 3. 运行 main 方法，自动生成 Entity / Mapper / Service / Controller 样板代码。
 *
 * 配合 Lombok 模板：生成的 Entity 自动使用 @Data 注解。
 *
 * 注意：本类依赖 mybatis-plus-generator 和 velocity-engine-core（pom.xml 中 provided scope），
 *      只在编译期可见，不会打进最终 jar 包。
 */
public class CodeGenerator {

    private static final class CONFIG {
        /** 数据库连接（与 application.yml 保持一致） */
        static final String URL = "jdbc:mysql://localhost:3306/znxsglTest?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        static final String USERNAME = "root";
        static final String PASSWORD = "123456";

        /** 父包名 */
        static final String PARENT = "com.znxsgl";
        /** Entity 输出目录，留空使用默认 src/main/java */
        static final String OUTPUT_DIR = System.getProperty("user.dir") + "/src/main/java";

        /** 需要生成代码的表名（多个用逗号分隔） */
        static final String[] TABLES = {
                "course",
                "schedule",
                "chat_message"
        };

        /** 表前缀（生成类名时去掉），无前缀留空 */
        static final String TABLE_PREFIX = "";
    }

    public static void main(String[] args) {
        FastAutoGenerator.create(CONFIG.URL, CONFIG.USERNAME, CONFIG.PASSWORD)
                .globalConfig(c -> c
                        .author("znxsgl-codegen")
                        .outputDir(CONFIG.OUTPUT_DIR)
                        .disableOpenDir())
                .packageConfig(c -> c
                        .parent(CONFIG.PARENT)
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .controller("controller")
                        .pathInfo(Collections.singletonMap(OutputFile.xml,
                                System.getProperty("user.dir") + "/src/main/resources/mapper")))
                .strategyConfig(c -> c
                        .addInclude(CONFIG.TABLES)
                        .addTablePrefix(CONFIG.TABLE_PREFIX)
                        .entityBuilder()
                            .naming(NamingStrategy.underline_to_camel)
                            .columnNaming(NamingStrategy.underline_to_camel)
                            .enableLombok()           // 生成的 Entity 使用 @Data
                            .enableTableFieldAnnotation() // 生成 @TableField 注解
                            .logicDeleteColumnName("deleted")
                            .controllerBuilder()
                            .enableRestStyle()
                            .mapperBuilder()
                            .enableBaseColumnList()
                            .enableBaseResultMap())
                .templateEngine(new VelocityTemplateEngine())
                .execute();

        System.out.println("\n===== 代码生成完毕 =====");
        System.out.println("输出目录：" + CONFIG.OUTPUT_DIR);
        System.out.println("提示：如已有同名类，请先备份；生成的代码会覆盖现有文件！");
    }
}
