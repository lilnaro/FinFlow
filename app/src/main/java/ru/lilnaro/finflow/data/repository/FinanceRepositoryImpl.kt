package ru.lilnaro.finflow.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.lilnaro.finflow.data.local.dao.FinancialMonthDao
import ru.lilnaro.finflow.data.local.dao.TransactionCategoryDao
import ru.lilnaro.finflow.data.local.dao.TransactionDao
import ru.lilnaro.finflow.data.mapper.toDomain
import ru.lilnaro.finflow.data.mapper.toEntity
import ru.lilnaro.finflow.domain.model.FinancialMonth
import ru.lilnaro.finflow.domain.model.Transaction
import ru.lilnaro.finflow.domain.model.TransactionCategory
import ru.lilnaro.finflow.domain.model.TransactionType
import ru.lilnaro.finflow.domain.repository.FinanceRepository

class FinanceRepositoryImpl(
    private val financialMonthDao: FinancialMonthDao,
    private val transactionDao: TransactionDao,
    private val transactionCategoryDao: TransactionCategoryDao,
) : FinanceRepository {

    override fun observeActiveFinancialMonth(): Flow<FinancialMonth?> {
        return financialMonthDao
            .observeActiveFinancialMonth()
            .map { entity ->
                entity?.toDomain()
            }
    }

    override fun observeFinancialMonths(): Flow<List<FinancialMonth>> {
        return financialMonthDao
            .observeFinancialMonths()
            .map { entities ->
                entities.map { entity ->
                    entity.toDomain()
                }
            }
    }

    override suspend fun getFinancialMonthById(
        financialMonthId: Long,
    ): FinancialMonth? {
        return financialMonthDao
            .getFinancialMonthById(financialMonthId)
            ?.toDomain()
    }

    override suspend fun addFinancialMonth(
        financialMonth: FinancialMonth,
    ): Long {
        return financialMonthDao.insertFinancialMonth(
            financialMonth = financialMonth.toEntity(),
        )
    }

    override suspend fun updateFinancialMonth(
        financialMonth: FinancialMonth,
    ) {
        financialMonthDao.updateFinancialMonth(
            financialMonth = financialMonth.toEntity(),
        )
    }

    override fun observeTransactionsByMonth(
        financialMonthId: Long,
    ): Flow<List<Transaction>> {
        return transactionDao
            .observeTransactionsByMonth(financialMonthId)
            .map { entities ->
                entities.map { entity ->
                    entity.toDomain()
                }
            }
    }

    override suspend fun getTransactionById(
        transactionId: Long,
    ): Transaction? {
        return transactionDao
            .getTransactionById(transactionId)
            ?.toDomain()
    }

    override suspend fun addTransaction(
        transaction: Transaction,
    ): Long {
        return transactionDao.insertTransaction(
            transaction = transaction.toEntity(),
        )
    }

    override suspend fun updateTransaction(
        transaction: Transaction,
    ) {
        transactionDao.updateTransaction(
            transaction = transaction.toEntity(),
        )
    }

    override suspend fun deleteTransactionById(
        transactionId: Long,
    ) {
        transactionDao.deleteTransactionById(transactionId)
    }

    override fun observeCategoriesByType(
        type: TransactionType,
    ): Flow<List<TransactionCategory>> {
        return transactionCategoryDao
            .observeCategoriesByType(type)
            .map { entities ->
                entities.map { entity ->
                    entity.toDomain()
                }
            }
    }

    override suspend fun getCategoryById(
        categoryId: Long,
    ): TransactionCategory? {
        return transactionCategoryDao
            .getCategoryById(categoryId)
            ?.toDomain()
    }

    override suspend fun addCategory(
        category: TransactionCategory,
    ): Long {
        return transactionCategoryDao.insertCategory(
            category = category.toEntity(),
        )
    }

    override suspend fun updateCategory(
        category: TransactionCategory,
    ) {
        transactionCategoryDao.updateCategory(
            category = category.toEntity(),
        )
    }

    override suspend fun deleteCategoryById(
        categoryId: Long,
    ) {
        transactionCategoryDao.deleteCategoryById(categoryId)
    }
}