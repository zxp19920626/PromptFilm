package com.promptfilm.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** PromptFilm 后端启动入口。 */
@SpringBootApplication
public class PromptFilmBackendApplication {
    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 由运行环境传入的启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PromptFilmBackendApplication.class, args);
    }
}
