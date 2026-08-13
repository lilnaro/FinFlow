package ru.lilnaro.finflow.domain.model.result

import ru.lilnaro.finflow.domain.model.TransactionType

sealed interface AddTransactionResult {

    data class Success(
        val transactionId: Long,
    ) : AddTransactionResult

    data class TransactionAlreadySaved(
        val transactionId: Long,
    ) : AddTransactionResult

    data class FinancialMonthNotFound(
        val financialMonthId: Long,
    ) : AddTransactionResult

    data class FinancialMonthClosed(
        val financialMonthId: Long,
    ) : AddTransactionResult

    data class CategoryNotFound(
        val categoryId: Long,
    ) : AddTransactionResult

    data class CategoryTypeMismatch(
        val transactionType: TransactionType,
        val categoryType: TransactionType,
    ) : AddTransactionResult
}