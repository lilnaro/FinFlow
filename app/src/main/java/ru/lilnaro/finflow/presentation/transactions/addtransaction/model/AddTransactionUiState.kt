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
    val isCustomCategoryDialogVisible:
    Boolean = false,
    val customCategoryNameInput: String = "",
    val selectedParentCategoryId: Long? = null,
    val customCategoryNameError: String? = null,
    val customCategoryParentError: String? = null,
    val isCreatingCategory: Boolean = false,
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
                    !isCreatingCategory &&
                    !isSaving
        }

    val selectedCategory:
            AddTransactionCategoryUiModel?
        get() {
            return categories.firstOrNull { category ->
                category.id == selectedCategoryId
            }
        }

    val builtInCategories:
            List<AddTransactionCategoryUiModel>
        get() {
            return categories.filterNot { category ->
                category.isCustom
            }
        }

    val isCustomCategoryCreateEnabled: Boolean
        get() {
            return customCategoryNameInput.isNotBlank() &&
                    selectedParentCategoryId != null &&
                    customCategoryNameError == null &&
                    customCategoryParentError == null &&
                    !isCreatingCategory
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