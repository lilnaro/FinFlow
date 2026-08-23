package ru.lilnaro.finflow.presentation.transactions.model

sealed interface TransactionsAction {

    data object BackClicked : TransactionsAction

    data object RetryClicked : TransactionsAction

    data class FilterChanged(
        val filter: TransactionsFilter,
    ) : TransactionsAction

    data class TransactionLongClicked(
        val transactionId: Long,
    ) : TransactionsAction

    data class TransactionClicked(
        val transactionId: Long,
    ) : TransactionsAction

    data object ExitSelectionModeClicked :
        TransactionsAction

    data object DeleteSelectedClicked :
        TransactionsAction

    data object DeleteConfirmed :
        TransactionsAction

    data object DeleteCancelled :
        TransactionsAction
}