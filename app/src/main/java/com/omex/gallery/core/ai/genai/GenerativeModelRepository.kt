package com.omex.gallery.core.ai.genai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class GenerativeModelInfo(
    val isInstalled: Boolean,
    val isValidated: Boolean = false,
    val modelFileName: String = "gemma-2b-it-cpu-int4.task",
    val modelDirectory: String = "",
    val modelSizeMb: Float = 0f,
    val statusMessage: String = ""
)

class GenerativeModelRepository(private val context: Context) {

    private val defaultFileName = "gemma-2b-it-cpu-int4.task"

    private val _modelInfo = MutableStateFlow(checkModelStatus())
    val modelInfo: StateFlow<GenerativeModelInfo> = _modelInfo.asStateFlow()

    fun checkModelStatus(): GenerativeModelInfo {
        val modelsDir = getModelsDir()
        var modelFile = File(modelsDir, defaultFileName)

        // Find any valid .task model in the directory if default file doesn't exist
        if (!modelFile.exists() || modelFile.length() <= 0) {
            val taskFiles = modelsDir.listFiles { _, name -> name.endsWith(".task") }
            if (!taskFiles.isNullOrEmpty()) {
                modelFile = taskFiles[0]
            }
        }

        val exists = modelFile.exists() && modelFile.length() > 0 && modelFile.name.endsWith(".task")
        val sizeMb = if (exists) modelFile.length() / (1024f * 1024f) else 0f

        if (!exists) {
            return GenerativeModelInfo(
                isInstalled = false,
                isValidated = false,
                modelFileName = defaultFileName,
                modelDirectory = modelsDir.absolutePath,
                modelSizeMb = 0f,
                statusMessage = "لم يتم تثبيت نموذج .task محلي"
            )
        }

        // Validate model format and readability
        if (!modelFile.canRead() || sizeMb < 10f) {
            return GenerativeModelInfo(
                isInstalled = true,
                isValidated = false,
                modelFileName = modelFile.name,
                modelDirectory = modelsDir.absolutePath,
                modelSizeMb = sizeMb,
                statusMessage = "Invalid or incompatible AI model."
            )
        }

        return GenerativeModelInfo(
            isInstalled = true,
            isValidated = true,
            modelFileName = modelFile.name,
            modelDirectory = modelsDir.absolutePath,
            modelSizeMb = sizeMb,
            statusMessage = "النموذج متوافق وجاهز للاستخدام"
        )
    }

    fun getModelFile(): File? {
        val modelsDir = getModelsDir()
        var modelFile = File(modelsDir, defaultFileName)
        if (!modelFile.exists() || modelFile.length() <= 0) {
            val taskFiles = modelsDir.listFiles { _, name -> name.endsWith(".task") }
            if (!taskFiles.isNullOrEmpty()) {
                modelFile = taskFiles[0]
            }
        }
        return if (modelFile.exists() && modelFile.length() > 0 && modelFile.name.endsWith(".task")) modelFile else null
    }

    fun importModelFromUri(context: Context, uri: android.net.Uri): Result<String> {
        return try {
            val contentResolver = context.contentResolver

            // Check file name extension from URI if possible
            val cursor = contentResolver.query(uri, null, null, null, null)
            val displayName = cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && it.moveToFirst()) it.getString(nameIndex) else null
            } ?: ""

            if (displayName.isNotEmpty() && !displayName.endsWith(".task", ignoreCase = true)) {
                return Result.failure(IllegalArgumentException("Invalid or incompatible AI model. Only .task MediaPipe Task files are supported."))
            }

            val modelsDir = getModelsDir()
            val targetFileName = if (displayName.endsWith(".task", ignoreCase = true)) displayName else defaultFileName
            val targetFile = File(modelsDir, targetFileName)

            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalArgumentException("تعذر فتح ملف النموذج المحدد."))

            val tempFile = File(modelsDir, "temp_model_import.tmp")
            if (tempFile.exists()) tempFile.delete()

            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!tempFile.exists() || tempFile.length() < 10 * 1024 * 1024) { // Minimum 10MB check
                tempFile.delete()
                return Result.failure(IllegalArgumentException("Invalid or incompatible AI model."))
            }

            // Test loading model via LlmInference
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(tempFile.absolutePath)
                    .setMaxTokens(16)
                    .setResultListener { _, _ -> }
                    .build()
                val testInference = LlmInference.createFromOptions(context, options)
            } catch (e: Exception) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("Invalid or incompatible AI model: ${e.localizedMessage}"))
            }

            if (targetFile.exists()) targetFile.delete()
            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            refreshModelStatus()
            Result.success("تم استيراد نموذج MediaPipe Task بنجاح: ${targetFile.length() / (1024 * 1024)} ميجابايت")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun refreshModelStatus() {
        _modelInfo.value = checkModelStatus()
    }

    private fun getModelsDir(): File {
        val dir = File(context.filesDir, "models/llm")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}

