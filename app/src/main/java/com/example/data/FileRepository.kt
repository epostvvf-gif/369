package com.example.data

import com.example.api.*
import kotlinx.coroutines.flow.Flow

class FileRepository(private val fileDao: FileDao) {

    val normalFiles: Flow<List<FileEntity>> = fileDao.getNormalFiles()
    val safeFiles: Flow<List<FileEntity>> = fileDao.getSafeFiles()
    val junkFiles: Flow<List<FileEntity>> = fileDao.getJunkFiles()
    val allFiles: Flow<List<FileEntity>> = fileDao.getAllFiles()
    val allTags: Flow<List<FileTagEntity>> = fileDao.getAllTagsFlow()

    fun getNormalFilesByNameLike(query: String): Flow<List<FileEntity>> = fileDao.getNormalFilesByNameLike(query)

    suspend fun insertFile(file: FileEntity) = fileDao.insertFile(file)
    suspend fun insertFiles(files: List<FileEntity>) = fileDao.insertFiles(files)
    suspend fun deleteFiles(files: List<FileEntity>) = fileDao.deleteFiles(files)
    suspend fun deleteFileById(id: Int) = fileDao.deleteFileById(id)
    suspend fun updateSafeStatus(ids: List<Int>, isSafe: Boolean) = fileDao.updateSafeStatus(ids, isSafe)
    suspend fun updateFilesCategory(ids: List<Int>, category: String) = fileDao.updateFilesCategory(ids, category)
    suspend fun clearAllJunk() = fileDao.clearAllJunk()

    // Tag management
    suspend fun insertTag(tag: FileTagEntity) = fileDao.insertTag(tag)
    suspend fun deleteTag(fileId: String, tag: String) = fileDao.deleteTag(fileId, tag)
    suspend fun deleteTagsForFile(fileId: String) = fileDao.deleteTagsForFile(fileId)

    // PIN management
    suspend fun getSafePin(): SafePinEntity? = fileDao.getSafePin()
    suspend fun setSafePin(pin: String) = fileDao.setSafePin(SafePinEntity(pin = pin, isRegistered = true))
    suspend fun deleteSafePin() = fileDao.deleteSafePin()

    // Gemini API Query
    suspend fun askGemini(
        prompt: String,
        customKey: String?,
        useHighThinking: Boolean,
        history: List<Content> = emptyList()
    ): String {
        val activeKey = if (!customKey.isNullOrBlank()) {
            customKey
        } else {
            com.example.BuildConfig.GEMINI_API_KEY
        }

        if (activeKey.isBlank() || activeKey == "MY_GEMINI_API_KEY" || activeKey == "null") {
            return "Please provide a valid Gemini API Key in the 'AI Assistant' setup panel, or configure it via AI Studio platform secrets."
        }

        val currentContent = Content(parts = listOf(Part(text = prompt)))
        val allContents = history + currentContent

        val request = if (useHighThinking) {
            GenerateContentRequest(
                contents = allContents,
                generationConfig = GenerationConfig(
                    thinkingConfig = ThinkingConfig(thinkingLevel = "HIGH")
                )
            )
        } else {
            GenerateContentRequest(
                contents = allContents
            )
        }

        return try {
            val response = if (useHighThinking) {
                RetrofitClient.service.generateProContent(activeKey, request)
            } else {
                RetrofitClient.service.generateFlashContent(activeKey, request)
            }
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No response received from Gemini."
        } catch (e: Exception) {
            "API Failure: ${e.localizedMessage ?: e.message}"
        }
    }
}
