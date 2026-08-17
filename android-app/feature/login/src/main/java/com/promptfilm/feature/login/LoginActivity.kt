package com.promptfilm.feature.login

import android.os.Bundle
import android.widget.Button
import com.alibaba.android.arouter.facade.annotation.Route
import com.promptfilm.core.router.AppRouter
import com.promptfilm.core.router.AppRoutes
import com.promptfilm.core.ui.BaseActivity

/** 登录模块入口，只暴露 ARouter 路由，不向其它业务模块暴露实现类。 */
@Route(path = AppRoutes.LOGIN)
class LoginActivity : BaseActivity(R.layout.activity_login) {
    override fun onViewReady(savedInstanceState: Bundle?) {
        findViewById<Button>(R.id.openPaymentButton).setOnClickListener {
            AppRouter.navigate(this, AppRoutes.PAYMENT)
        }
    }
}

