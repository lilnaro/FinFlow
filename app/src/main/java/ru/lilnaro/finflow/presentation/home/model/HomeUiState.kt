package ru.lilnaro.finflow.presentation.home.model

import java.math.BigDecimal

data class HomeUiState(
    val status: HomeUiStatus = HomeUiStatus.LOADING,
    val greeting: String = "",
    val monthLabel: String = "",
    val initialBudget: BigDecimal = BigDecimal.ZERO,
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpense: BigDecimal = BigDecimal.ZERO,
    val balanceChangePercent: Double = 0.0,
    val budgetRemainingPercent: Double = 0.0,
    val errorMessage: String? = null,
)

enum class HomeUiStatus {
    LOADING,
    CONTENT,
    NO_ACTIVE_MONTH,
    ERROR,
}