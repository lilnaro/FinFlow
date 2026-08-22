package ru.lilnaro.finflow.presentation.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.math.BigDecimal
import java.math.MathContext
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
import ru.lilnaro.finflow.domain.model.Transaction
import ru.lilnaro.finflow.domain.model.TransactionCategory
import ru.lilnaro.finflow.domain.model.TransactionType
import ru.lilnaro.finflow.domain.usecase.CalculateMonthSummaryUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveArchivedFinancialMonthsUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveCategoriesByTypeUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveTransactionsByMonthUseCase
import ru.lilnaro.finflow.presentation.archive.model.ArchiveExpenseCategoryUiModel
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthDetailsAction
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthDetailsEffect
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthDetailsUiState
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthDetailsUiStatus
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthTransactionUiModel

class ArchiveMonthDetailsViewModel(
    private val observeArchivedFinancialMonthsUseCase:
    ObserveArchivedFinancialMonthsUseCase,
    private val observeTransactionsByMonthUseCase:
    ObserveTransactionsByMonthUseCase,
    private val observeCategoriesByTypeUseCase:
    ObserveCategoriesByTypeUseCase,
    private val calculateMonthSummaryUseCase:
    CalculateMonthSummaryUseCase,
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
            _uiState.value =
                ArchiveMonthDetailsUiState(
                    status =
                        ArchiveMonthDetailsUiStatus.NOT_FOUND,
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
                combine(
                    observeArchivedFinancialMonthsUseCase(),
                    observeTransactionsByMonthUseCase(
                        financialMonthId = monthId,
                    ),
                    observeCategoriesByTypeUseCase(
                        type = TransactionType.EXPENSE,
                    ),
                    observeCategoriesByTypeUseCase(
                        type = TransactionType.INCOME,
                    ),
                ) {
                        archivedMonths,
                        transactions,
                        expenseCategories,
                        incomeCategories,
                    ->
                    ArchiveMonthDetailsSnapshot(
                        financialMonth =
                            archivedMonths.firstOrNull {
                                    financialMonth ->
                                financialMonth.id == monthId
                            },
                        transactions = transactions,
                        categories =
                            expenseCategories + incomeCategories,
                    )
                }
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
                    .collect { snapshot ->
                        updateUiState(
                            monthId = monthId,
                            snapshot = snapshot,
                        )
                    }
            }
    }

    private fun updateUiState(
        monthId: Long,
        snapshot: ArchiveMonthDetailsSnapshot,
    ) {
        val financialMonth = snapshot.financialMonth

        if (financialMonth == null) {
            _uiState.value =
                ArchiveMonthDetailsUiState(
                    status =
                        ArchiveMonthDetailsUiStatus.NOT_FOUND,
                    monthId = monthId,
                )
            return
        }

        val monthSummary =
            calculateMonthSummaryUseCase(
                financialMonth = financialMonth,
                transactions = snapshot.transactions,
            )

        val finalBalance =
            monthSummary.initialBudget
                .add(monthSummary.totalIncome)
                .subtract(monthSummary.totalExpense)

        val categoriesById =
            snapshot.categories.associateBy { category ->
                category.id
            }

        val transactionUiModels =
            snapshot.transactions
                .sortedByDescending { transaction ->
                    transaction.createdAtMillis
                }
                .map { transaction ->
                    transaction.toUiModel(
                        categoriesById = categoriesById,
                    )
                }

        val expenseBreakdown =
            createExpenseBreakdown(
                transactions = snapshot.transactions,
                categoriesById = categoriesById,
                totalExpense = monthSummary.totalExpense,
            )

        _uiState.value =
            ArchiveMonthDetailsUiState(
                status =
                    ArchiveMonthDetailsUiStatus.CONTENT,
                monthId = financialMonth.id,
                monthLabel =
                    createMonthLabel(
                        year = financialMonth.year,
                        monthNumber = financialMonth.monthNumber,
                    ),
                initialBudget = financialMonth.initialBudget,
                totalIncome = monthSummary.totalIncome,
                totalExpense = monthSummary.totalExpense,
                finalBalance = finalBalance,
                transactionCount = snapshot.transactions.size,
                startedAtMillis = financialMonth.startedAtMillis,
                closedAtMillis = financialMonth.closedAtMillis,
                expenseBreakdown = expenseBreakdown,
                transactions = transactionUiModels,
                errorMessage = null,
            )
    }

    private fun createExpenseBreakdown(
        transactions: List<Transaction>,
        categoriesById: Map<Long, TransactionCategory>,
        totalExpense: BigDecimal,
    ): List<ArchiveExpenseCategoryUiModel> {
        return transactions
            .asSequence()
            .filter { transaction ->
                transaction.type == TransactionType.EXPENSE
            }
            .groupBy { transaction ->
                transaction.categoryId
            }
            .map { (categoryId, categoryTransactions) ->
                val amount =
                    categoryTransactions.fold(
                        BigDecimal.ZERO,
                    ) { total, transaction ->
                        total.add(transaction.amount)
                    }

                val sharePercent =
                    if (
                        totalExpense.compareTo(BigDecimal.ZERO) == 0
                    ) {
                        0.0
                    } else {
                        amount
                            .divide(
                                totalExpense,
                                MathContext.DECIMAL64,
                            )
                            .multiply(PERCENT_MULTIPLIER)
                            .toDouble()
                    }

                ArchiveExpenseCategoryUiModel(
                    categoryId = categoryId,
                    categoryName =
                        categoriesById[categoryId]
                            ?.name
                            ?: UNKNOWN_CATEGORY_NAME,
                    amount = amount,
                    transactionCount =
                        categoryTransactions.size,
                    sharePercent = sharePercent,
                )
            }
            .sortedByDescending { category ->
                category.amount
            }
    }

    private fun Transaction.toUiModel(
        categoriesById: Map<Long, TransactionCategory>,
    ): ArchiveMonthTransactionUiModel {
        return ArchiveMonthTransactionUiModel(
            id = id,
            amount = amount,
            type = type,
            categoryName =
                categoriesById[categoryId]
                    ?.name
                    ?: UNKNOWN_CATEGORY_NAME,
            note = note,
            createdAtMillis = createdAtMillis,
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

    private data class ArchiveMonthDetailsSnapshot(
        val financialMonth: FinancialMonth?,
        val transactions: List<Transaction>,
        val categories: List<TransactionCategory>,
    )

    private companion object {

        val PERCENT_MULTIPLIER =
            BigDecimal("100")

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