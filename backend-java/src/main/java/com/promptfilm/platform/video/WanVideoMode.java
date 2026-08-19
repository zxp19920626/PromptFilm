package com.promptfilm.platform.video;

/** 本地演示支持的万相 2.7 视频生成能力。 */
public enum WanVideoMode {
    /** 仅根据提示词生成视频，对应 Wan2.7-T2V。 */
    TEXT,
    /** 根据一张首帧图片和提示词生成视频，对应 Wan2.7-I2V。 */
    IMAGE,
    /** 根据一至五张参考图片和提示词生成视频，对应 Wan2.7-R2V。 */
    REFERENCE
}
