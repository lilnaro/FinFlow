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
import ru.lilnaro.finflow.domain.model.FinancialMonthReport
import ru.lilnaro.finflow.domain.usecase.ObserveFinancialMonthReportUseCase
import ru.lilnaro.finflow.presentation.archive.model.ArchiveCategoryBreakdownUiModel
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthDetailsAction
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthDetailsEffect
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthDetailsUiState
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthDetailsUiStatus
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthTransactionUiModel

class ArchiveMonthDetailsViewModel(
    private val observeFinancialMonthReportUseCase:
    ObserveFinancialMonthReportUseCase,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ArchiveMonthDetailsUiState(),
        )

    val uiState: StateFlow<ArchiveMonthDetailsUiState> =
        _uiState.asStateFlow()

    private val _effect =
        Channel<ArchiveMonthDetailsEffect>(
            capacity = Channel.BUFFERED,
        )

    val effect: Flow<ArchiveMonthDetailsEffect> =
        _effect.receiveAsFlow()

    private var observationJob: Job? = null
    private var requestedMonthId: Long? = null

    fun loadMonth(
        monthId: Long,
    ) {
        requestedMonthId = monthId

        observeMonthDetails(
            monthId = monthId,
        )
    }

    fun onAction(
        action: ArchiveMonthDetailsAction,
    ) {
        when (action) {
            ArchiveMonthDetailsAction.BackClicked -> {
                _effect.trySend(
                    ArchiveMonthDetailsEffect.NavigateBack,
                )
            }

            ArchiveMonthDetailsAction.RetryClicked -> {
                requestedMonthId?.let { monthId ->
                    observeMonthDetails(
                        monthId = monthId,
                    )
                }
            }
        }
    }

    private fun observeMonthDetails(
        monthId: Long,
    ) {
        observationJob?.cancel()

        if (monthId <= 0L) {
            showNotFound(
                monthId = monthId,
            )
            return
        }

        _uiState.value =
            ArchiveMonthDetailsUiState(
                status =
                    ArchiveMonthDetailsUiStatus.LOADING,
                monthId = monthId,
            )

        observationJob =
            viewModelScope.launch {
                observeFinancialMonthReportUseCase(
                    financialMonthId = monthId,
                )
                    .catch {
                        _uiState.value =
                            ArchiveMonthDetailsUiState(
                                status =
                                    ArchiveMonthDetailsUiStatus.ERROR,
                                monthId = monthId,
                                errorMessage =
                                    "Не удалось загрузить историю этого месяца.",
                            )
                    }
                    .collect { report ->
                        if (
                            report == null ||
                            !report.financialMonth.isClosed
                        ) {
                            showNotFound(
                                monthId = monthId,
                            )
                        } else {
                            showContent(
                                report = report,
                            )
                        }
                    }
            }
    }

    private fun showContent(
        report: FinancialMonthReport,
    ) {
        val financialMonth =
            report.financialMonth

        _uiState.value =
            ArchiveMonthDetailsUiState(
                status =
                    ArchiveMonthDetailsUiStatus.CONTENT,
                monthId = financialMonth.id,
                monthLabel =
                    createMonthLabel(
                        year = financialMonth.year,
                        monthNumber =
                            financialMonth.monthNumber,
                    ),
                initialBudget = report.initialBudget,
                totalIncome = report.totalIncome,
                totalExpense = report.totalExpense,
                finalBalance = report.finalBalance,
                transactionCount =
                    report.transactionCount,
                startedAtMillis =
                    financialMonth.startedAtMillis,
                closedAtMillis =
                    financialMonth.closedAtMillis,
                expenseBreakdown =
                    report.expenseBreakdown.map {
                            category ->
                        ArchiveCategoryBreakdownUiModel(
                            categoryId =
                                category.categoryId,
                            categoryName =
                                category.categoryName
                                    ?: UNKNOWN_CATEGORY_NAME,
                            amount = category.amount,
                            transactionCount =
                                category.transactionCount,
                            sharePercent =
                                category.sharePercent,
                        )
                    },
                incomeBreakdown =
                    report.incomeBreakdown.map {
                            category ->
                        ArchiveCategoryBreakdownUiModel(
                            categoryId =
                                category.categoryId,
                            categoryName =
                                category.categoryName
                                    ?: UNKNOWN_CATEGORY_NAME,
                            amount = category.amount,
                            transactionCount =
                                category.transactionCount,
                            sharePercent =
                                category.sharePercent,
                        )
                    },
                transactions =
                    report.transactions.map { transaction ->
                        ArchiveMonthTransactionUiModel(
                            id = transaction.id,
                            amount = transaction.amount,
                            type = transaction.type,
                            categoryName =
                                transaction.categoryName
                                    ?: UNKNOWN_CATEGORY_NAME,
                            note = transaction.note,
                            createdAtMillis =
                                transaction.createdAtMillis,
                        )
                    },
                errorMessage = null,
            )
    }

    private fun showNotFound(
        monthId: Long,
    ) {
        _uiState.value =
            ArchiveMonthDetailsUiState(
                status =
                    ArchiveMonthDetailsUiStatus.NOT_FOUND,
                monthId = monthId,
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

        const val UNKNOWN_CATEGORY_NAME =
            "Категория недоступна"

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