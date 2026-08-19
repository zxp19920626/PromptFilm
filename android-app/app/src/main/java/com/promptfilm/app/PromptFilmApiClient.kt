package com.promptfilm.app

import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** 调用本地 PromptFilm 后端，应用内不保存或接触阿里云 API Key。 */
class PromptFilmApiClient(baseUrl: String) {
    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')

    /** Android 演示支持的三种 Wan2.7 视频生成能力。 */
    enum class GenerationMode {
        /** 仅提交提示词，调用 Wan2.7-T2V。 */
        TEXT,
        /** 提交一张首帧图片，调用 Wan2.7-I2V。 */
        IMAGE,
        /** 提交一至五张参考图片，调用 Wan2.7-R2V。 */
        REFERENCE,
    }

    /** 提交给本地后端的图片二进制数据。 */
    data class UploadImage(
        /** 用于 multipart 文件名的本地显示名称，不参与模型生成。 */
        val fileName: String,
        /** 与图片内容匹配的 MIME 类型，支持 image/jpeg、image/png、image/bmp、image/webp。 */
        val contentType: String,
        /** 从系统文档选择器读取的完整图片字节，单张不得超过 20 MB。 */
        val bytes: ByteArray,
    )

    /** 创建 Wan2.7 视频任务时由用户选择的生成参数。 */
    data class GenerationParameters(
        /** 输出清晰度档位，支持 720P 或 1080P。 */
        val resolution: String,
        /** 文生和参考生视频的宽高比，支持 16:9、9:16、1:1、4:3 或 3:4；图生模式由首帧决定。 */
        val ratio: String,
        /** 输出视频时长，单位为秒，有效范围为 2 至 15。 */
        val duration: Int,
        /** true 表示允许模型扩写提示词，false 表示原样使用提示词。 */
        val promptExtend: Boolean,
        /** true 表示在输出中添加 AI 生成水印，false 表示不添加。 */
        val watermark: Boolean,
    )

    /** 创建任务或查询任务后返回的业务数据。 */
    data class VideoTask(
        /** 创建任务响应中的唯一任务标识。 */
        val taskId: String,
        /** 当前任务状态，例如 PENDING、RUNNING、SUCCEEDED 或 FAILED。 */
        val status: String,
        /** 任务成功后的临时视频地址；任务未完成时为空。 */
        val videoUrl: String?,
        /** 任务失败时上游返回的机器可读错误码；任务正常时为空。 */
        val errorCode: String?,
    )

