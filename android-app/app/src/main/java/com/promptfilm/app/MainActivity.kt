package com.promptfilm.app

import android.os.Bundle
import android.widget.Button
import com.promptfilm.core.router.AppRouter
import com.promptfilm.core.router.AppRoutes
import com.promptfilm.core.ui.BaseActivity

/** 基础框架首页，用于验证业务模块之间的路由隔离。 */
class MainActivity : BaseActivity(R.layout.activity_main) {
    override fun onViewReady(savedInstanceState: Bundle?) {
        findViewById<Button>(R.id.loginButton).setOnClickListener {
            AppRouter.navigate(this, AppRoutes.LOGIN)
        }
        findViewById<Button>(R.id.paymentButton).setOnClickListener {
            AppRouter.navigate(this, AppRoutes.PAYMENT)
        }
    }
}

