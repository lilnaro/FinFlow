package ru.lilnaro.finflow.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.lilnaro.finflow.domain.model.FinancialAiAnswerResult
import ru.lilnaro.finflow.domain.model.FinancialAiInitializationResult
import ru.lilnaro.finflow.domain.model.FinancialAiMessage
import ru.lilnaro.finflow.domain.model.FinancialAnalysisContext
import ru.lilnaro.finflow.domain.model.FinancialAnalyticsSnapshot

interface FinancialAiRepository {

    fun isModelAvailable(): Boolean

    fun getModelDownloadInfo():
            FinancialAiModelDownloadInfo

    fun observeModelDownloadState():
            Flow<FinancialAiModelDownloadState>

    suspend fun startModelDownload():
            FinancialAiModelDownloadStartResult

    suspend fun cancelModelDownload()

    suspend fun initialize():
            FinancialAiInitializationResult

    suspend fun ask(
        question: String,
        analysisContext: FinancialAnalysisContext,
        analyticsSnapshot: FinancialAnalyticsSnapshot,
        conversationHistory:
        List<FinancialAiMessage>,
    ): FinancialAiAnswerResult

    fun close()
}

data class FinancialAiModelDownloadInfo(
    val approximateDownloadBytes: Long,
    val minimumRequiredFreeSpaceBytes: Long,
)

sealed interface FinancialAiModelDownloadState {

    data object NotInstalled :
        FinancialAiModelDownloadState

    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long?,
    ) : FinancialAiModelDownloadState

    data object Installed :
        FinancialAiModelDownloadState

    data class Failed(
        val message: String,
    ) : FinancialAiModelDownloadState
}

sealed interface FinancialAiModelDownloadStartResult {

    data object Started :
        FinancialAiModelDownloadStartResult

    data object AlreadyRunning :
        FinancialAiModelDownloadStartResult

    data object AlreadyInstalled :
        FinancialAiModelDownloadStartResult

    data class NotEnoughSpace(
        val availableBytes: Long,
        val requiredBytes: Long,
    ) : FinancialAiModelDownloadStartResult

    data class Failure(
        val message: String,
    ) : FinancialAiModelDownloadStartResult
}