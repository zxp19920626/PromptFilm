package com.promptfilm.app

import android.app.Application
import android.content.pm.ApplicationInfo
import com.promptfilm.core.router.AppRouter

/** 应用装配入口，仅负责初始化跨模块基础设施。 */
class PromptFilmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        AppRouter.initialize(this, isDebuggable)
    }
}

