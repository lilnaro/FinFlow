package ru.lilnaro.finflow.presentation.transactions.addtransaction.model

import ru.lilnaro.finflow.domain.model.TransactionType

data class AddTransactionUiState(
    val status: AddTransactionUiStatus =
        AddTransactionUiStatus.LOADING,
    val monthLabel: String = "",
    val type: TransactionType =
        TransactionType.EXPENSE,
    val amountInput: String = "",
    val categories:
    List<AddTransactionCategoryUiModel> =
        emptyList(),
    val selectedCategoryId: Long? = null,
    val noteInput: String = "",
    val amountError: String? = null,
    val categoryError: String? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
) {

    val isSaveEnabled: Boolean
        get() {
            return status ==
                    AddTransactionUiStatus.CONTENT &&
                    amountInput.isNotBlank() &&
                    amountError == null &&
                    selectedCategoryId != null &&
                    categoryError == null &&
                    !isSaving
        }

    val selectedCategory:
            AddTransactionCategoryUiModel?
        get() {
            return categories.firstOrNull { category ->
                category.id == selectedCategoryId
            }
        }
}

enum class AddTransactionUiStatus {
    LOADING,
    CONTENT,
    NO_ACTIVE_MONTH,
    ERROR,
}

data class AddTransactionCategoryUiModel(
    val id: Long,
    val name: String,
    val isCustom: Boolean,
)