package com.promptfilm.platform.video;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 提交给万相 2.7 的视频生成参数与已编码图片。 */
public class WanVideoCreateRequest {
    private WanVideoMode mode = WanVideoMode.TEXT;
    private String prompt;
    private String resolution = "720P";
    private String ratio = "9:16";
    private Integer duration = 5;
    private Boolean promptExtend = true;
    private Boolean watermark = true;
    private Integer seed;
    private List<String> imageDataUrls = Collections.emptyList();

    /** @return 生成能力类型：TEXT、IMAGE 或 REFERENCE */
    public WanVideoMode getMode() {
        return mode;
    }

    /** @param mode 生成能力类型：TEXT、IMAGE 或 REFERENCE */
    public void setMode(WanVideoMode mode) {
        this.mode = mode;
    }

    /** @return 描述视频主体、动作、场景、镜头和风格的提示词 */
    public String getPrompt() {
        return prompt;
    }

    /** @param prompt 描述视频主体、动作、场景、镜头和风格的提示词 */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /** @return 输出清晰度，合法值为 720P 或 1080P，默认 720P */
    public String getResolution() {
        return resolution;
    }

    /** @param resolution 输出清晰度，合法值为 720P 或 1080P */
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    /** @return 文生或参考生视频的输出宽高比，默认 9:16 */
    public String getRatio() {
        return ratio;
    }

    /** @param ratio 输出宽高比，支持 16:9、9:16、1:1、4:3 或 3:4 */
    public void setRatio(String ratio) {
        this.ratio = ratio;
    }

    /** @return 输出视频时长，单位为秒，有效范围为 2 至 15，默认 5 */
    public Integer getDuration() {
        return duration;
    }

    /** @param duration 输出视频时长，单位为秒，有效范围为 2 至 15 */
    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    /** @return true 表示由模型扩写提示词，false 表示按原提示词生成 */
    public Boolean getPromptExtend() {
        return promptExtend;
    }

    /** @param promptExtend true 表示由模型扩写提示词，false 表示按原提示词生成 */
    public void setPromptExtend(Boolean promptExtend) {
        this.promptExtend = promptExtend;
    }

    /** @return true 表示添加平台水印，false 表示不添加平台水印 */
    public Boolean getWatermark() {
        return watermark;
    }

    /** @param watermark true 表示添加平台水印，false 表示不添加平台水印 */
    public void setWatermark(Boolean watermark) {
        this.watermark = watermark;
    }

    /** @return 随机数种子；为空时由万相自动生成，有效范围为 0 至 2147483647 */
    public Integer getSeed() {
        return seed;
    }

    /** @param seed 随机数种子；为空时由万相自动生成，有效范围为 0 至 2147483647 */
    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    /** @return 图片按上传顺序转换得到的完整 Base64 data URL，不包含时返回空列表 */
    public List<String> getImageDataUrls() {
        return imageDataUrls;
    }

    /** @param imageDataUrls 图片按上传顺序转换得到的完整 Base64 data URL */
    public void setImageDataUrls(List<String> imageDataUrls) {
        this.imageDataUrls = imageDataUrls == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(imageDataUrls));
    }
}
