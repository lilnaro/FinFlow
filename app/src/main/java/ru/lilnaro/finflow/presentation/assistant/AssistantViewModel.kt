package ru.lilnaro.finflow.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.lilnaro.finflow.domain.model.FinancialAiAnswerResult
import ru.lilnaro.finflow.domain.model.FinancialAiInitializationResult
import ru.lilnaro.finflow.domain.model.FinancialAiMessage
import ru.lilnaro.finflow.domain.model.FinancialAiMessageRole
import ru.lilnaro.finflow.domain.model.FinancialAnalysisContext
import ru.lilnaro.finflow.domain.model.FinancialAnalyticsSnapshot
import ru.lilnaro.finflow.domain.model.FinancialForecastStatus
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadStartResult
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadState
import ru.lilnaro.finflow.domain.usecase.AskFinancialAssistantUseCase
import ru.lilnaro.finflow.domain.usecase.CancelFinancialAiModelDownloadUseCase
import ru.lilnaro.finflow.domain.usecase.GetFinancialAiModelDownloadInfoUseCase
import ru.lilnaro.finflow.domain.usecase.InitializeFinancialAiUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveActiveFinancialMonthUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveFinancialAiModelDownloadUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveFinancialAnalysisContextUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveFinancialAnalyticsUseCase
import ru.lilnaro.finflow.domain.usecase.StartFinancialAiModelDownloadUseCase
import ru.lilnaro.finflow.presentation.assistant.model.AssistantAction
import ru.lilnaro.finflow.presentation.assistant.model.AssistantEffect
import ru.lilnaro.finflow.presentation.assistant.model.AssistantForecastStatus
import ru.lilnaro.finflow.presentation.assistant.model.AssistantMessageRole
import ru.lilnaro.finflow.presentation.assistant.model.AssistantMessageUiModel
import ru.lilnaro.finflow.presentation.assistant.model.AssistantModelDownloadStatus
import ru.lilnaro.finflow.presentation.assistant.model.AssistantModelStatus
import ru.lilnaro.finflow.presentation.assistant.model.AssistantPaceUiModel
import ru.lilnaro.finflow.presentation.assistant.model.AssistantUiState
import ru.lilnaro.finflow.presentation.assistant.model.AssistantUiStatus

