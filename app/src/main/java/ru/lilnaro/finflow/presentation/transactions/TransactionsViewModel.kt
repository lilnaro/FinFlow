package ru.lilnaro.finflow.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.lilnaro.finflow.domain.model.FinancialMonth
import ru.lilnaro.finflow.domain.model.Transaction
import ru.lilnaro.finflow.domain.model.TransactionCategory
import ru.lilnaro.finflow.domain.model.TransactionType
import ru.lilnaro.finflow.domain.model.result.DeleteTransactionsResult
import ru.lilnaro.finflow.domain.usecase.CalculateMonthSummaryUseCase
import ru.lilnaro.finflow.domain.usecase.DeleteTransactionsUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveActiveFinancialMonthUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveCategoriesByTypeUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveTransactionsByMonthUseCase
import ru.lilnaro.finflow.presentation.transactions.model.TransactionUiModel
import ru.lilnaro.finflow.presentation.transactions.model.TransactionsAction
import ru.lilnaro.finflow.presentation.transactions.model.TransactionsEffect
import ru.lilnaro.finflow.presentation.transactions.model.TransactionsFilter
import ru.lilnaro.finflow.presentation.transactions.model.TransactionsUiState
import ru.lilnaro.finflow.presentation.transactions.model.TransactionsUiStatus

