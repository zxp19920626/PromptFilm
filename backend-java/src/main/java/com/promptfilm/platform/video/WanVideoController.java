package com.promptfilm.platform.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.promptfilm.platform.common.api.ApiEnvelope;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 提供万相 2.7 文生、图生和参考生视频的本地演示接口。 */
@Validated
@RestController
@RequestMapping("/api/demo/video-tasks")
public class WanVideoController {
    private static final long MAX_IMAGE_BYTES = 20L * 1024L * 1024L;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/bmp", "image/webp");

    private final WanVideoClient wanVideoClient;

    public WanVideoController(WanVideoClient wanVideoClient) {
        this.wanVideoClient = wanVideoClient;
    }

    /**
     * 创建一个万相 2.7 视频生成异步任务。
     *
     * @param mode TEXT 为文生视频，IMAGE 为单图生视频，REFERENCE 为一至五图参考生视频
     * @param prompt 描述主体、动作、场景和镜头的提示词，最多 5000 个字符
     * @param resolution 输出清晰度，支持 720P 或 1080P
     * @param ratio 文生和参考生视频的输出宽高比；图生视频按首帧比例生成
     * @param duration 输出视频时长，单位为秒，有效范围为 2 至 15
     * @param promptExtend true 表示开启智能改写，false 表示按原提示词生成
     * @param watermark true 表示添加 AI 生成水印，false 表示不添加
     * @param seed 可选随机种子，有效范围为 0 至 2147483647；为空时由平台生成
     * @param images IMAGE 模式恰好一张图片，REFERENCE 模式一至五张图片，TEXT 模式为空
     * @return 包含上游 output.task_id 的任务响应
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiEnvelope<JsonNode> createTask(
            @RequestParam WanVideoMode mode,
            @RequestParam @NotBlank @Size(max = 5000) String prompt,
            @RequestParam(defaultValue = "720P")
            @Pattern(regexp = "720P|1080P") String resolution,
            @RequestParam(defaultValue = "9:16")
            @Pattern(regexp = "16:9|9:16|1:1|4:3|3:4") String ratio,
            @RequestParam(defaultValue = "5") @Min(2) @Max(15) Integer duration,
            @RequestParam(defaultValue = "true") Boolean promptExtend,
            @RequestParam(defaultValue = "true") Boolean watermark,
            @RequestParam(required = false) @Min(0) Integer seed,
            @RequestPart(name = "images", required = false) List<MultipartFile> images) {
        List<MultipartFile> suppliedImages = images == null
                ? Collections.emptyList()
                : images;
        validateImageCount(mode, suppliedImages.size());

        WanVideoCreateRequest request = new WanVideoCreateRequest();
        request.setMode(mode);
        request.setPrompt(prompt);
        request.setResolution(resolution);
        request.setRatio(ratio);
        request.setDuration(duration);
        request.setPromptExtend(promptExtend);
        request.setWatermark(watermark);
        request.setSeed(seed);
        request.setImageDataUrls(encodeImages(suppliedImages));
        return ApiEnvelope.success(wanVideoClient.createTask(request));
    }

    /**
     * 查询指定万相视频任务的当前状态和结果。
     *
     * @param taskId 创建任务时返回的任务标识，仅允许字母、数字、下划线和连字符
     * @return 万相返回的任务状态和结果，任务完成时包含视频地址
     */
    @GetMapping("/{taskId}")
    public ApiEnvelope<JsonNode> getTask(
            @PathVariable
            @Size(min = 1, max = 128)
            @Pattern(regexp = "[A-Za-z0-9_-]+") String taskId) {
        return ApiEnvelope.success(wanVideoClient.getTask(taskId));
    }

