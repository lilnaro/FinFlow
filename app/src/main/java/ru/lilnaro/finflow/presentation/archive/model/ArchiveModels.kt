package ru.lilnaro.finflow.presentation.archive.model

import java.math.BigDecimal

sealed interface ArchiveAction {

    data object BackClicked : ArchiveAction

    data object RetryClicked : ArchiveAction

    data class MonthClicked(
        val monthId: Long,
    ) : ArchiveAction
}

sealed interface ArchiveEffect {

    data object NavigateBack : ArchiveEffect

    data class NavigateToMonthDetails(
        val monthId: Long,
    ) : ArchiveEffect
}

data class ArchiveUiState(
    val status: ArchiveUiStatus =
        ArchiveUiStatus.LOADING,
    val months: List<ArchiveMonthUiModel> =
        emptyList(),
    val errorMessage: String? = null,
)

enum class ArchiveUiStatus {
    LOADING,
    EMPTY,
    CONTENT,
    ERROR,
}

data class ArchiveMonthUiModel(
    val id: Long,
    val monthLabel: String,
    val initialBudget: BigDecimal,
    val startedAtMillis: Long,
    val closedAtMillis: Long?,
)