package ru.lilnaro.finflow.domain.model.result

sealed interface DeleteTransactionsResult {

    data class Success(
        val deletedCount: Int,
    ) : DeleteTransactionsResult

    data object EmptySelection :
        DeleteTransactionsResult

    data class InvalidTransactionId(
        val transactionId: Long,
    ) : DeleteTransactionsResult

    data class TransactionNotFound(
        val transactionId: Long,
    ) : DeleteTransactionsResult

    data class FinancialMonthNotFound(
        val financialMonthId: Long,
    ) : DeleteTransactionsResult

    data class FinancialMonthClosed(
        val financialMonthId: Long,
    ) : DeleteTransactionsResult

    data object TransactionsBelongToDifferentMonths :
        DeleteTransactionsResult
}