    /**
     * 通过本地后端返回已完成任务的视频，规避模拟器对临时 OSS 地址的网络限制。
     *
     * @param taskId 创建任务时返回的任务标识，仅允许字母、数字、下划线和连字符
     * @param range 可选的单段 HTTP 字节范围，例如 bytes=0-1023
     * @return MP4 全量内容或指定字节范围，供 Android 原生播放器读取
     */
    @GetMapping(value = "/{taskId}/video", produces = "video/mp4")
    public ResponseEntity<byte[]> getVideo(
            @PathVariable
            @Size(min = 1, max = 128)
            @Pattern(regexp = "[A-Za-z0-9_-]+") String taskId,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = HttpHeaders.RANGE, required = false) String range) {
        byte[] video = wanVideoClient.downloadVideo(taskId);
        if (range == null || range.isBlank()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentType(MediaType.valueOf("video/mp4"))
                    .contentLength(video.length)
                    .body(video);
        }
        ByteRange requested = parseRange(range, video.length);
        byte[] content = java.util.Arrays.copyOfRange(
                video, requested.start, requested.endInclusive + 1);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE,
                        "bytes " + requested.start + "-" + requested.endInclusive
                                + "/" + video.length)
                .contentType(MediaType.valueOf("video/mp4"))
                .contentLength(content.length)
                .body(content);
    }

    private void validateImageCount(WanVideoMode mode, int imageCount) {
        if (mode == WanVideoMode.TEXT && imageCount != 0) {
            throw new WanVideoRequestException("Text-to-video does not accept image files.");
        }
        if (mode == WanVideoMode.IMAGE && imageCount != 1) {
            throw new WanVideoRequestException("Image-to-video requires exactly one image file.");
        }
        if (mode == WanVideoMode.REFERENCE && (imageCount < 1 || imageCount > 5)) {
            throw new WanVideoRequestException(
                    "Reference-to-video requires between one and five image files.");
        }
    }

    private List<String> encodeImages(List<MultipartFile> images) {
        List<String> dataUrls = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image.isEmpty()) {
                throw new WanVideoRequestException("Uploaded image files must not be empty.");
            }
            if (image.getSize() > MAX_IMAGE_BYTES) {
                throw new WanVideoRequestException("Each uploaded image must not exceed 20 MB.");
            }
            String mediaType = normalizeMediaType(image.getContentType());
            if (!ALLOWED_IMAGE_TYPES.contains(mediaType)) {
                throw new WanVideoRequestException(
                        "Image format must be JPEG, PNG, BMP, or WEBP.");
            }
            try {
                String encoded = Base64.getEncoder().encodeToString(image.getBytes());
                dataUrls.add("data:" + mediaType + ";base64," + encoded);
            } catch (IOException exception) {
                throw new WanVideoRequestException("Uploaded image could not be read.");
            }
        }
        return dataUrls;
    }

    private String normalizeMediaType(String contentType) {
        if (contentType == null) {
            return "";
        }
        String value = contentType.trim().toLowerCase(Locale.ROOT);
        return "image/jpg".equals(value) ? "image/jpeg" : value;
    }

    private ByteRange parseRange(String header, int contentLength) {
        if (!header.matches("bytes=\\d*-\\d*")) {
            throw new WanVideoRequestException("Video byte range is invalid.");
        }
        String[] parts = header.substring("bytes=".length()).split("-", -1);
        try {
            int start;
            int end;
            if (parts[0].isEmpty()) {
                int suffixLength = Integer.parseInt(parts[1]);
                if (suffixLength <= 0) {
                    throw new IllegalArgumentException();
                }
                start = Math.max(0, contentLength - suffixLength);
                end = contentLength - 1;
            } else {
                start = Integer.parseInt(parts[0]);
                end = parts[1].isEmpty()
                        ? contentLength - 1
                        : Integer.parseInt(parts[1]);
            }
            if (start < 0 || start >= contentLength || end < start) {
                throw new IllegalArgumentException();
            }
            return new ByteRange(start, Math.min(end, contentLength - 1));
        } catch (IllegalArgumentException exception) {
            throw new WanVideoRequestException("Video byte range is invalid.");
        }
    }

    private static final class ByteRange {
        private final int start;
        private final int endInclusive;

        private ByteRange(int start, int endInclusive) {
            this.start = start;
            this.endInclusive = endInclusive;
        }
    }
}
