package com.promptfilm.core.ui

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity

/** 为 XML 页面统一提供布局装载入口。 */
abstract class BaseActivity(@param:LayoutRes private val layoutResId: Int) : AppCompatActivity() {
    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutResId)
        onViewReady(savedInstanceState)
    }

    /**
     * 在 XML 布局完成装载后初始化页面。
     *
     * @param savedInstanceState 系统恢复的页面状态，首次创建时为空
     */
    protected abstract fun onViewReady(savedInstanceState: Bundle?)
}
