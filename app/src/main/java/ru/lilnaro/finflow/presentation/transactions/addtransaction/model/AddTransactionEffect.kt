package ru.lilnaro.finflow.presentation.transactions.addtransaction.model

sealed interface AddTransactionEffect {

    data object NavigateBack :
        AddTransactionEffect

    data class TransactionSaved(
        val message: String,
    ) : AddTransactionEffect

    data class ShowMessage(
        val message: String,
    ) : AddTransactionEffect
}