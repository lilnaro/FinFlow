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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.lilnaro.finflow.domain.model.FinancialMonth
import ru.lilnaro.finflow.domain.usecase.ObserveArchivedFinancialMonthsUseCase
import ru.lilnaro.finflow.presentation.archive.model.ArchiveAction
import ru.lilnaro.finflow.presentation.archive.model.ArchiveEffect
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthUiModel
import ru.lilnaro.finflow.presentation.archive.model.ArchiveUiState
import ru.lilnaro.finflow.presentation.archive.model.ArchiveUiStatus

class ArchiveViewModel(
    private val observeArchivedFinancialMonthsUseCase:
    ObserveArchivedFinancialMonthsUseCase,
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

        _uiState.value =
            ArchiveUiState(
                status =
                    ArchiveUiStatus.LOADING,
            )

        observationJob =
            viewModelScope.launch {
                observeArchivedFinancialMonthsUseCase()
                    .catch {
                        _uiState.value =
                            ArchiveUiState(
                                status =
                                    ArchiveUiStatus.ERROR,
                                errorMessage =
                                    "Не удалось загрузить архив месяцев.",
                            )
                    }
                    .collect { financialMonths ->
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