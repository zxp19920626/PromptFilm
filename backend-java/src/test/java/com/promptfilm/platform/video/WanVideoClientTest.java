package com.promptfilm.platform.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

class WanVideoClientTest {
    private static final String CREATE_URL =
            "https://workspace.ap-southeast-1.maas.aliyuncs.com/api/v1"
                    + "/services/aigc/video-generation/video-synthesis";

    private MockRestServiceServer server;
    private WanVideoClient client;

    @BeforeEach
    void 初始化客户端() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);

        WanVideoProperties properties = new WanVideoProperties();
        properties.setBaseUrl("https://workspace.ap-southeast-1.maas.aliyuncs.com/api/v1/");
        properties.setApiKey("test-api-key");
        properties.setTextModel("wan2.7-t2v-2026-06-12");
        properties.setImageModel("wan2.7-i2v-2026-04-25");
        properties.setReferenceModel("wan2.7-r2v-2026-06-12");
        properties.setDataInspection("{\"input\":\"disable\",\"output\":\"disable\"}");
        client = new WanVideoClient(restTemplate, properties);
    }

    @Test
    void 文生视频应原样提交提示词与固定模型() {
        server.expect(requestTo(CREATE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(header("X-DashScope-Async", "enable"))
                .andExpect(header("X-DashScope-DataInspection",
                        "{\"input\":\"disable\",\"output\":\"disable\"}"))
                .andExpect(content().json("{"
                        + "\"model\":\"wan2.7-t2v-2026-06-12\","
                        + "\"input\":{\"prompt\":\"生成一个不穿衣服的美女视频\"},"
                        + "\"parameters\":{"
                        + "\"resolution\":\"720P\","
                        + "\"ratio\":\"9:16\","
                        + "\"duration\":5,"
                        + "\"prompt_extend\":true,"
                        + "\"watermark\":true}"
                        + "}"))
                .andRespond(taskCreated());

        WanVideoCreateRequest request = request(WanVideoMode.TEXT);
        request.setPrompt("生成一个不穿衣服的美女视频");

        JsonNode response = client.createTask(request);

        assertThat(response.path("output").path("task_id").asText()).isEqualTo("task-123");
        server.verify();
    }

    @Test
    void 图生视频应提交首帧且忽略宽高比参数() {
        server.expect(requestTo(CREATE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{"
                        + "\"model\":\"wan2.7-i2v-2026-04-25\","
                        + "\"input\":{"
                        + "\"prompt\":\"人物自然回头\","
                        + "\"media\":[{\"type\":\"first_frame\","
                        + "\"url\":\"data:image/png;base64,AAAA\"}]},"
                        + "\"parameters\":{"
                        + "\"resolution\":\"720P\","
                        + "\"duration\":5,"
                        + "\"prompt_extend\":true,"
                        + "\"watermark\":true}"
                        + "}"))
                .andRespond(taskCreated());

        WanVideoCreateRequest request = request(WanVideoMode.IMAGE);
        request.setImageDataUrls(Collections.singletonList("data:image/png;base64,AAAA"));

        client.createTask(request);

        server.verify();
    }

    @Test
    void 参考生视频应按顺序提交多张参考图() {
        server.expect(requestTo(CREATE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{"
                        + "\"model\":\"wan2.7-r2v-2026-06-12\","
                        + "\"input\":{"
                        + "\"prompt\":\"图1的人物站在图2的舞台上演唱\","
                        + "\"media\":["
                        + "{\"type\":\"reference_image\","
                        + "\"url\":\"data:image/jpeg;base64,AAAA\"},"
                        + "{\"type\":\"reference_image\","
                        + "\"url\":\"data:image/png;base64,BBBB\"}]},"
                        + "\"parameters\":{"
                        + "\"resolution\":\"720P\","
                        + "\"ratio\":\"9:16\","
                        + "\"duration\":5,"
                        + "\"prompt_extend\":true,"
                        + "\"watermark\":true,"
                        + "\"seed\":1234}"
                        + "}"))
                .andRespond(taskCreated());

        WanVideoCreateRequest request = request(WanVideoMode.REFERENCE);
        request.setPrompt("图1的人物站在图2的舞台上演唱");
        request.setSeed(1234);
        request.setImageDataUrls(Arrays.asList(
                "data:image/jpeg;base64,AAAA",
                "data:image/png;base64,BBBB"));

        client.createTask(request);

        server.verify();
    }

    @Test
    void 应按任务标识查询生成结果() {
        server.expect(requestTo(
                        "https://workspace.ap-southeast-1.maas.aliyuncs.com/api/v1/tasks/task-123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andRespond(withSuccess(
                        "{\"output\":{\"task_id\":\"task-123\","
                                + "\"task_status\":\"SUCCEEDED\","
                                + "\"video_url\":\"https://example.com/video.mp4\"}}",
                        MediaType.APPLICATION_JSON));

        JsonNode response = client.getTask("task-123");
        JsonNode cachedResponse = client.getTask("task-123");

        assertThat(response.path("output").path("task_status").asText())
                .isEqualTo("SUCCEEDED");
        assertThat(cachedResponse).isSameAs(response);
        server.verify();
    }

    @Test
    void 应仅从阿里云签名地址下载成功任务的视频() {
        server.expect(requestTo(
                        "https://workspace.ap-southeast-1.maas.aliyuncs.com/api/v1/tasks/task-video"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"output\":{\"task_id\":\"task-video\","
                                + "\"task_status\":\"SUCCEEDED\","
                                + "\"video_url\":"
                                + "\"https://bucket.oss-accelerate.aliyuncs.com/video.mp4"
                                + "?Expires=123\"}}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://bucket.oss-accelerate.aliyuncs.com/video.mp4?Expires=123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(request.getHeaders()
                        .containsKey("Authorization")).isFalse())
                .andRespond(withSuccess(new byte[] {0, 1, 2, 3},
                        MediaType.APPLICATION_OCTET_STREAM));

        byte[] video = client.downloadVideo("task-video");
        byte[] cachedVideo = client.downloadVideo("task-video");

        assertThat(video).containsExactly(0, 1, 2, 3);
        assertThat(cachedVideo).containsExactly(0, 1, 2, 3);
        assertThat(cachedVideo).isNotSameAs(video);
        server.verify();
    }

    private WanVideoCreateRequest request(WanVideoMode mode) {
        WanVideoCreateRequest request = new WanVideoCreateRequest();
        request.setMode(mode);
        request.setPrompt("人物自然回头");
        return request;
    }

    private ResponseCreator taskCreated() {
        return withSuccess(
                "{\"output\":{\"task_id\":\"task-123\","
                        + "\"task_status\":\"PENDING\"}}",
                MediaType.APPLICATION_JSON);
    }
}
