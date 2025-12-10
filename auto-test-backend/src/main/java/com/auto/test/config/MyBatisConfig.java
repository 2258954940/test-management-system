package com.auto.test.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean; // 替换为MyBatis-Plus的工厂类
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 适配 MyBatis-Plus 的核心配置（同时兼容原生 MyBatis 注解 Mapper）
 */
@Configuration
@MapperScan(
    basePackages = "com.auto.test.mapper", 
    sqlSessionFactoryRef = "sqlSessionFactory"
)
public class MyBatisConfig {

    private final DataSource dataSource;

    public MyBatisConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // 核心：使用 MyBatis-Plus 的 MybatisSqlSessionFactoryBean（加载BaseMapper方法）
    @Bean(name = "sqlSessionFactory")
    public MybatisSqlSessionFactoryBean sqlSessionFactory() throws Exception {
        MybatisSqlSessionFactoryBean sessionFactory = new MybatisSqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource); // 关联数据源

        // 配置 MyBatis-Plus 全局属性（适配 BaseMapper）
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true); // 下划线转驼峰
        sessionFactory.setConfiguration(configuration);

        // 配置 MyBatis-Plus 全局配置（可选，比如主键策略）
        com.baomidou.mybatisplus.core.config.GlobalConfig globalConfig = new com.baomidou.mybatisplus.core.config.GlobalConfig();
        com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig dbConfig = new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig();
        dbConfig.setIdType(com.baomidou.mybatisplus.annotation.IdType.AUTO); // 主键自增
        globalConfig.setDbConfig(dbConfig);
        sessionFactory.setGlobalConfig(globalConfig);

        return sessionFactory;
    }

    // 事务管理器
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }
}