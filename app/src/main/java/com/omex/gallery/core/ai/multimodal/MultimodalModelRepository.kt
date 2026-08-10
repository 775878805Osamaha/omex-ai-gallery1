package com.omex.gallery.core.ai.multimodal

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class MultimodalModelInfo(
    val isInstalled: Boolean,
    val isValidated: Boolean = false,
    val modelFileName: String = "gemma-3n-e2b-it.litertlm",
    val modelDirectory: String = "",
    val modelSizeMb: Float = 0f,
    val statusMessage: String = ""
)

class MultimodalModelRepository(private val context: Context) {

    private val defaultFileName = "gemma-3n-e2b-it.litertlm"

    private val _modelInfo = MutableStateFlow(checkModelStatus())
    val modelInfo: StateFlow<MultimodalModelInfo> = _modelInfo.asStateFlow()

    fun checkModelStatus(): MultimodalModelInfo {
        val modelsDir = getModelsDir()
        var modelFile = File(modelsDir, defaultFileName)

        if (!modelFile.exists() || modelFile.length() <= 0) {
            val litertFiles = modelsDir.listFiles { _, name -> name.endsWith(".litertlm") }
            if (!litertFiles.isNullOrEmpty()) {
                modelFile = litertFiles[0]
            }
        }

        val exists = modelFile.exists() && modelFile.length() > 0 && modelFile.name.endsWith(".litertlm")
        val sizeMb = if (exists) modelFile.length() / (1024f * 1024f) else 0f

        if (!exists) {
            return MultimodalModelInfo(
                isInstalled = false,
                isValidated = false,
                modelFileName = defaultFileName,
                modelDirectory = modelsDir.absolutePath,
                modelSizeMb = 0f,
                statusMessage = "لم يتم تثبيت نموذج .litertlm محلي للرؤية"
            )
        }

        if (!modelFile.canRead() || sizeMb < 10f) {
            return MultimodalModelInfo(
                isInstalled = true,
                isValidated = false,
                modelFileName = modelFile.name,
                modelDirectory = modelsDir.absolutePath,
                modelSizeMb = sizeMb,
                statusMessage = "Invalid or incompatible LiteRT-LM model."
            )
        }

        return MultimodalModelInfo(
            isInstalled = true,
            isValidated = true,
            modelFileName = modelFile.name,
            modelDirectory = modelsDir.absolutePath,
            modelSizeMb = sizeMb,
            statusMessage = "نموذج الرؤية المتعدد الوسائط جاهز للاستخدام"
        )
    }

    fun refreshModelStatus() {
        _modelInfo.value = checkModelStatus()
    }

    fun getModelFile(): File? {
        val modelsDir = getModelsDir()
        var modelFile = File(modelsDir, defaultFileName)
        if (!modelFile.exists() || modelFile.length() <= 0) {
            val litertFiles = modelsDir.listFiles { _, name -> name.endsWith(".litertlm") }
            if (!litertFiles.isNullOrEmpty()) {
                modelFile = litertFiles[0]
            }
        }
        return if (modelFile.exists() && modelFile.length() > 0 && modelFile.name.endsWith(".litertlm")) modelFile else null
    }

    fun importModelFromUri(context: Context, uri: android.net.Uri): Result<String> {
        return try {
            val contentResolver = context.contentResolver

            val cursor = contentResolver.query(uri, null, null, null, null)
            val displayName = cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && it.moveToFirst()) it.getString(nameIndex) else null
            } ?: ""

            if (displayName.isNotEmpty() && !displayName.endsWith(".litertlm", ignoreCase = true)) {
                return Result.failure(IllegalArgumentException("Invalid or incompatible LiteRT-LM model. Only .litertlm files are supported."))
            }

            val modelsDir = getModelsDir()
            val targetFileName = if (displayName.endsWith(".litertlm", ignoreCase = true)) displayName else defaultFileName
            val targetFile = File(modelsDir, targetFileName)

            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalArgumentException("تعذر فتح ملف النموذج المحدد."))

            val tempFile = File(modelsDir, "temp_multimodal_import.tmp")
            if (tempFile.exists()) tempFile.delete()

            tempFile.outputStream().use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                }
            }

            if (!tempFile.exists() || tempFile.length() < 10 * 1024 * 1024) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("Invalid or incompatible LiteRT-LM model."))
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            refreshModelStatus()
            Result.success("تم استيراد نموذج LiteRT-LM (Gemma 3n) بنجاح: ${targetFile.length() / (1024 * 1024)} ميجابايت")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun removeModel(): Boolean {
        val file = getModelFile() ?: return false
        val deleted = file.delete()
        refreshModelStatus()
        return deleted
    }

    private fun getModelsDir(): File {
        val dir = File(context.filesDir, "litert_models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
