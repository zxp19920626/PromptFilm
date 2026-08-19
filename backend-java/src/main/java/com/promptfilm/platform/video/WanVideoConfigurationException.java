package com.promptfilm.platform.video;

/** 表示本地运行环境缺少调用万相所需的非敏感地址或服务端凭据配置。 */
public class WanVideoConfigurationException extends RuntimeException {
    public WanVideoConfigurationException() {
        super("Wan video service is not configured.");
    }
}
