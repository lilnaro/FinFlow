package ru.lilnaro.finflow.presentation.transactions.model

sealed interface TransactionsEffect {

    data object NavigateBack : TransactionsEffect

    data class ShowMessage(
        val message: String,
    ) : TransactionsEffect
}