class TransactionsViewModel(
    private val observeActiveFinancialMonthUseCase:
    ObserveActiveFinancialMonthUseCase,
    private val observeTransactionsByMonthUseCase:
    ObserveTransactionsByMonthUseCase,
    private val observeCategoriesByTypeUseCase:
    ObserveCategoriesByTypeUseCase,
    private val calculateMonthSummaryUseCase:
    CalculateMonthSummaryUseCase,
    private val deleteTransactionsUseCase:
    DeleteTransactionsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransactionsUiState(),
    )

    val uiState: StateFlow<TransactionsUiState> =
        _uiState.asStateFlow()

    private val _effect = Channel<TransactionsEffect>(
        capacity = Channel.BUFFERED,
    )

    val effect: Flow<TransactionsEffect> =
        _effect.receiveAsFlow()

    private var observationJob: Job? = null

    init {
        observeTransactions()
    }

    fun onAction(
        action: TransactionsAction,
    ) {
        when (action) {
            TransactionsAction.BackClicked -> {
                handleBackClick()
            }

            TransactionsAction.RetryClicked -> {
                observeTransactions()
            }

            is TransactionsAction.FilterChanged -> {
                changeFilter(
                    filter = action.filter,
                )
            }

            is TransactionsAction.TransactionLongClicked -> {
                toggleTransactionSelection(
                    transactionId = action.transactionId,
                )
            }

            is TransactionsAction.TransactionClicked -> {
                if (_uiState.value.isSelectionMode) {
                    toggleTransactionSelection(
                        transactionId = action.transactionId,
                    )
                }
            }

            TransactionsAction.ExitSelectionModeClicked -> {
                clearSelection()
            }

            TransactionsAction.DeleteSelectedClicked -> {
                showDeleteConfirmation()
            }

            TransactionsAction.DeleteConfirmed -> {
                deleteSelectedTransactions()
            }

            TransactionsAction.DeleteCancelled -> {
                hideDeleteConfirmation()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTransactions() {
        observationJob?.cancel()

        _uiState.value = TransactionsUiState(
            status = TransactionsUiStatus.LOADING,
            selectedFilter = _uiState.value.selectedFilter,
        )

        observationJob = viewModelScope.launch {
            observeActiveFinancialMonthUseCase()
                .flatMapLatest { financialMonth ->
                    if (financialMonth == null) {
                        flowOf<TransactionsSnapshot?>(null)
                    } else {
                        combine(
                            observeTransactionsByMonthUseCase(
                                financialMonthId =
                                    financialMonth.id,
                            ),
                            observeCategoriesByTypeUseCase(
                                type = TransactionType.INCOME,
                            ),
                            observeCategoriesByTypeUseCase(
                                type = TransactionType.EXPENSE,
                            ),
                        ) {
                                transactions,
                                incomeCategories,
                                expenseCategories,
                            ->

                            TransactionsSnapshot(
                                financialMonth = financialMonth,
                                transactions = transactions,
                                categories =
                                    incomeCategories +
                                            expenseCategories,
                            )
                        }
                    }
                }
                .catch {
                    _uiState.value =
                        _uiState.value.copy(
                            status =
                                TransactionsUiStatus.ERROR,
                            selectedTransactionIds =
                                emptySet(),
                            isDeleteConfirmationVisible =
                                false,
                            isDeleting = false,
                            errorMessage =
                                "Не удалось загрузить транзакции. Попробуйте ещё раз.",
                        )
                }
                .collect { snapshot ->
                    if (snapshot == null) {
                        _uiState.value =
                            TransactionsUiState(
                                status =
                                    TransactionsUiStatus
                                        .NO_ACTIVE_MONTH,
                                selectedFilter =
                                    _uiState.value
                                        .selectedFilter,
                            )
                    } else {
                        updateContent(
                            snapshot = snapshot,
                        )
                    }
                }
        }
    }

    private fun updateContent(
        snapshot: TransactionsSnapshot,
    ) {
        val monthSummary =
            calculateMonthSummaryUseCase(
                financialMonth =
                    snapshot.financialMonth,
                transactions =
                    snapshot.transactions,
            )

        val currentBalance =
            monthSummary.initialBudget
                .add(monthSummary.totalIncome)
                .subtract(monthSummary.totalExpense)

        val categoriesById =
            snapshot.categories.associateBy { category ->
                category.id
            }

        val transactionUiModels =
            snapshot.transactions.map { transaction ->
                transaction.toUiModel(
                    category = categoriesById[
                        transaction.categoryId
                    ],
                )
            }

        val actualTransactionIds =
            transactionUiModels
                .map { transaction ->
                    transaction.id
                }
                .toSet()

        val selectedTransactionIds =
            _uiState.value
                .selectedTransactionIds
                .intersect(actualTransactionIds)

        _uiState.value =
            _uiState.value.copy(
                status = if (
                    transactionUiModels.isEmpty()
                ) {
                    TransactionsUiStatus.EMPTY
                } else {
                    TransactionsUiStatus.CONTENT
                },
                monthLabel = createMonthLabel(
                    monthNumber =
                        snapshot.financialMonth
                            .monthNumber,
                ),
                currentBalance = currentBalance,
                totalIncome =
                    monthSummary.totalIncome,
                totalExpense =
                    monthSummary.totalExpense,
                transactions =
                    transactionUiModels,
                selectedTransactionIds =
                    selectedTransactionIds,
                isDeleteConfirmationVisible =
                    _uiState.value
                        .isDeleteConfirmationVisible &&
                            selectedTransactionIds
                                .isNotEmpty(),
                errorMessage = null,
            )
    }

    private fun handleBackClick() {
        val currentState = _uiState.value

        when {
            currentState.isDeleteConfirmationVisible -> {
                hideDeleteConfirmation()
            }

            currentState.isSelectionMode -> {
                clearSelection()
            }

            else -> {
                _effect.trySend(
                    TransactionsEffect.NavigateBack,
                )
            }
        }
    }

    private fun changeFilter(
        filter: TransactionsFilter,
    ) {
        _uiState.value =
            _uiState.value.copy(
                selectedFilter = filter,
                selectedTransactionIds =
                    emptySet(),
                isDeleteConfirmationVisible =
                    false,
            )
    }

    private fun toggleTransactionSelection(
        transactionId: Long,
    ) {
        val currentState = _uiState.value

        val transactionExists =
            currentState.transactions.any { transaction ->
                transaction.id == transactionId
            }

        if (!transactionExists) {
            return
        }

        val updatedSelection =
            if (
                transactionId in
                currentState.selectedTransactionIds
            ) {
                currentState.selectedTransactionIds -
                        transactionId
            } else {
                currentState.selectedTransactionIds +
                        transactionId
            }

        _uiState.value =
            currentState.copy(
                selectedTransactionIds =
                    updatedSelection,
                isDeleteConfirmationVisible =
                    if (updatedSelection.isEmpty()) {
                        false
                    } else {
                        currentState
                            .isDeleteConfirmationVisible
                    },
            )
    }

    private fun clearSelection() {
        _uiState.value =
            _uiState.value.copy(
                selectedTransactionIds =
                    emptySet(),
                isDeleteConfirmationVisible =
                    false,
            )
    }

    private fun showDeleteConfirmation() {
        val currentState = _uiState.value

        if (
            currentState.selectedTransactionIds
                .isEmpty() ||
            currentState.isDeleting
        ) {
            return
        }

        _uiState.value =
            currentState.copy(
                isDeleteConfirmationVisible = true,
            )
    }

    private fun hideDeleteConfirmation() {
        if (_uiState.value.isDeleting) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                isDeleteConfirmationVisible = false,
            )
    }

    private fun deleteSelectedTransactions() {
        val currentState = _uiState.value

        if (
            currentState.selectedTransactionIds
                .isEmpty() ||
            currentState.isDeleting
        ) {
            return
        }

        val transactionIds =
            currentState.selectedTransactionIds

        _uiState.value =
            currentState.copy(
                isDeleting = true,
            )

        viewModelScope.launch {
            val result =
                try {
                    deleteTransactionsUseCase(
                        transactionIds = transactionIds,
                    )
                } catch (_: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isDeleting = false,
                            isDeleteConfirmationVisible =
                                false,
                            selectedTransactionIds =
                                emptySet(),
                        )

                    _effect.send(
                        TransactionsEffect.ShowMessage(
                            message =
                                "Не удалось удалить транзакции.",
                        ),
                    )

                    return@launch
                }

            handleDeleteResult(
                result = result,
            )
        }
    }

    private suspend fun handleDeleteResult(
        result: DeleteTransactionsResult,
    ) {
        when (result) {
            is DeleteTransactionsResult.Success -> {
                _uiState.value =
                    _uiState.value.copy(
                        selectedTransactionIds =
                            emptySet(),
                        isDeleteConfirmationVisible =
                            false,
                        isDeleting = false,
                    )

                val message =
                    if (result.deletedCount == 1) {
                        "Транзакция удалена."
                    } else {
                        "Удалено транзакций: ${result.deletedCount}."
                    }

                _effect.send(
                    TransactionsEffect.ShowMessage(
                        message = message,
                    ),
                )
            }

            DeleteTransactionsResult.EmptySelection -> {
                finishDeleteWithMessage(
                    message =
                        "Не выбраны транзакции для удаления.",
                )
            }

            is DeleteTransactionsResult.InvalidTransactionId -> {
                finishDeleteWithMessage(
                    message =
                        "Не удалось определить выбранную транзакцию.",
                )
            }

            is DeleteTransactionsResult.TransactionNotFound -> {
                finishDeleteWithMessage(
                    message =
                        "Одна из выбранных транзакций больше недоступна.",
                )
            }

            is DeleteTransactionsResult.FinancialMonthNotFound -> {
                finishDeleteWithMessage(
                    message =
                        "Финансовый месяц больше недоступен.",
                )
            }

            is DeleteTransactionsResult.FinancialMonthClosed -> {
                finishDeleteWithMessage(
                    message =
                        "Закрытый финансовый месяц нельзя редактировать.",
                )
            }

            DeleteTransactionsResult
                .TransactionsBelongToDifferentMonths -> {
                finishDeleteWithMessage(
                    message =
                        "Выбранные транзакции относятся к разным месяцам.",
                )
            }
        }
    }

    private suspend fun finishDeleteWithMessage(
        message: String,
    ) {
        _uiState.value =
            _uiState.value.copy(
                selectedTransactionIds =
                    emptySet(),
                isDeleteConfirmationVisible =
                    false,
                isDeleting = false,
            )

        _effect.send(
            TransactionsEffect.ShowMessage(
                message = message,
            ),
        )
    }

    private fun Transaction.toUiModel(
        category: TransactionCategory?,
    ): TransactionUiModel {
        return TransactionUiModel(
            id = id,
            amount = amount,
            type = type,
            categoryId = categoryId,
            categoryName =
                category?.name
                    ?: "Категория недоступна",
            note = note,
            createdAtMillis = createdAtMillis,
        )
    }

    private fun createMonthLabel(
        monthNumber: Int,
    ): String {
        return MONTH_NAMES.getOrElse(
            index = monthNumber - 1,
        ) {
            "Месяц $monthNumber"
        }
    }

    private data class TransactionsSnapshot(
        val financialMonth: FinancialMonth,
        val transactions: List<Transaction>,
        val categories:
        List<TransactionCategory>,
    )

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