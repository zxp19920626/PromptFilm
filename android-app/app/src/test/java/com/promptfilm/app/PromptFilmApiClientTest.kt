package com.promptfilm.app

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptFilmApiClientTest {
    @Test
    fun createTaskSendsSelectedGenerationParameters() {
        val requestBody = AtomicReference("")
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/api/demo/video-tasks") { exchange ->
                requestBody.set(exchange.requestBody.use { it.readBytes().toString(Charsets.UTF_8) })
                val response = """
                    {"code":"OK","message":"Success","data":{"output":{"task_id":"task-123","task_status":"PENDING"}}}
                """.trimIndent().toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, response.size.toLong())
                exchange.responseBody.use { it.write(response) }
            }
            start()
        }

        try {
            val task = PromptFilmApiClient("http://127.0.0.1:${server.address.port}")
                .createTask(
                    prompt = "测试提示词",
                    mode = PromptFilmApiClient.GenerationMode.TEXT,
                    images = emptyList(),
                    parameters = PromptFilmApiClient.GenerationParameters(
                        resolution = "1080P",
                        ratio = "16:9",
                        duration = 10,
                        promptExtend = false,
                        watermark = false,
                    ),
                )

            assertEquals("task-123", task.taskId)
            assertMultipartField(requestBody.get(), "resolution", "1080P")
            assertMultipartField(requestBody.get(), "ratio", "16:9")
            assertMultipartField(requestBody.get(), "duration", "10")
            assertMultipartField(requestBody.get(), "promptExtend", "false")
            assertMultipartField(requestBody.get(), "watermark", "false")
        } finally {
            server.stop(0)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun createTaskRejectsUnsupportedDurationBeforeNetworkRequest() {
        PromptFilmApiClient("http://127.0.0.1:1").createTask(
            prompt = "测试提示词",
            mode = PromptFilmApiClient.GenerationMode.TEXT,
            images = emptyList(),
            parameters = PromptFilmApiClient.GenerationParameters(
                resolution = "720P",
                ratio = "9:16",
                duration = 16,
                promptExtend = true,
                watermark = true,
            ),
        )
    }

    private fun assertMultipartField(body: String, name: String, value: String) {
        assertTrue(
            body.contains(
                "name=\"$name\"\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n$value\r\n",
            ),
        )
    }
}
