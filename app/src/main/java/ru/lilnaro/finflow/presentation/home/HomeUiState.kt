package ru.lilnaro.finflow.presentation.home

import java.math.BigDecimal

data class HomeUiState(
    val initialBudget: BigDecimal = BigDecimal("40000.00"),
    val currentBalance: BigDecimal = BigDecimal("27400.00"),
    val balanceChangePercent: Double = -31.5,
)