    /**
     * 使用用户当前选择的清晰度、比例、时长和生成开关创建 Wan2.7 视频任务。
     *
     * IMAGE 模式的输出比例由首帧图片决定，因此本地后端会忽略 ratio 参数。
     *
     * @param prompt 原样提交给万相的主体、动作、场景和镜头提示词
     * @param mode TEXT、IMAGE 或 REFERENCE
     * @param images TEXT 为空，IMAGE 恰好一张，REFERENCE 为一至五张
     * @param parameters 用户选择的输出清晰度、比例、时长、智能改写和水印设置
     * @return 包含任务标识和初始状态的数据
     */
    fun createTask(
        prompt: String,
        mode: GenerationMode,
        images: List<UploadImage>,
        parameters: GenerationParameters,
    ): VideoTask {
        validateImages(mode, images)
        validateParameters(parameters)
        val connection = openConnection("POST", "/api/demo/video-tasks")
        val boundary = "PromptFilm-${UUID.randomUUID()}"
        connection.doOutput = true
        connection.setChunkedStreamingMode(64 * 1024)
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        try {
            BufferedOutputStream(connection.outputStream).use { output ->
                writeField(output, boundary, "mode", mode.name)
                writeField(output, boundary, "prompt", prompt)
                writeField(output, boundary, "resolution", parameters.resolution)
                writeField(output, boundary, "ratio", parameters.ratio)
                writeField(output, boundary, "duration", parameters.duration.toString())
                writeField(output, boundary, "promptExtend", parameters.promptExtend.toString())
                writeField(output, boundary, "watermark", parameters.watermark.toString())
                images.forEachIndexed { index, image ->
                    writeImage(output, boundary, index, image)
                }
                output.write("--$boundary--\r\n".toByteArray(Charsets.US_ASCII))
            }
            return readTaskResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 查询指定任务的状态和视频结果。
     *
     * @param taskId 创建任务接口返回的任务标识
     * @return 当前任务状态，成功时包含临时视频地址
     */
    fun getTask(taskId: String): VideoTask {
        require(taskId.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { "Invalid task ID." }
        val connection = openConnection("GET", "/api/demo/video-tasks/$taskId")
        return try {
            readTaskResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 返回本地后端的视频转发地址，供 Android 原生播放器读取。
     *
     * @param taskId 已成功生成视频的任务标识
     * @return 不包含阿里云签名参数的本地 HTTP 视频地址
     */
    fun videoPlaybackUrl(taskId: String): String {
        require(taskId.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { "Invalid task ID." }
        return "$normalizedBaseUrl/api/demo/video-tasks/$taskId/video"
    }

    private fun validateImages(mode: GenerationMode, images: List<UploadImage>) {
        require(mode != GenerationMode.TEXT || images.isEmpty()) {
            "Text-to-video does not accept image files."
        }
        require(mode != GenerationMode.IMAGE || images.size == 1) {
            "Image-to-video requires exactly one image file."
        }
        require(mode != GenerationMode.REFERENCE || images.size in 1..5) {
            "Reference-to-video requires between one and five image files."
        }
        require(images.all { it.bytes.isNotEmpty() && it.bytes.size <= MAX_IMAGE_BYTES }) {
            "Each image must contain data and must not exceed 20 MB."
        }
    }

    private fun validateParameters(parameters: GenerationParameters) {
        require(parameters.resolution in ALLOWED_RESOLUTIONS) {
            "Resolution must be 720P or 1080P."
        }
        require(parameters.ratio in ALLOWED_RATIOS) {
            "Ratio must be 16:9, 9:16, 1:1, 4:3, or 3:4."
        }
        require(parameters.duration in MIN_DURATION_SECONDS..MAX_DURATION_SECONDS) {
            "Duration must be between 2 and 15 seconds."
        }
    }

    private fun openConnection(method: String, path: String): HttpURLConnection {
        return (URL(normalizedBaseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 35_000
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun writeField(
        output: BufferedOutputStream,
        boundary: String,
        name: String,
        value: String,
    ) {
        output.write("--$boundary\r\n".toByteArray(Charsets.US_ASCII))
        output.write(
            "Content-Disposition: form-data; name=\"$name\"\r\n".toByteArray(Charsets.US_ASCII),
        )
        output.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n".toByteArray(Charsets.US_ASCII))
        output.write(value.toByteArray(Charsets.UTF_8))
        output.write("\r\n".toByteArray(Charsets.US_ASCII))
    }

    private fun writeImage(
        output: BufferedOutputStream,
        boundary: String,
        index: Int,
        image: UploadImage,
    ) {
        val suffix = image.fileName.substringAfterLast('.', "jpg")
            .lowercase()
            .filter { it.isLetterOrDigit() }
            .take(5)
            .ifBlank { "jpg" }
        output.write("--$boundary\r\n".toByteArray(Charsets.US_ASCII))
        output.write(
            ("Content-Disposition: form-data; name=\"images\"; " +
                "filename=\"image-${index + 1}.$suffix\"\r\n")
                .toByteArray(Charsets.US_ASCII),
        )
        output.write("Content-Type: ${image.contentType}\r\n\r\n".toByteArray(Charsets.US_ASCII))
        output.write(image.bytes)
        output.write("\r\n".toByteArray(Charsets.US_ASCII))
    }

    private fun readTaskResponse(connection: HttpURLConnection): VideoTask {
        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (responseCode !in 200..299) {
            val message = responseText.takeIf { it.isNotBlank() }
                ?.let { runCatching { JSONObject(it).optString("message") }.getOrNull() }
                ?.takeIf { it.isNotBlank() }
                ?: "Backend request failed with HTTP $responseCode."
            throw IOException(message)
        }
        return parseTask(JSONObject(responseText))
    }

    private fun parseTask(envelope: JSONObject): VideoTask {
        val data = envelope.optJSONObject("data")
            ?: throw IOException("Backend response does not contain task data.")
        val output = data.optJSONObject("output")
            ?: throw IOException("Backend response does not contain task output.")
        val taskId = output.optString("task_id")
        if (taskId.isBlank()) {
            throw IOException("Backend response does not contain a task ID.")
        }
        return VideoTask(
            taskId = taskId,
            status = output.optString("task_status", "UNKNOWN"),
            videoUrl = output.optString("video_url").takeIf { it.isNotBlank() },
            errorCode = output.optString("code")
                .ifBlank { data.optString("code") }
                .takeIf { it.isNotBlank() },
        )
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
        const val MIN_DURATION_SECONDS = 2
        const val MAX_DURATION_SECONDS = 15
        val ALLOWED_RESOLUTIONS = setOf("720P", "1080P")
        val ALLOWED_RATIOS = setOf("16:9", "9:16", "1:1", "4:3", "3:4")
    }
}
