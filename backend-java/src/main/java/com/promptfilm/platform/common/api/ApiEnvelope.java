package com.promptfilm.platform.common.api;

/**
 * API 统一响应结构。
 *
 * @param <T> 业务数据类型
 */
public final class ApiEnvelope<T> {
    private final String code;
    private final String message;
    private final T data;

    private ApiEnvelope(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应。
     *
     * @param data 本次请求返回的业务数据，可按接口合同为空
     * @param <T> 业务数据类型
     * @return 错误码为 OK 的统一响应
     */
    public static <T> ApiEnvelope<T> success(T data) {
        return new ApiEnvelope<>("OK", "Success", data);
    }

    /**
     * 创建失败响应。
     *
     * @param code 稳定的机器可读错误码，供客户端分支处理
     * @param message 可直接展示的英文兜底文案
     * @param data 与错误相关的结构化数据，可按接口合同为空
     * @param <T> 错误数据类型
     * @return 包含指定错误码、文案和错误数据的统一响应
     */
    public static <T> ApiEnvelope<T> failure(String code, String message, T data) {
        return new ApiEnvelope<>(code, message, data);
    }

    /** @return 稳定的机器可读业务码，成功时为 OK */
    public String getCode() {
        return code;
    }

    /** @return 可直接展示的英文兜底文案 */
    public String getMessage() {
        return message;
    }

    /** @return 接口业务数据，是否允许为空由具体接口合同决定 */
    public T getData() {
        return data;
    }
}
