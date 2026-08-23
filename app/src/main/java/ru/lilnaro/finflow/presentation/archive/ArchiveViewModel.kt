package ru.lilnaro.finflow.presentation.archive

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
import ru.lilnaro.finflow.domain.model.FinancialMonth
import ru.lilnaro.finflow.domain.model.result.CloseFinancialMonthResult
import ru.lilnaro.finflow.domain.usecase.CloseFinancialMonthUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveActiveFinancialMonthUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveArchivedFinancialMonthsUseCase
import ru.lilnaro.finflow.presentation.archive.model.ArchiveAction
import ru.lilnaro.finflow.presentation.archive.model.ArchiveEffect
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthUiModel
import ru.lilnaro.finflow.presentation.archive.model.ArchiveUiState
import ru.lilnaro.finflow.presentation.archive.model.ArchiveUiStatus

class ArchiveViewModel(
    private val observeArchivedFinancialMonthsUseCase:
    ObserveArchivedFinancialMonthsUseCase,
    private val observeActiveFinancialMonthUseCase:
    ObserveActiveFinancialMonthUseCase,
    private val closeFinancialMonthUseCase:
    CloseFinancialMonthUseCase,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ArchiveUiState(),
        )

    val uiState: StateFlow<ArchiveUiState> =
        _uiState.asStateFlow()

    private val _effect =
        Channel<ArchiveEffect>(
            capacity = Channel.BUFFERED,
        )

    val effect: Flow<ArchiveEffect> =
        _effect.receiveAsFlow()

    private var observationJob: Job? = null

    private var activeFinancialMonthId: Long? =
        null

    init {
        observeArchive()
    }

    fun onAction(
        action: ArchiveAction,
    ) {
        when (action) {
            ArchiveAction.BackClicked -> {
                _effect.trySend(
                    ArchiveEffect.NavigateBack,
                )
            }

            ArchiveAction.NewMonthClicked -> {
                handleNewMonthClick()
            }

            ArchiveAction.CloseMonthConfirmed -> {
                closeActiveFinancialMonth()
            }

            ArchiveAction.CloseMonthCancelled -> {
                cancelMonthClosing()
            }

            ArchiveAction.RetryClicked -> {
                observeArchive()
            }

            is ArchiveAction.MonthClicked -> {
                openMonthDetails(
                    monthId = action.monthId,
                )
            }
        }
    }

    private fun handleNewMonthClick() {
        val currentState =
            _uiState.value

        if (currentState.isClosingMonth) {
            return
        }

        if (activeFinancialMonthId == null) {
            _effect.trySend(
                ArchiveEffect.NavigateToNewMonth,
            )

            return
        }

        _uiState.value =
            currentState.copy(
                isCloseMonthConfirmationVisible = true,
                closeMonthErrorMessage = null,
            )
    }

    private fun cancelMonthClosing() {
        val currentState =
            _uiState.value

        if (currentState.isClosingMonth) {
            return
        }

        _uiState.value =
            currentState.copy(
                isCloseMonthConfirmationVisible = false,
                closeMonthErrorMessage = null,
            )
    }

    private fun closeActiveFinancialMonth() {
        val currentState =
            _uiState.value

        if (
            currentState.isClosingMonth ||
            !currentState
                .isCloseMonthConfirmationVisible
        ) {
            return
        }

        val financialMonthId =
            activeFinancialMonthId

        if (financialMonthId == null) {
            _uiState.value =
                currentState.copy(
                    isCloseMonthConfirmationVisible = false,
                    isClosingMonth = false,
                    closeMonthErrorMessage = null,
                )

            _effect.trySend(
                ArchiveEffect.NavigateToNewMonth,
            )

            return
        }

        _uiState.value =
            currentState.copy(
                isClosingMonth = true,
                closeMonthErrorMessage = null,
            )

        viewModelScope.launch {
            val result =
                try {
                    closeFinancialMonthUseCase(
                        financialMonthId =
                            financialMonthId,
                        closedAtMillis =
                            System.currentTimeMillis(),
                    )
                } catch (_: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isClosingMonth = false,
                            closeMonthErrorMessage =
                                "Не удалось завершить финансовый месяц.",
                        )

                    return@launch
                }

            handleCloseMonthResult(
                result = result,
            )
        }
    }

    private suspend fun handleCloseMonthResult(
        result: CloseFinancialMonthResult,
    ) {
        when (result) {
            is CloseFinancialMonthResult.Success,
            is CloseFinancialMonthResult
            .FinancialMonthAlreadyClosed,
            is CloseFinancialMonthResult
            .FinancialMonthNotFound -> {
                activeFinancialMonthId = null

                _uiState.value =
                    _uiState.value.copy(
                        isCloseMonthConfirmationVisible = false,
                        isClosingMonth = false,
                        closeMonthErrorMessage = null,
                    )

                _effect.send(
                    ArchiveEffect.NavigateToNewMonth,
                )
            }

            is CloseFinancialMonthResult
            .InvalidFinancialMonthId -> {
                showCloseMonthError(
                    message =
                        "Не удалось определить финансовый месяц.",
                )
            }

            is CloseFinancialMonthResult
            .InvalidClosedAtMillis -> {
                showCloseMonthError(
                    message =
                        "Не удалось определить время завершения месяца.",
                )
            }

            is CloseFinancialMonthResult
            .ClosingTimeBeforeMonthStart -> {
                showCloseMonthError(
                    message =
                        "Проверьте дату и время устройства: время завершения месяца некорректно.",
                )
            }
        }
    }

    private fun showCloseMonthError(
        message: String,
    ) {
        _uiState.value =
            _uiState.value.copy(
                isClosingMonth = false,
                closeMonthErrorMessage = message,
            )
    }

    private fun openMonthDetails(
        monthId: Long,
    ) {
        if (monthId <= 0L) {
            return
        }

        val monthExists =
            _uiState.value.months.any { month ->
                month.id == monthId
            }

        if (!monthExists) {
            return
        }

        _effect.trySend(
            ArchiveEffect.NavigateToMonthDetails(
                monthId = monthId,
            ),
        )
    }

    private fun observeArchive() {
        observationJob?.cancel()

        activeFinancialMonthId = null

        _uiState.value =
            ArchiveUiState(
                status =
                    ArchiveUiStatus.LOADING,
            )

        observationJob =
            viewModelScope.launch {
                combine(
                    observeArchivedFinancialMonthsUseCase(),
                    observeActiveFinancialMonthUseCase(),
                ) { archivedFinancialMonths, activeFinancialMonth ->
                    archivedFinancialMonths to
                            activeFinancialMonth
                }
                    .catch {
                        activeFinancialMonthId = null

                        _uiState.value =
                            ArchiveUiState(
                                status =
                                    ArchiveUiStatus.ERROR,
                                errorMessage =
                                    "Не удалось загрузить архив месяцев.",
                            )
                    }
                    .collect {
                            (financialMonths, activeFinancialMonth) ->
                        activeFinancialMonthId =
                            activeFinancialMonth?.id

                        val previousState =
                            _uiState.value

                        val archivedMonths =
                            financialMonths
                                .sortedWith(
                                    compareByDescending<
                                            FinancialMonth
                                            > { financialMonth ->
                                        financialMonth.year
                                    }.thenByDescending {
                                            financialMonth ->
                                        financialMonth.monthNumber
                                    },
                                )
                                .map { financialMonth ->
                                    financialMonth.toUiModel()
                                }

                        _uiState.value =
                            ArchiveUiState(
                                status =
                                    if (
                                        archivedMonths.isEmpty()
                                    ) {
                                        ArchiveUiStatus.EMPTY
                                    } else {
                                        ArchiveUiStatus.CONTENT
                                    },
                                months = archivedMonths,
                                isCloseMonthConfirmationVisible =
                                    previousState
                                        .isCloseMonthConfirmationVisible,
                                isClosingMonth =
                                    previousState
                                        .isClosingMonth,
                                closeMonthErrorMessage =
                                    previousState
                                        .closeMonthErrorMessage,
                                errorMessage = null,
                            )
                    }
            }
    }

    private fun FinancialMonth.toUiModel():
            ArchiveMonthUiModel {
        return ArchiveMonthUiModel(
            id = id,
            monthLabel =
                createMonthLabel(
                    year = year,
                    monthNumber = monthNumber,
                ),
            initialBudget = initialBudget,
            startedAtMillis = startedAtMillis,
            closedAtMillis = closedAtMillis,
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