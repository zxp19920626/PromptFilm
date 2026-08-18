package com.promptfilm.app

import android.app.Activity
import android.os.Bundle

/** Android 空项目的唯一启动页面。 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
