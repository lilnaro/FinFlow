package ru.lilnaro.finflow.presentation.transactions.addtransaction.model

import ru.lilnaro.finflow.domain.model.TransactionType

sealed interface AddTransactionAction {

    data object BackClicked : AddTransactionAction

    data object RetryClicked : AddTransactionAction

    data class TypeChanged(
        val type: TransactionType,
    ) : AddTransactionAction

    data class AmountChanged(
        val value: String,
    ) : AddTransactionAction

    data class CategorySelected(
        val categoryId: Long,
    ) : AddTransactionAction

    data class NoteChanged(
        val value: String,
    ) : AddTransactionAction

    data object SaveClicked : AddTransactionAction
}