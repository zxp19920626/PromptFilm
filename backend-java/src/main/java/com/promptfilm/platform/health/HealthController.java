package com.promptfilm.platform.health;

import com.promptfilm.platform.common.api.ApiEnvelope;
import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供无需数据库访问的进程健康检查。 */
@RestController
public class HealthController {

    /**
     * 返回 API 进程存活状态。
     *
     * @return 包含 status=UP 的统一响应
     */
    @GetMapping("/health")
    public ApiEnvelope<Map<String, String>> health() {
        return ApiEnvelope.success(Collections.singletonMap("status", "UP"));
    }
}

