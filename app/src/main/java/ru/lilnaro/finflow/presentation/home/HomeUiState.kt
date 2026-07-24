package ru.lilnaro.finflow.presentation.home

data class HomeUiState(
    val initialBudget: Long = 40_000L,
    val currentBalance: Long = 27_400L,
    val balanceChangePercent: Double = -31.5,
)