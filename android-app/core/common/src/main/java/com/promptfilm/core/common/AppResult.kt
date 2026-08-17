package com.promptfilm.core.common

/**
 * 跨模块异步结果，业务分支应依赖稳定错误码，不依赖可读文案。
 */
sealed interface AppResult<out T> {
    /** 请求成功并携带业务数据。 */
    data class Success<T>(val data: T) : AppResult<T>

    /**
     * 请求失败。
     *
     * @property code 供程序分支判断的稳定错误码
     * @property message 可直接展示的英文兜底文案
     * @property cause 仅用于受控日志的底层异常，不应直接展示
     */
    data class Failure(
        val code: String,
        val message: String,
        val cause: Throwable? = null,
    ) : AppResult<Nothing>
}

