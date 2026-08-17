package com.promptfilm.feature.payment

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.promptfilm.core.router.AppRoutes
import com.promptfilm.core.ui.BaseActivity

/** 支付模块入口，真实支付渠道将在独立基础设施适配器中接入。 */
@Route(path = AppRoutes.PAYMENT)
class PaymentActivity : BaseActivity(R.layout.activity_payment) {
    override fun onViewReady(savedInstanceState: Bundle?) = Unit
}

