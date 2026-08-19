package com.promptfilm.platform.video;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** 配置调用万相 API 使用的短连接 HTTP 客户端。 */
@Configuration
public class WanHttpConfiguration {

    /**
     * 创建带连接和读取超时的 HTTP 客户端。
     *
     * @param builder Spring Boot 提供的 HTTP 客户端构建器
     * @return 调用万相创建任务与查询任务接口的客户端
     */
    @Bean
    public RestTemplate wanRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
