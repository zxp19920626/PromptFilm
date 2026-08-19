package com.promptfilm.platform.video;

/** 表示客户端提交的视频生成模式、图片或参数不符合演示接口合同。 */
public class WanVideoRequestException extends RuntimeException {
    public WanVideoRequestException(String message) {
        super(message);
    }
}
