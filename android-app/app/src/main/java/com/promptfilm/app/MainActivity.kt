package com.promptfilm.app

import android.app.Activity
import android.content.Intent
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import java.net.URI
import java.util.concurrent.Executors

/** 提供 Android 到本地后端再到万相 2.7 的三能力可视化验证页面。 */
class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var backendUrlInput: EditText
    private lateinit var generationModeGroup: RadioGroup
    private lateinit var imageUploadSection: LinearLayout
    private lateinit var imageUploadLabel: TextView
    private lateinit var selectImagesButton: Button
    private lateinit var selectedImagesText: TextView
    private lateinit var promptInput: EditText
    private lateinit var resolutionSpinner: Spinner
    private lateinit var ratioSpinner: Spinner
    private lateinit var durationSpinner: Spinner
    private lateinit var promptExtendSwitch: Switch
    private lateinit var watermarkSwitch: Switch
    private lateinit var parametersText: TextView
    private lateinit var generateButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var videoContainer: View
    private lateinit var videoView: TextureView
    private var selectedImageUris: List<Uri> = emptyList()
    private var mediaPlayer: MediaPlayer? = null
    private var pendingVideoUrl: String? = null
    private var pendingTaskId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        backendUrlInput = findViewById(R.id.backendUrlInput)
        generationModeGroup = findViewById(R.id.generationModeGroup)
        imageUploadSection = findViewById(R.id.imageUploadSection)
        imageUploadLabel = findViewById(R.id.imageUploadLabel)
        selectImagesButton = findViewById(R.id.selectImagesButton)
        selectedImagesText = findViewById(R.id.selectedImagesText)
        promptInput = findViewById(R.id.promptInput)
        resolutionSpinner = findViewById(R.id.resolutionSpinner)
        ratioSpinner = findViewById(R.id.ratioSpinner)
        durationSpinner = findViewById(R.id.durationSpinner)
        promptExtendSwitch = findViewById(R.id.promptExtendSwitch)
        watermarkSwitch = findViewById(R.id.watermarkSwitch)
        parametersText = findViewById(R.id.parametersText)
        generateButton = findViewById(R.id.generateButton)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        videoContainer = findViewById(R.id.videoContainer)
        videoView = findViewById(R.id.videoView)

        generationModeGroup.check(R.id.textModeButton)
        generationModeGroup.setOnCheckedChangeListener { _, _ -> updateModeUi(clearImages = true) }
        val parameterSelectionListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                updateParametersSummary()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        resolutionSpinner.onItemSelectedListener = parameterSelectionListener
        ratioSpinner.onItemSelectedListener = parameterSelectionListener
        durationSpinner.onItemSelectedListener = parameterSelectionListener
        promptExtendSwitch.setOnCheckedChangeListener { _, _ -> updateParametersSummary() }
        watermarkSwitch.setOnCheckedChangeListener { _, _ -> updateParametersSummary() }
        selectImagesButton.setOnClickListener { openImagePicker() }
        generateButton.setOnClickListener { startGeneration() }
        configureVideoSurface()
        updateModeUi(clearImages = false)
        resumePendingTaskIfAny()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != IMAGE_PICK_REQUEST || resultCode != RESULT_OK || data == null) {
            return
        }

        val uris = mutableListOf<Uri>()
        data.clipData?.let { clipData ->
            for (index in 0 until clipData.itemCount) {
                uris.add(clipData.getItemAt(index).uri)
            }
        }
        data.data?.let { uris.add(it) }
        val uniqueUris = uris.distinctBy(Uri::toString)
        val maximum = if (selectedMode() == PromptFilmApiClient.GenerationMode.IMAGE) 1 else 5
        if (uniqueUris.isEmpty() || uniqueUris.size > maximum) {
            selectedImagesText.text = if (maximum == 1) {
                getString(R.string.error_single_image)
            } else {
                getString(R.string.error_reference_images)
            }
            selectedImageUris = emptyList()
            return
        }

        uniqueUris.forEach { uri ->
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        selectedImageUris = uniqueUris
        updateSelectedImagesText()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        worker.shutdownNow()
        releaseMediaPlayer()
        super.onDestroy()
    }

    private fun configureVideoSurface() {
        videoView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                val videoUrl = pendingVideoUrl ?: return
                val taskId = pendingTaskId ?: return
                startVideoPlayback(surfaceTexture, taskId, videoUrl)
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int,
            ) = Unit

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                releaseMediaPlayer()
                return true
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
        }
    }

    private fun selectedMode(): PromptFilmApiClient.GenerationMode {
        return when (generationModeGroup.checkedRadioButtonId) {
            R.id.imageModeButton -> PromptFilmApiClient.GenerationMode.IMAGE
            R.id.referenceModeButton -> PromptFilmApiClient.GenerationMode.REFERENCE
            else -> PromptFilmApiClient.GenerationMode.TEXT
        }
    }

    private fun updateModeUi(clearImages: Boolean) {
        if (clearImages) {
            selectedImageUris = emptyList()
        }
        when (selectedMode()) {
            PromptFilmApiClient.GenerationMode.TEXT -> {
                imageUploadSection.visibility = View.GONE
            }
            PromptFilmApiClient.GenerationMode.IMAGE -> {
                imageUploadSection.visibility = View.VISIBLE
                imageUploadLabel.setText(R.string.image_upload_single_label)
            }
            PromptFilmApiClient.GenerationMode.REFERENCE -> {
                imageUploadSection.visibility = View.VISIBLE
                imageUploadLabel.setText(R.string.image_upload_reference_label)
            }
        }
        ratioSpinner.isEnabled = selectedMode() != PromptFilmApiClient.GenerationMode.IMAGE
        updateParametersSummary()
        updateSelectedImagesText()
    }

    private fun selectedParameters(): PromptFilmApiClient.GenerationParameters {
        return PromptFilmApiClient.GenerationParameters(
            resolution = resolutionSpinner.selectedItem.toString(),
            ratio = ratioSpinner.selectedItem.toString(),
            duration = durationSpinner.selectedItem.toString().toInt(),
            promptExtend = promptExtendSwitch.isChecked,
            watermark = watermarkSwitch.isChecked,
        )
    }

    private fun updateParametersSummary() {
        if (!::resolutionSpinner.isInitialized || resolutionSpinner.selectedItem == null) {
            return
        }
        val parameters = selectedParameters()
        val promptExtendText = getString(
            if (parameters.promptExtend) R.string.option_enabled else R.string.option_disabled,
        )
        val watermarkText = getString(
            if (parameters.watermark) R.string.option_enabled else R.string.option_disabled,
        )
        parametersText.text = if (selectedMode() == PromptFilmApiClient.GenerationMode.IMAGE) {
            getString(
                R.string.selected_image_parameters,
                parameters.resolution,
                parameters.duration,
                promptExtendText,
                watermarkText,
            )
        } else {
            getString(
                R.string.selected_parameters,
                parameters.resolution,
                parameters.ratio,
                parameters.duration,
                promptExtendText,
                watermarkText,
            )
        }
    }

    private fun openImagePicker() {
        val allowMultiple = selectedMode() == PromptFilmApiClient.GenerationMode.REFERENCE
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, IMAGE_PICK_REQUEST)
    }

    private fun updateSelectedImagesText() {
        if (selectedImageUris.isEmpty()) {
            selectedImagesText.setText(R.string.no_image_selected)
            return
        }
        val names = selectedImageUris.joinToString("、") { displayName(it) }
        selectedImagesText.text = getString(
            R.string.selected_images_count,
            selectedImageUris.size,
            names,
        )
    }

    private fun displayName(uri: Uri): String {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "image"
    }

    private fun startGeneration() {
        val backendUrl = backendUrlInput.text.toString().trim()
        val prompt = promptInput.text.toString().trim()
        val mode = selectedMode()
        val parameters = selectedParameters()
        if (!isValidBackendUrl(backendUrl)) {
            backendUrlInput.error = getString(R.string.error_backend_url)
            return
        }
        if (prompt.isBlank()) {
            promptInput.error = getString(R.string.error_prompt)
            return
        }
        if (mode == PromptFilmApiClient.GenerationMode.IMAGE && selectedImageUris.size != 1) {
            selectedImagesText.text = getString(R.string.error_single_image)
            return
        }
        if (mode == PromptFilmApiClient.GenerationMode.REFERENCE && selectedImageUris.size !in 1..5) {
            selectedImagesText.text = getString(R.string.error_reference_images)
            return
        }

        setLoading(true, getString(R.string.status_creating))
        pendingVideoUrl = null
        pendingTaskId = null
        releaseMediaPlayer()
        videoContainer.visibility = View.GONE
        val uris = selectedImageUris.toList()

        worker.execute {
            try {
                val images = readUploadImages(uris)
                val client = PromptFilmApiClient(backendUrl)
                val created = client.createTask(prompt, mode, images, parameters)
                savePendingTask(created.taskId, backendUrl)
                showStatus(getString(R.string.status_created, created.taskId))
                pollUntilCompleted(client, created.taskId)
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (exception: Exception) {
                showFailure(exception.message ?: "Unknown error.")
            }
        }
    }

    private fun resumePendingTaskIfAny() {
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        val extraTaskId = intent.getStringExtra(EXTRA_RESUME_TASK_ID)
        val taskId = extraTaskId ?: preferences.getString(PENDING_TASK_ID_KEY, null)
        if (taskId == null || !taskId.matches(Regex("[A-Za-z0-9_-]{1,128}"))) {
            return
        }
        val backendUrl = preferences.getString(PENDING_BACKEND_URL_KEY, null)
            ?.takeIf(::isValidBackendUrl)
            ?: backendUrlInput.text.toString().trim()
        if (!isValidBackendUrl(backendUrl)) {
            return
        }

        backendUrlInput.setText(backendUrl)
        savePendingTask(taskId, backendUrl)
        setLoading(true, getString(R.string.status_resuming, taskId))
        worker.execute {
            try {
                pollUntilCompleted(PromptFilmApiClient(backendUrl), taskId)
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (exception: Exception) {
                showFailure(exception.message ?: "Unknown error.")
            }
        }
    }

    private fun savePendingTask(taskId: String, backendUrl: String) {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .putString(PENDING_TASK_ID_KEY, taskId)
            .putString(PENDING_BACKEND_URL_KEY, backendUrl)
            .apply()
    }

    private fun clearPendingTask() {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .remove(PENDING_TASK_ID_KEY)
            .remove(PENDING_BACKEND_URL_KEY)
            .apply()
    }

    private fun readUploadImages(uris: List<Uri>): List<PromptFilmApiClient.UploadImage> {
        return uris.map { uri ->
            val contentType = normalizeImageType(contentResolver.getType(uri))
            require(contentType in ALLOWED_IMAGE_TYPES) {
                "仅支持 JPEG、PNG、BMP 或 WEBP 图片。"
            }
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException(getString(R.string.error_image_read, displayName(uri)))
            require(bytes.isNotEmpty() && bytes.size <= MAX_IMAGE_BYTES) {
                "每张图片必须包含内容且不得超过 20 MB。"
            }
            PromptFilmApiClient.UploadImage(
                fileName = displayName(uri),
                contentType = contentType,
                bytes = bytes,
            )
        }
    }

    private fun normalizeImageType(contentType: String?): String {
        val value = contentType?.trim()?.lowercase().orEmpty()
        return if (value == "image/jpg") "image/jpeg" else value
    }

    private fun pollUntilCompleted(client: PromptFilmApiClient, taskId: String) {
        repeat(MAX_POLL_ATTEMPTS) {
            Thread.sleep(POLL_INTERVAL_MILLIS)
            val task = client.getTask(taskId)
            showStatus(getString(R.string.status_polling, task.status, task.taskId))

            when (task.status.uppercase()) {
                "SUCCEEDED" -> {
                    task.videoUrl
                        ?: throw IllegalStateException("Completed task does not contain a video URL.")
                    showSuccess(task.taskId, client.videoPlaybackUrl(task.taskId))
                    return
                }
                "FAILED", "CANCELED", "CANCELLED", "UNKNOWN" -> {
                    clearPendingTask()
                    val errorSuffix = task.errorCode?.let { " (code: $it)" }.orEmpty()
                    throw IllegalStateException(
                        "Wan task ended with status ${task.status}$errorSuffix.",
                    )
                }
            }
        }
        mainHandler.post {
            setLoading(false, getString(R.string.error_timeout, taskId))
        }
    }

    private fun showSuccess(taskId: String, videoUrl: String) {
        mainHandler.post {
            setLoading(false, getString(R.string.status_succeeded, taskId))
            pendingTaskId = taskId
            pendingVideoUrl = videoUrl
            videoContainer.visibility = View.VISIBLE
            if (videoView.isAvailable) {
                videoView.surfaceTexture?.let { surfaceTexture ->
                    startVideoPlayback(surfaceTexture, taskId, videoUrl)
                }
            }
        }
    }

    private fun startVideoPlayback(
        surfaceTexture: SurfaceTexture,
        taskId: String,
        videoUrl: String,
    ) {
        releaseMediaPlayer()
        val player = MediaPlayer()
        mediaPlayer = player
        val surface = Surface(surfaceTexture)
        try {
            player.setSurface(surface)
        } finally {
            surface.release()
        }

        player.isLooping = true
        player.setOnPreparedListener { preparedPlayer ->
            if (mediaPlayer === preparedPlayer) {
                preparedPlayer.start()
                clearPendingTask()
                setLoading(false, getString(R.string.status_playing, taskId))
            }
        }
        player.setOnErrorListener { failedPlayer, what, extra ->
            if (mediaPlayer === failedPlayer) {
                mediaPlayer = null
            }
            failedPlayer.release()
            setLoading(
                false,
                getString(
                    R.string.status_playback_failed,
                    taskId,
                    what.toString(),
                    extra.toString(),
                ),
            )
            true
        }

        try {
            player.setDataSource(videoUrl)
            player.prepareAsync()
        } catch (exception: Exception) {
            if (mediaPlayer === player) {
                mediaPlayer = null
            }
            player.release()
            setLoading(
                false,
                getString(
                    R.string.status_playback_failed,
                    taskId,
                    PLAYBACK_SETUP_ERROR.toString(),
                    exception.javaClass.simpleName,
                ),
            )
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun showFailure(message: String) {
        mainHandler.post {
            setLoading(false, getString(R.string.status_failed, message))
        }
    }

    private fun showStatus(message: String) {
        mainHandler.post { statusText.text = message }
    }

    private fun setLoading(loading: Boolean, message: String) {
        generateButton.isEnabled = !loading
        selectImagesButton.isEnabled = !loading
        for (index in 0 until generationModeGroup.childCount) {
            generationModeGroup.getChildAt(index).isEnabled = !loading
        }
        resolutionSpinner.isEnabled = !loading
        ratioSpinner.isEnabled = !loading &&
            selectedMode() != PromptFilmApiClient.GenerationMode.IMAGE
        durationSpinner.isEnabled = !loading
        promptExtendSwitch.isEnabled = !loading
        watermarkSwitch.isEnabled = !loading
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        statusText.text = message
    }

    private fun isValidBackendUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        (uri.scheme == "http" || uri.scheme == "https") &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null &&
            uri.query == null &&
            uri.fragment == null
    }.getOrDefault(false)

    private companion object {
        const val IMAGE_PICK_REQUEST = 1201
        const val POLL_INTERVAL_MILLIS = 3_000L
        const val MAX_POLL_ATTEMPTS = 400
        const val PLAYBACK_SETUP_ERROR = -1
        const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
        const val EXTRA_RESUME_TASK_ID = "resume_task_id"
        const val PREFERENCES_NAME = "promptfilm_demo_state"
        const val PENDING_TASK_ID_KEY = "pending_task_id"
        const val PENDING_BACKEND_URL_KEY = "pending_backend_url"
        val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/bmp", "image/webp")
    }
}
