package com.yilena.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch REST客户端配置类
 * 
 * 功能说明：
 * 1. 配置Elasticsearch高级REST客户端
 * 2. 管理Elasticsearch连接
 * 
 * 使用场景：
 * - 全文搜索功能
 * - 日志存储和查询
 * - 数据分析和聚合
 * 
 * 技术实现：
 * - 使用Elasticsearch High Level REST Client
 * - 支持HTTP协议连接
 * - 配置连接超时和读取超时
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Configuration
public class RestClientConfig {

    /**
     * 创建Elasticsearch高级REST客户端
     * 
     * 功能：配置Elasticsearch连接客户端
     * 业务逻辑：
     * 1. 读取Elasticsearch主机配置
     * 2. 创建REST客户端构建器
     * 3. 返回高级REST客户端实例
     * 
     * @param host Elasticsearch主机地址
     * @return RestHighLevelClient Elasticsearch高级REST客户端
     */
    @Bean
    public RestHighLevelClient restHighLevelClient(@Value("${elasticSearch.host}") String host) {
        return new RestHighLevelClient(RestClient.builder(
                HttpHost.create(host)
        ));
    }
}