class AssistantViewModel(
    private val observeActiveFinancialMonthUseCase:
    ObserveActiveFinancialMonthUseCase,
    private val observeFinancialAnalysisContextUseCase:
    ObserveFinancialAnalysisContextUseCase,
    private val observeFinancialAnalyticsUseCase:
    ObserveFinancialAnalyticsUseCase,
    private val initializeFinancialAiUseCase:
    InitializeFinancialAiUseCase,
    private val askFinancialAssistantUseCase:
    AskFinancialAssistantUseCase,
    private val getFinancialAiModelDownloadInfoUseCase:
    GetFinancialAiModelDownloadInfoUseCase,
    private val observeFinancialAiModelDownloadUseCase:
    ObserveFinancialAiModelDownloadUseCase,
    private val startFinancialAiModelDownloadUseCase:
    StartFinancialAiModelDownloadUseCase,
    private val cancelFinancialAiModelDownloadUseCase:
    CancelFinancialAiModelDownloadUseCase,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            AssistantUiState(),
        )

    val uiState: StateFlow<AssistantUiState> =
        _uiState.asStateFlow()

    private val _effect =
        Channel<AssistantEffect>(
            capacity = Channel.BUFFERED,
        )

    val effect: Flow<AssistantEffect> =
        _effect.receiveAsFlow()

    private var observationJob: Job? = null
    private var modelJob: Job? = null
    private var downloadObservationJob: Job? = null
    private var downloadActionJob: Job? = null
    private var generationJob: Job? = null

    private var latestAnalysisContext:
            FinancialAnalysisContext? = null

    private var latestAnalytics:
            FinancialAnalyticsSnapshot? = null

    private var nextMessageId =
        1L

    init {
        val downloadInfo =
            getFinancialAiModelDownloadInfoUseCase()

        _uiState.value =
            _uiState.value.copy(
                approximateModelDownloadBytes =
                    downloadInfo
                        .approximateDownloadBytes,
                minimumRequiredFreeSpaceBytes =
                    downloadInfo
                        .minimumRequiredFreeSpaceBytes,
            )

        observeFinancialData()
        observeModelDownload()
    }

    fun onAction(
        action: AssistantAction,
    ) {
        when (action) {
            AssistantAction.BackClicked -> {
                _effect.trySend(
                    AssistantEffect.NavigateBack,
                )
            }

            AssistantAction.RetryClicked -> {
                observeFinancialData()
                observeModelDownload()
            }

            AssistantAction.CheckModelClicked -> {
                observeModelDownload()
            }

            AssistantAction.StartModelDownloadClicked -> {
                startModelDownload()
            }

            AssistantAction.CancelModelDownloadClicked -> {
                cancelModelDownload()
            }

            is AssistantAction.QuestionChanged -> {
                _uiState.value =
                    _uiState.value.copy(
                        questionInput =
                            action.value.take(
                                MAX_QUESTION_LENGTH,
                            ),
                        chatErrorMessage = null,
                    )
            }

            is AssistantAction.QuickQuestionClicked -> {
                _uiState.value =
                    _uiState.value.copy(
                        questionInput =
                            action.question.take(
                                MAX_QUESTION_LENGTH,
                            ),
                        showQuickQuestions = false,
                        chatErrorMessage = null,
                    )
            }

            AssistantAction.SendQuestionClicked -> {
                sendQuestion()
            }
        }
    }

    private fun observeFinancialData() {
        observationJob?.cancel()

        _uiState.value =
            _uiState.value.copy(
                status =
                    AssistantUiStatus.LOADING,
                errorMessage = null,
            )

        val nowMillis =
            System.currentTimeMillis()

        observationJob =
            viewModelScope.launch {
                combine(
                    observeActiveFinancialMonthUseCase(),
                    observeFinancialAnalysisContextUseCase(),
                    observeFinancialAnalyticsUseCase(
                        nowMillis = nowMillis,
                    ),
                ) {
                        activeMonth,
                        analysisContext:
                        FinancialAnalysisContext,
                        analyticsSnapshot:
                        FinancialAnalyticsSnapshot,
                    ->
                    Triple(
                        activeMonth,
                        analysisContext,
                        analyticsSnapshot,
                    )
                }
                    .catch {
                        _uiState.value =
                            _uiState.value.copy(
                                status =
                                    AssistantUiStatus.ERROR,
                                errorMessage =
                                    "Не удалось подготовить финансовый контекст.",
                            )
                    }
                    .collect {
                            (
                                activeMonth,
                                analysisContext,
                                analyticsSnapshot,
                            ),
                        ->
                        if (activeMonth == null) {
                            latestAnalysisContext = null
                            latestAnalytics = null

                            generationJob?.cancel()

                            _uiState.value =
                                _uiState.value.copy(
                                    status =
                                        AssistantUiStatus.EMPTY,
                                    monthLabel = "",
                                    isFocusClosed = false,
                                    pace = null,
                                    questionInput = "",
                                    messages =
                                        emptyList(),
                                    isGenerating = false,
                                    chatErrorMessage = null,
                                    errorMessage = null,
                                )

                            return@collect
                        }

                        latestAnalysisContext =
                            analysisContext

                        latestAnalytics =
                            analyticsSnapshot

                        val focusMonth =
                            analyticsSnapshot
                                .focusMonth

                        if (focusMonth == null) {
                            _uiState.value =
                                _uiState.value.copy(
                                    status =
                                        AssistantUiStatus.EMPTY,
                                    monthLabel = "",
                                    pace = null,
                                )

                            return@collect
                        }

                        _uiState.value =
                            _uiState.value.copy(
                                status =
                                    AssistantUiStatus.CONTENT,
                                monthLabel =
                                    createMonthLabel(
                                        year =
                                            focusMonth.year,
                                        monthNumber =
                                            focusMonth
                                                .monthNumber,
                                    ),
                                isFocusClosed =
                                    focusMonth.isClosed,
                                pace =
                                    focusMonth.pace
                                        ?.let { pace ->
                                            AssistantPaceUiModel(
                                                elapsedDays =
                                                    pace.elapsedDays,
                                                totalPeriodDays =
                                                    pace.totalPeriodDays,
                                                remainingDays =
                                                    pace.remainingDays,
                                                averageDailyIncome =
                                                    pace.averageDailyIncome,
                                                averageDailyExpense =
                                                    pace.averageDailyExpense,
                                                forecastStatus =
                                                    when (
                                                        pace.forecastStatus
                                                    ) {
                                                        FinancialForecastStatus.AVAILABLE -> {
                                                            AssistantForecastStatus.AVAILABLE
                                                        }

                                                        FinancialForecastStatus.INSUFFICIENT_DATA -> {
                                                            AssistantForecastStatus.INSUFFICIENT_DATA
                                                        }
                                                    },
                                                projectedIncome =
                                                    pace.projectedIncome,
                                                projectedExpense =
                                                    pace.projectedExpense,
                                                projectedFinalBalance =
                                                    pace.projectedFinalBalance,
                                            )
                                        },
                                errorMessage = null,
                            )
                    }
            }
    }

    private fun observeModelDownload() {
        downloadObservationJob?.cancel()

        downloadObservationJob =
            viewModelScope.launch {
                observeFinancialAiModelDownloadUseCase()
                    .catch {
                        _uiState.value =
                            _uiState.value.copy(
                                modelStatus =
                                    AssistantModelStatus.MISSING,
                                modelDownloadStatus =
                                    AssistantModelDownloadStatus.FAILED,
                                modelDownloadErrorMessage =
                                    "Не удалось проверить состояние локального AI-модуля.",
                            )
                    }
                    .collect { state ->
                        when (state) {
                            FinancialAiModelDownloadState.NotInstalled -> {
                                _uiState.value =
                                    _uiState.value.copy(
                                        modelStatus =
                                            AssistantModelStatus.MISSING,
                                        modelDownloadStatus =
                                            AssistantModelDownloadStatus.IDLE,
                                        modelDownloadedBytes = 0L,
                                        modelDownloadTotalBytes = null,
                                        modelDownloadErrorMessage = null,
                                        modelErrorMessage = null,
                                    )
                            }

                            is FinancialAiModelDownloadState.Downloading -> {
                                _uiState.value =
                                    _uiState.value.copy(
                                        modelStatus =
                                            AssistantModelStatus.MISSING,
                                        modelDownloadStatus =
                                            AssistantModelDownloadStatus.DOWNLOADING,
                                        modelDownloadedBytes =
                                            state.downloadedBytes,
                                        modelDownloadTotalBytes =
                                            state.totalBytes,
                                        modelDownloadErrorMessage = null,
                                        modelErrorMessage = null,
                                    )
                            }

                            FinancialAiModelDownloadState.Installed -> {
                                _uiState.value =
                                    _uiState.value.copy(
                                        modelDownloadStatus =
                                            AssistantModelDownloadStatus.IDLE,
                                        modelDownloadErrorMessage = null,
                                    )

                                initializeModel()
                            }

                            is FinancialAiModelDownloadState.Failed -> {
                                _uiState.value =
                                    _uiState.value.copy(
                                        modelStatus =
                                            AssistantModelStatus.MISSING,
                                        modelDownloadStatus =
                                            AssistantModelDownloadStatus.FAILED,
                                        modelDownloadErrorMessage =
                                            state.message,
                                    )
                            }
                        }
                    }
            }
    }

    private fun startModelDownload() {
        downloadActionJob?.cancel()

        _uiState.value =
            _uiState.value.copy(
                modelDownloadErrorMessage = null,
            )

        downloadActionJob =
            viewModelScope.launch {
                when (
                    val result =
                        startFinancialAiModelDownloadUseCase()
                ) {
                    FinancialAiModelDownloadStartResult.Started,
                    FinancialAiModelDownloadStartResult.AlreadyRunning,
                        -> {
                        _uiState.value =
                            _uiState.value.copy(
                                modelStatus =
                                    AssistantModelStatus.MISSING,
                                modelDownloadStatus =
                                    AssistantModelDownloadStatus.DOWNLOADING,
                            )

                        observeModelDownload()
                    }

                    FinancialAiModelDownloadStartResult.AlreadyInstalled -> {
                        initializeModel()
                    }

                    is FinancialAiModelDownloadStartResult.NotEnoughSpace -> {
                        _uiState.value =
                            _uiState.value.copy(
                                modelStatus =
                                    AssistantModelStatus.MISSING,
                                modelDownloadStatus =
                                    AssistantModelDownloadStatus.FAILED,
                                modelDownloadErrorMessage =
                                    "Недостаточно свободного места для установки локального AI-модуля.",
                            )
                    }

                    is FinancialAiModelDownloadStartResult.Failure -> {
                        _uiState.value =
                            _uiState.value.copy(
                                modelStatus =
                                    AssistantModelStatus.MISSING,
                                modelDownloadStatus =
                                    AssistantModelDownloadStatus.FAILED,
                                modelDownloadErrorMessage =
                                    result.message,
                            )
                    }
                }
            }
    }

    private fun cancelModelDownload() {
        downloadActionJob?.cancel()

        downloadActionJob =
            viewModelScope.launch {
                cancelFinancialAiModelDownloadUseCase()

                _uiState.value =
                    _uiState.value.copy(
                        modelStatus =
                            AssistantModelStatus.MISSING,
                        modelDownloadStatus =
                            AssistantModelDownloadStatus.IDLE,
                        modelDownloadedBytes = 0L,
                        modelDownloadTotalBytes = null,
                        modelDownloadErrorMessage = null,
                    )

                observeModelDownload()
            }
    }

    private fun initializeModel() {
        modelJob?.cancel()

        _uiState.value =
            _uiState.value.copy(
                modelStatus =
                    AssistantModelStatus.LOADING,
                expectedModelFileName = null,
                modelErrorMessage = null,
                modelDownloadStatus =
                    AssistantModelDownloadStatus.IDLE,
            )

        modelJob =
            viewModelScope.launch {
                when (
                    val result =
                        initializeFinancialAiUseCase()
                ) {
                    FinancialAiInitializationResult.Ready -> {
                        _uiState.value =
                            _uiState.value.copy(
                                modelStatus =
                                    AssistantModelStatus.READY,
                                expectedModelFileName =
                                    null,
                                modelErrorMessage =
                                    null,
                            )
                    }

                    is FinancialAiInitializationResult.ModelMissing -> {
                        _uiState.value =
                            _uiState.value.copy(
                                modelStatus =
                                    AssistantModelStatus.MISSING,
                                expectedModelFileName =
                                    result.expectedFileName,
                                modelErrorMessage =
                                    null,
                            )
                    }

                    is FinancialAiInitializationResult.Failure -> {
                        _uiState.value =
                            _uiState.value.copy(
                                modelStatus =
                                    AssistantModelStatus.ERROR,
                                expectedModelFileName =
                                    null,
                                modelErrorMessage =
                                    result.message,
                            )
                    }
                }
            }
    }

    private fun sendQuestion() {
        val currentState =
            _uiState.value

        if (!currentState.canSend) {
            return
        }

        val analysisContext =
            latestAnalysisContext
                ?: return

        val analyticsSnapshot =
            latestAnalytics
                ?: return

        val question =
            currentState.questionInput
                .trim()

        if (question.isEmpty()) {
            return
        }

        val conversationHistory =
            currentState.messages
                .map { message ->
                    message.toDomain()
                }

        val userMessage =
            AssistantMessageUiModel(
                id = nextMessageId++,
                role =
                    AssistantMessageRole.USER,
                text = question,
            )

        _uiState.value =
            currentState.copy(
                questionInput = "",
                showQuickQuestions = false,
                messages =
                    (
                            currentState.messages +
                                    userMessage
                            )
                        .takeLast(
                            MAX_UI_MESSAGES,
                        ),
                isGenerating = true,
                chatErrorMessage = null,
            )

        generationJob?.cancel()

        generationJob =
            viewModelScope.launch {
                when (
                    val result =
                        askFinancialAssistantUseCase(
                            question = question,
                            analysisContext =
                                analysisContext,
                            analyticsSnapshot =
                                analyticsSnapshot,
                            conversationHistory =
                                conversationHistory,
                        )
                ) {
                    is FinancialAiAnswerResult.Success -> {
                        val assistantMessage =
                            AssistantMessageUiModel(
                                id = nextMessageId++,
                                role =
                                    AssistantMessageRole.ASSISTANT,
                                text =
                                    result.answer.trim(),
                            )

                        _uiState.value =
                            _uiState.value.copy(
                                messages =
                                    (
                                            _uiState
                                                .value
                                                .messages +
                                                    assistantMessage
                                            )
                                        .takeLast(
                                            MAX_UI_MESSAGES,
                                        ),
                                isGenerating = false,
                                chatErrorMessage =
                                    null,
                            )
                    }

                    is FinancialAiAnswerResult.Failure -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isGenerating = false,
                                chatErrorMessage =
                                    result.message,
                            )
                    }
                }
            }
    }

    private fun AssistantMessageUiModel.toDomain():
            FinancialAiMessage {
        return FinancialAiMessage(
            role =
                when (role) {
                    AssistantMessageRole.USER -> {
                        FinancialAiMessageRole.USER
                    }

                    AssistantMessageRole.ASSISTANT -> {
                        FinancialAiMessageRole.ASSISTANT
                    }
                },
            text = text,
        )
    }

    private fun createMonthLabel(
        year: Int,
        monthNumber: Int,
    ): String {
        val monthName =
            MONTH_NAMES.getOrElse(
                index = monthNumber - 1,
            ) {
                "Месяц $monthNumber"
            }

        return "$monthName $year"
    }

    private companion object {

        const val MAX_QUESTION_LENGTH =
            500

        const val MAX_UI_MESSAGES =
            12

        val MONTH_NAMES = listOf(
            "Январь",
            "Февраль",
            "Март",
            "Апрель",
            "Май",
            "Июнь",
            "Июль",
            "Август",
            "Сентябрь",
            "Октябрь",
            "Ноябрь",
            "Декабрь",
        )
    }
}