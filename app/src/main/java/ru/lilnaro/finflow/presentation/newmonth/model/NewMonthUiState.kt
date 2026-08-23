package ru.lilnaro.finflow.presentation.newmonth.model

data class NewMonthUiState(
    val year: Int = 0,
    val monthNumber: Int = 0,
    val monthLabel: String = "",
    val availableMonthNumbers: List<Int> = emptyList(),
    val initialBudgetInput: String = "",
    val budgetError: String? = null,
    val periodError: String? = null,
    val isPeriodLoading: Boolean = true,
    val isSaving: Boolean = false,
) {

    val isCreateEnabled: Boolean
        get() {
            return initialBudgetInput.isNotBlank() &&
                    budgetError == null &&
                    periodError == null &&
                    !isPeriodLoading &&
                    !isSaving
        }
}
