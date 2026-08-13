package ru.lilnaro.finflow.presentation.transactions.model

import java.math.BigDecimal
import ru.lilnaro.finflow.domain.model.TransactionType

data class TransactionsUiState(
    val status: TransactionsUiStatus =
        TransactionsUiStatus.LOADING,
    val monthLabel: String = "",
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpense: BigDecimal = BigDecimal.ZERO,
    val selectedFilter: TransactionsFilter =
        TransactionsFilter.ALL,
    val transactions: List<TransactionUiModel> =
        emptyList(),
    val selectedTransactionIds: Set<Long> =
        emptySet(),
    val isDeleteConfirmationVisible: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
) {

    val visibleTransactions: List<TransactionUiModel>
        get() {
            return when (selectedFilter) {
                TransactionsFilter.ALL -> {
                    transactions
                }

                TransactionsFilter.EXPENSE -> {
                    transactions.filter { transaction ->
                        transaction.type ==
                                TransactionType.EXPENSE
                    }
                }

                TransactionsFilter.INCOME -> {
                    transactions.filter { transaction ->
                        transaction.type ==
                                TransactionType.INCOME
                    }
                }
            }
        }

    val isSelectionMode: Boolean
        get() = selectedTransactionIds.isNotEmpty()

    val selectedCount: Int
        get() = selectedTransactionIds.size
}

enum class TransactionsUiStatus {
    LOADING,
    NO_ACTIVE_MONTH,
    EMPTY,
    CONTENT,
    ERROR,
}

enum class TransactionsFilter {
    ALL,
    EXPENSE,
    INCOME,
}

data class TransactionUiModel(
    val id: Long,
    val amount: BigDecimal,
    val type: TransactionType,
    val categoryId: Long,
    val categoryName: String,
    val note: String,
    val createdAtMillis: Long,
)