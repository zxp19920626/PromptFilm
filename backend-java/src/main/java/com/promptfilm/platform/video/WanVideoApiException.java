package com.promptfilm.platform.video;

/** 表示万相上游接口返回错误或没有返回有效响应。 */
public class WanVideoApiException extends RuntimeException {
    private final Integer upstreamStatus;

    public WanVideoApiException(String message, Integer upstreamStatus, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
    }

    /** @return 万相上游 HTTP 状态码；网络失败或无响应时为空 */
    public Integer getUpstreamStatus() {
        return upstreamStatus;
    }
}
