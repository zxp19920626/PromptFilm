package com.promptfilm.platform.video;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 管理万相视频接口的本地运行配置，不持久化或输出 API Key。 */
@Component
@ConfigurationProperties(prefix = "app.wan")
public class WanVideoProperties {
    private String baseUrl = "";
    private String apiKey = "";
    private String textModel = "wan2.7-t2v-2026-06-12";
    private String imageModel = "wan2.7-i2v-2026-04-25";
    private String referenceModel = "wan2.7-r2v-2026-06-12";
    private String dataInspection = "";

    /** @return 当前业务空间所属地域的 API 根地址，以 /api/v1 结尾 */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** @param baseUrl 当前业务空间所属地域的 API 根地址，以 /api/v1 结尾 */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** @return 仅用于服务端调用万相接口的百炼 API Key */
    public String getApiKey() {
        return apiKey;
    }

    /** @param apiKey 仅用于服务端调用万相接口的百炼 API Key */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /** @return 文生视频任务使用的 Wan2.7-T2V 模型快照名称 */
    public String getTextModel() {
        return textModel;
    }

    /** @param textModel 文生视频任务使用的 Wan2.7-T2V 模型快照名称 */
    public void setTextModel(String textModel) {
        this.textModel = textModel;
    }

    /** @return 图生视频任务使用的 Wan2.7-I2V 模型快照名称 */
    public String getImageModel() {
        return imageModel;
    }

    /** @param imageModel 图生视频任务使用的 Wan2.7-I2V 模型快照名称 */
    public void setImageModel(String imageModel) {
        this.imageModel = imageModel;
    }

    /** @return 参考生视频任务使用的 Wan2.7-R2V 模型快照名称 */
    public String getReferenceModel() {
        return referenceModel;
    }

    /** @param referenceModel 参考生视频任务使用的 Wan2.7-R2V 模型快照名称 */
    public void setReferenceModel(String referenceModel) {
        this.referenceModel = referenceModel;
    }

    /** @return 模型创建请求使用的数据检查策略 JSON；为空时不发送对应请求头 */
    public String getDataInspection() {
        return dataInspection;
    }

    /** @param dataInspection 模型创建请求使用的数据检查策略 JSON；为空时不发送对应请求头 */
    public void setDataInspection(String dataInspection) {
        this.dataInspection = dataInspection;
    }

    /** @return 去除首尾空白后的数据检查策略 JSON；未配置时返回空字符串 */
    public String normalizedDataInspection() {
        return dataInspection == null ? "" : dataInspection.trim();
    }

    /**
     * 根据能力类型返回固定的 Wan2.7 模型快照。
     *
     * @param mode TEXT、IMAGE 或 REFERENCE
     * @return 与能力类型对应的模型快照名称
     */
    public String modelFor(WanVideoMode mode) {
        if (mode == WanVideoMode.IMAGE) {
            return imageModel;
        }
        if (mode == WanVideoMode.REFERENCE) {
            return referenceModel;
        }
        return textModel;
    }

    /** @return true 表示根地址、API Key 和模型名均已配置，false 表示至少缺少一项 */
    public boolean isConfigured() {
        return hasText(baseUrl)
                && hasText(apiKey)
                && hasText(textModel)
                && hasText(imageModel)
                && hasText(referenceModel);
    }

    /** @return 去除末尾斜杠后的 API 根地址，便于安全拼接固定路径 */
    public String normalizedBaseUrl() {
        String value = baseUrl == null ? "" : baseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
