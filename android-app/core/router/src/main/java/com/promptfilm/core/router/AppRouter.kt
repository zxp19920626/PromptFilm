package com.promptfilm.core.router

import android.app.Application
import android.content.Context
import com.alibaba.android.arouter.launcher.ARouter

/** 统一封装 ARouter 初始化与页面导航。 */
object AppRouter {
    /**
     * 初始化路由运行时。
     *
     * @param application 当前应用实例
     * @param debug true 时启用调试与路由日志，false 时使用发布模式
     */
    fun initialize(application: Application, debug: Boolean) {
        if (debug) {
            ARouter.openDebug()
            ARouter.openLog()
        }
        ARouter.init(application)
    }

    /**
     * 打开指定路由页面。
     *
     * @param context 发起导航的上下文
     * @param path 已在 AppRoutes 声明的路由地址
     */
    fun navigate(context: Context, path: String) {
        ARouter.getInstance().build(path).navigation(context)
    }
}

