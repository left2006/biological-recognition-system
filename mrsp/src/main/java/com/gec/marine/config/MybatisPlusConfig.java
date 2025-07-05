package com.gec.marine.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 创建分页拦截器，并指定数据库类型
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置请求的页面大于最大页后操作，true 表示调回到首页，false 表示继续请求（默认是 false）
        // paginationInterceptor.setOverflow(false);
        // 设置最大单页限制数量，-1 表示不受限制
        paginationInterceptor.setMaxLimit(-1L);
        // 将分页拦截器加入到 MybatisPlusInterceptor 中
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}
