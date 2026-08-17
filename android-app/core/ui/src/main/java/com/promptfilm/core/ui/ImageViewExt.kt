package com.promptfilm.core.ui

import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide

/**
 * 使用共享 Glide 配置加载远程图片。
 *
 * @param url 目标图片 URL，为空时展示占位图
 * @param placeholderRes 加载中及失败时使用的本地图片资源
 */
fun ImageView.loadUrl(url: String?, @DrawableRes placeholderRes: Int) {
    Glide.with(this)
        .load(url)
        .placeholder(placeholderRes)
        .error(placeholderRes)
        .into(this)
}

