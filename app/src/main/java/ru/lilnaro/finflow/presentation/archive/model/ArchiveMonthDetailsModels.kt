package ru.lilnaro.finflow.presentation.archive.model

import java.math.BigDecimal
import ru.lilnaro.finflow.domain.model.TransactionType

sealed interface ArchiveMonthDetailsAction {

    data object BackClicked : ArchiveMonthDetailsAction

    data object RetryClicked : ArchiveMonthDetailsAction
}

sealed interface ArchiveMonthDetailsEffect {

    data object NavigateBack : ArchiveMonthDetailsEffect
}

data class ArchiveMonthDetailsUiState(
    val status: ArchiveMonthDetailsUiStatus =
        ArchiveMonthDetailsUiStatus.LOADING,
    val monthId: Long = 0L,
    val monthLabel: String = "",
    val initialBudget: BigDecimal = BigDecimal.ZERO,
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpense: BigDecimal = BigDecimal.ZERO,
    val finalBalance: BigDecimal = BigDecimal.ZERO,
    val transactionCount: Int = 0,
    val startedAtMillis: Long = 0L,
    val closedAtMillis: Long? = null,
    val expenseBreakdown:
    List<ArchiveCategoryBreakdownUiModel> = emptyList(),
    val incomeBreakdown:
    List<ArchiveCategoryBreakdownUiModel> = emptyList(),
    val transactions:
    List<ArchiveMonthTransactionUiModel> = emptyList(),
    val errorMessage: String? = null,
)

enum class ArchiveMonthDetailsUiStatus {
    LOADING,
    CONTENT,
    NOT_FOUND,
    ERROR,
}

data class ArchiveCategoryBreakdownUiModel(
    val categoryId: Long,
    val categoryName: String,
    val amount: BigDecimal,
    val transactionCount: Int,
    val sharePercent: Double,
)

data class ArchiveMonthTransactionUiModel(
    val id: Long,
    val amount: BigDecimal,
    val type: TransactionType,
    val categoryName: String,
    val note: String,
    val createdAtMillis: Long,
)