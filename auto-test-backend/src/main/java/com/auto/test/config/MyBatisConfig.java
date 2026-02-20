package com.auto.test.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean; // 替换为MyBatis-Plus的工厂类
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 适配 MyBatis-Plus 的核心配置（同时兼容原生 MyBatis 注解 Mapper）
 * 新增：MyBatis-Plus 分页插件配置（解决日志分页total=0问题）
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

    /**
     * 核心：配置 MyBatis-Plus 分页插件（必须配置，否则分页失效）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 添加分页插件，指定数据库类型为MySQL（关键！）
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 2. 可选：设置单页最大条数，-1表示无限制
        paginationInnerInterceptor.setMaxLimit(1000L);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }

    // 核心：使用 MyBatis-Plus 的 MybatisSqlSessionFactoryBean（加载BaseMapper方法）
    @Bean(name = "sqlSessionFactory")
    public MybatisSqlSessionFactoryBean sqlSessionFactory(MybatisPlusInterceptor mybatisPlusInterceptor) throws Exception {
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

        // 使 XML Mapper 生效
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(resolver.getResources("classpath*:mapper/*.xml"));

        // 确保分页拦截器生效（自定义工厂需要显式注入插件）
        sessionFactory.setPlugins(new Interceptor[] { mybatisPlusInterceptor });

        return sessionFactory;
    }

    // 事务管理器
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }
}