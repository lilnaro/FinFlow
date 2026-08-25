package ru.lilnaro.finflow.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.lilnaro.finflow.domain.model.FinancialMonth
import ru.lilnaro.finflow.domain.model.Transaction
import ru.lilnaro.finflow.domain.model.TransactionCategory
import ru.lilnaro.finflow.domain.model.TransactionType

interface FinanceRepository {

    fun observeActiveFinancialMonth(): Flow<FinancialMonth?>

    fun observeFinancialMonths(): Flow<List<FinancialMonth>>

    suspend fun getFinancialMonthById(
        financialMonthId: Long,
    ): FinancialMonth?

    suspend fun addFinancialMonth(
        financialMonth: FinancialMonth,
    ): Long

    suspend fun updateFinancialMonth(
        financialMonth: FinancialMonth,
    )

    fun observeTransactionsByMonth(
        financialMonthId: Long,
    ): Flow<List<Transaction>>

    suspend fun getTransactionById(
        transactionId: Long,
    ): Transaction?

    suspend fun addTransaction(
        transaction: Transaction,
    ): Long

    suspend fun updateTransaction(
        transaction: Transaction,
    )

    suspend fun deleteTransactionById(
        transactionId: Long,
    )

    suspend fun deleteTransactionsByIds(
        transactionIds: List<Long>,
    )

    fun observeCategoriesByType(
        type: TransactionType,
    ): Flow<List<TransactionCategory>>

    suspend fun getCategoryById(
        categoryId: Long,
    ): TransactionCategory?

    suspend fun addCategory(
        category: TransactionCategory,
    ): Long

    suspend fun updateCategory(
        category: TransactionCategory,
    )

    suspend fun deleteCategoryById(
        categoryId: Long,
    )
}