package com.promptfilm.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/** 集中维护应用内唯一的 OkHttpClient 配置。 */
object HttpClientProvider {
    private const val TIMEOUT_SECONDS = 15L

    @Volatile
    private var client: OkHttpClient? = null

    /**
     * 获取共享网络客户端。
     *
     * @param debug true 时记录请求与响应头，false 时关闭网络日志
     * @return 线程安全复用的 OkHttpClient
     */
    fun get(debug: Boolean): OkHttpClient {
        return client ?: synchronized(this) {
            client ?: create(debug).also { client = it }
        }
    }

    private fun create(debug: Boolean): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (debug) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }
}

