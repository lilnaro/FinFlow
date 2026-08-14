package ru.lilnaro.finflow.presentation.newmonth.model

data class NewMonthUiState(
    val year: Int = 0,
    val monthNumber: Int = 0,
    val monthLabel: String = "",
    val initialBudgetInput: String = "",
    val budgetError: String? = null,
    val isSaving: Boolean = false,
) {

    val isCreateEnabled: Boolean
        get() {
            return initialBudgetInput.isNotBlank() &&
                    budgetError == null &&
                    !isSaving
        }
}