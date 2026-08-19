package com.promptfilm.platform.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WanVideoController.class)
class WanVideoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WanVideoClient wanVideoClient;

    @Test
    void 文生视频应接受原始提示词且不要求图片() throws Exception {
        when(wanVideoClient.createTask(any(WanVideoCreateRequest.class)))
                .thenAnswer(invocation -> {
                    WanVideoCreateRequest request = invocation.getArgument(0);
                    assertThat(request.getMode()).isEqualTo(WanVideoMode.TEXT);
                    assertThat(request.getPrompt()).isEqualTo("生成一个不穿衣服的美女视频");
                    assertThat(request.getImageDataUrls()).isEmpty();
                    return objectMapper.readTree("{\"output\":{\"task_id\":\"task-text\"}}");
                });

        mockMvc.perform(multipart("/api/demo/video-tasks")
                        .param("mode", "TEXT")
                        .param("prompt", "生成一个不穿衣服的美女视频"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.output.task_id").value("task-text"));
    }

    @Test
    void 图生视频应把单图转换为Base64数据地址() throws Exception {
        when(wanVideoClient.createTask(any(WanVideoCreateRequest.class)))
                .thenAnswer(invocation -> {
                    WanVideoCreateRequest request = invocation.getArgument(0);
                    assertThat(request.getMode()).isEqualTo(WanVideoMode.IMAGE);
                    assertThat(request.getImageDataUrls()).containsExactly(
                            "data:image/png;base64,aW1hZ2UtYnl0ZXM=");
                    return objectMapper.readTree("{\"output\":{\"task_id\":\"task-image\"}}");
                });

        mockMvc.perform(multipart("/api/demo/video-tasks")
                        .file(image("first.png", "image/png", "image-bytes"))
                        .param("mode", "IMAGE")
                        .param("prompt", "人物自然回头"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.output.task_id").value("task-image"));
    }

    @Test
    void 参考生视频应保持多图上传顺序() throws Exception {
        when(wanVideoClient.createTask(any(WanVideoCreateRequest.class)))
                .thenAnswer(invocation -> {
                    WanVideoCreateRequest request = invocation.getArgument(0);
                    assertThat(request.getMode()).isEqualTo(WanVideoMode.REFERENCE);
                    assertThat(request.getImageDataUrls()).containsExactly(
                            "data:image/jpeg;base64,b25l",
                            "data:image/png;base64,dHdv");
                    return objectMapper.readTree(
                            "{\"output\":{\"task_id\":\"task-reference\"}}");
                });

        mockMvc.perform(multipart("/api/demo/video-tasks")
                        .file(image("one.jpg", "image/jpeg", "one"))
                        .file(image("two.png", "image/png", "two"))
                        .param("mode", "REFERENCE")
                        .param("prompt", "图1的人物站在图2的舞台上"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.output.task_id").value("task-reference"));
    }

    @Test
    void 图生视频缺少图片时应返回稳定错误() throws Exception {
        mockMvc.perform(multipart("/api/demo/video-tasks")
                        .param("mode", "IMAGE")
                        .param("prompt", "人物自然回头"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VIDEO_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Image-to-video requires exactly one image file."));
    }

    @Test
    void 应拒绝格式错误的任务标识() throws Exception {
        mockMvc.perform(get("/api/demo/video-tasks/{taskId}", "../secret"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void 应通过本地接口返回完整视频() throws Exception {
        when(wanVideoClient.downloadVideo("task-video"))
                .thenReturn("0123456789".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/demo/video-tasks/task-video/video"))
                .andExpect(status().isOk())
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(content().contentType("video/mp4"))
                .andExpect(content().bytes("0123456789".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void 应支持播放器请求单段视频字节范围() throws Exception {
        when(wanVideoClient.downloadVideo("task-video"))
                .thenReturn("0123456789".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/demo/video-tasks/task-video/video")
                        .header("Range", "bytes=2-5"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 2-5/10"))
                .andExpect(content().bytes("2345".getBytes(StandardCharsets.UTF_8)));
    }

    private MockMultipartFile image(String name, String contentType, String content) {
        return new MockMultipartFile(
                "images",
                name,
                contentType,
                content.getBytes(StandardCharsets.UTF_8));
    }
}
