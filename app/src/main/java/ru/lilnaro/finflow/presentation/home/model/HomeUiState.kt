package ru.lilnaro.finflow.presentation.home.model

import java.math.BigDecimal
import ru.lilnaro.finflow.domain.model.TransactionType

data class HomeUiState(
    val status: HomeUiStatus =
        HomeUiStatus.LOADING,
    val greeting: String = "",
    val monthLabel: String = "",
    val initialBudget: BigDecimal =
        BigDecimal.ZERO,
    val currentBalance: BigDecimal =
        BigDecimal.ZERO,
    val totalIncome: BigDecimal =
        BigDecimal.ZERO,
    val totalExpense: BigDecimal =
        BigDecimal.ZERO,
    val balanceChangePercent: Double = 0.0,
    val budgetRemainingPercent: Double = 0.0,
    val transactionCount: Int = 0,
    val topExpenseCategoryName: String? = null,
    val topExpenseCategoryAmount: BigDecimal =
        BigDecimal.ZERO,
    val topExpenseSharePercent: Double = 0.0,
    val recentTransactions:
    List<HomeRecentTransactionUiModel> =
        emptyList(),
    val isCloseMonthConfirmationVisible:
    Boolean = false,
    val isClosingMonth: Boolean = false,
    val closeMonthErrorMessage: String? = null,
    val errorMessage: String? = null,
)

data class HomeRecentTransactionUiModel(
    val id: Long,
    val amount: BigDecimal,
    val type: TransactionType,
    val categoryName: String,
    val note: String,
    val createdAtMillis: Long,
)

enum class HomeUiStatus {
    LOADING,
    CONTENT,
    NO_ACTIVE_MONTH,
    ERROR,
}