package com.promptfilm.platform.video;

import com.promptfilm.platform.common.api.ApiEnvelope;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将万相调用错误映射为稳定的演示 API 错误结构。 */
@RestControllerAdvice(assignableTypes = WanVideoController.class)
public class WanVideoExceptionHandler {

    /**
     * 将模式、图片数量或图片格式错误映射为稳定的参数错误。
     *
     * @param exception 客户端视频生成参数不符合接口合同时产生的异常
     * @return HTTP 400 与稳定错误码 INVALID_VIDEO_REQUEST
     */
    @ExceptionHandler(WanVideoRequestException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleInvalidRequest(
            WanVideoRequestException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiEnvelope.failure(
                        "INVALID_VIDEO_REQUEST", exception.getMessage(), null));
    }

    /**
     * 将缺少本地环境变量映射为服务未就绪。
     *
     * @param exception 缺少万相调用配置时产生的异常
     * @return HTTP 503 与稳定错误码 WAN_NOT_CONFIGURED
     */
    @ExceptionHandler(WanVideoConfigurationException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleConfiguration(
            WanVideoConfigurationException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiEnvelope.failure(
                        "WAN_NOT_CONFIGURED", exception.getMessage(), null));
    }

    /**
     * 将上游调用失败映射为网关错误，不透传上游响应正文。
     *
     * @param exception 万相拒绝请求、无响应或网络访问失败时产生的异常
     * @return HTTP 502、稳定错误码和可选上游 HTTP 状态码
     */
    @ExceptionHandler(WanVideoApiException.class)
    public ResponseEntity<ApiEnvelope<Map<String, Integer>>> handleUpstream(
            WanVideoApiException exception) {
        Map<String, Integer> data = exception.getUpstreamStatus() == null
                ? Collections.emptyMap()
                : Collections.singletonMap("upstreamStatus", exception.getUpstreamStatus());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiEnvelope.failure("WAN_UPSTREAM_ERROR", exception.getMessage(), data));
    }
}
