package com.promptfilm.platform.video;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/** 封装万相 2.7 文生、图生、参考生视频任务的创建与查询请求。 */
@Service
public class WanVideoClient {
    private static final String CREATE_TASK_PATH =
            "/services/aigc/video-generation/video-synthesis";
    private static final int MAX_LOCAL_CACHE_ENTRIES = 8;

    private final RestTemplate restTemplate;
    private final WanVideoProperties properties;
    private final ConcurrentMap<String, JsonNode> terminalTaskCache =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, byte[]> videoCache = new ConcurrentHashMap<>();

    public WanVideoClient(RestTemplate wanRestTemplate, WanVideoProperties properties) {
        this.restTemplate = wanRestTemplate;
        this.properties = properties;
    }

    /**
     * 创建万相 2.7 视频生成异步任务。
     *
     * @param request 提示词、清晰度、宽高比、时长和生成选项
     * @return 万相返回的任务信息，其中 output.task_id 用于后续查询
     */
    public JsonNode createTask(WanVideoCreateRequest request) {
        ensureConfigured();

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("prompt", request.getPrompt());
        if (request.getMode() != WanVideoMode.TEXT) {
            List<Map<String, String>> media = request.getImageDataUrls().stream()
                    .map(dataUrl -> mediaItem(request.getMode(), dataUrl))
                    .collect(Collectors.toList());
            input.put("media", media);
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("resolution", request.getResolution());
        if (request.getMode() != WanVideoMode.IMAGE) {
            parameters.put("ratio", request.getRatio());
        }
        parameters.put("duration", request.getDuration());
        parameters.put("prompt_extend", request.getPromptExtend());
        parameters.put("watermark", request.getWatermark());
        if (request.getSeed() != null) {
            parameters.put("seed", request.getSeed());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.modelFor(request.getMode()));
        body.put("input", input);
        body.put("parameters", parameters);

        HttpHeaders headers = authenticatedHeaders();
        headers.set("X-DashScope-Async", "enable");
        String dataInspection = properties.normalizedDataInspection();
        if (!dataInspection.isEmpty()) {
            headers.set("X-DashScope-DataInspection", dataInspection);
        }

        return exchange(
                properties.normalizedBaseUrl() + CREATE_TASK_PATH,
                HttpMethod.POST,
                new HttpEntity<>(body, headers));
    }

    /**
     * 查询已经创建的万相视频任务。
     *
     * @param taskId 创建任务响应中 output.task_id 的值，有效期由万相平台决定
     * @return 万相返回的任务状态、生成结果或错误信息
     */
    public JsonNode getTask(String taskId) {
        ensureConfigured();
        JsonNode cachedTask = terminalTaskCache.get(taskId);
        if (cachedTask != null) {
            return cachedTask;
        }
        JsonNode task = exchange(
                properties.normalizedBaseUrl() + "/tasks/{taskId}",
                HttpMethod.GET,
                new HttpEntity<>(authenticatedHeaders()),
                taskId);
        if (isTerminal(task.path("output").path("task_status").asText())) {
            putBounded(terminalTaskCache, taskId, task);
        }
        return task;
    }

    /**
     * 下载已完成任务的视频内容，由本地后端转发给 Android 播放器。
     *
     * @param taskId 创建任务响应中 output.task_id 的值
     * @return 已生成 MP4 文件的完整二进制内容
     */
    public byte[] downloadVideo(String taskId) {
        byte[] cachedVideo = videoCache.get(taskId);
        if (cachedVideo != null) {
            return cachedVideo.clone();
        }
        JsonNode task = getTask(taskId);
        JsonNode output = task.path("output");
        if (!"SUCCEEDED".equalsIgnoreCase(output.path("task_status").asText())) {
            throw new WanVideoRequestException("Video task has not succeeded yet.");
        }

        String videoUrl = output.path("video_url").asText("");
        URI videoUri = validatedVideoUri(videoUrl);
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    videoUri, HttpMethod.GET, HttpEntity.EMPTY, byte[].class);
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                throw new WanVideoApiException("Wan video service returned an empty video.",
                        response.getStatusCodeValue(), null);
            }
            putBounded(videoCache, taskId, body.clone());
            return body;
        } catch (RestClientResponseException exception) {
            throw new WanVideoApiException("Wan video service rejected the video download.",
                    exception.getRawStatusCode(), exception);
        } catch (ResourceAccessException exception) {
            throw new WanVideoApiException("Wan video service video is temporarily unreachable.",
                    null, exception);
        }
    }

    private HttpHeaders authenticatedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getApiKey().trim());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, String> mediaItem(WanVideoMode mode, String dataUrl) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("type", mode == WanVideoMode.IMAGE ? "first_frame" : "reference_image");
        item.put("url", dataUrl);
        return item;
    }

    private URI validatedVideoUri(String videoUrl) {
        try {
            URI uri = URI.create(videoUrl);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || !host.toLowerCase(Locale.ROOT).endsWith(".aliyuncs.com")) {
                throw new WanVideoApiException("Wan video service returned an invalid video URL.",
                        null, null);
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new WanVideoApiException("Wan video service returned an invalid video URL.",
                    null, exception);
        }
    }

    private boolean isTerminal(String taskStatus) {
        return "SUCCEEDED".equalsIgnoreCase(taskStatus)
                || "FAILED".equalsIgnoreCase(taskStatus)
                || "CANCELED".equalsIgnoreCase(taskStatus)
                || "CANCELLED".equalsIgnoreCase(taskStatus);
    }

    private <T> void putBounded(ConcurrentMap<String, T> cache, String key, T value) {
        if (cache.size() >= MAX_LOCAL_CACHE_ENTRIES && !cache.containsKey(key)) {
            cache.keySet().stream().findFirst().ifPresent(cache::remove);
        }
        cache.put(key, value);
    }

    private JsonNode exchange(
            String url, HttpMethod method, HttpEntity<?> entity, Object... uriVariables) {
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, method, entity, JsonNode.class, uriVariables);
            if (response.getBody() == null) {
                throw new WanVideoApiException("Wan video service returned an empty response.",
                        response.getStatusCodeValue(), null);
            }
            return response.getBody();
        } catch (RestClientResponseException exception) {
            throw new WanVideoApiException("Wan video service rejected the request.",
                    exception.getRawStatusCode(), exception);
        } catch (ResourceAccessException exception) {
            throw new WanVideoApiException("Wan video service is temporarily unreachable.",
                    null, exception);
        }
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw new WanVideoConfigurationException();
        }
    }
}
