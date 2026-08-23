package ru.lilnaro.finflow.presentation.assistant.model

import java.math.BigDecimal

sealed interface AssistantAction {

    data object BackClicked : AssistantAction

    data object RetryClicked : AssistantAction

    data object CheckModelClicked : AssistantAction

    data object StartModelDownloadClicked :
        AssistantAction

    data object CancelModelDownloadClicked :
        AssistantAction

    data class QuestionChanged(
        val value: String,
    ) : AssistantAction

    data class QuickQuestionClicked(
        val question: String,
    ) : AssistantAction

    data object SendQuestionClicked :
        AssistantAction
}

sealed interface AssistantEffect {

    data object NavigateBack :
        AssistantEffect
}

data class AssistantUiState(
    val status: AssistantUiStatus =
        AssistantUiStatus.LOADING,
    val monthLabel: String = "",
    val isFocusClosed: Boolean = false,
    val pace: AssistantPaceUiModel? = null,
    val questionInput: String = "",
    val showQuickQuestions: Boolean = true,
    val messages:
    List<AssistantMessageUiModel> =
        emptyList(),
    val modelStatus:
    AssistantModelStatus =
        AssistantModelStatus.CHECKING,
    val expectedModelFileName: String? = null,
    val modelErrorMessage: String? = null,
    val modelDownloadStatus:
    AssistantModelDownloadStatus =
        AssistantModelDownloadStatus.IDLE,
    val modelDownloadedBytes: Long = 0L,
    val modelDownloadTotalBytes: Long? = null,
    val approximateModelDownloadBytes: Long = 0L,
    val minimumRequiredFreeSpaceBytes: Long = 0L,
    val modelDownloadErrorMessage: String? = null,
    val isGenerating: Boolean = false,
    val chatErrorMessage: String? = null,
    val errorMessage: String? = null,
) {
    val isModelReady: Boolean
        get() {
            return modelStatus ==
                    AssistantModelStatus.READY
        }

    val canSend: Boolean
        get() {
            return isModelReady &&
                    questionInput.isNotBlank() &&
                    !isGenerating
        }
}

enum class AssistantUiStatus {
    LOADING,
    EMPTY,
    CONTENT,
    ERROR,
}

enum class AssistantModelStatus {
    CHECKING,
    LOADING,
    MISSING,
    READY,
    ERROR,
}

enum class AssistantModelDownloadStatus {
    IDLE,
    DOWNLOADING,
    FAILED,
}

data class AssistantPaceUiModel(
    val elapsedDays: Int,
    val totalPeriodDays: Int,
    val remainingDays: Int,
    val averageDailyIncome: BigDecimal,
    val averageDailyExpense: BigDecimal,
    val forecastStatus:
    AssistantForecastStatus,
    val projectedIncome: BigDecimal?,
    val projectedExpense: BigDecimal?,
    val projectedFinalBalance: BigDecimal?,
)

enum class AssistantForecastStatus {
    AVAILABLE,
    INSUFFICIENT_DATA,
}

data class AssistantMessageUiModel(
    val id: Long,
    val role: AssistantMessageRole,
    val text: String,
)

enum class AssistantMessageRole {
    USER,
    ASSISTANT,
}