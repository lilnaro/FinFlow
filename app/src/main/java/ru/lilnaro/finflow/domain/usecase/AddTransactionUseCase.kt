package ru.lilnaro.finflow.domain.usecase

import ru.lilnaro.finflow.domain.model.Transaction
import ru.lilnaro.finflow.domain.model.result.AddTransactionResult
import ru.lilnaro.finflow.domain.repository.FinanceRepository

class AddTransactionUseCase(
    private val financeRepository: FinanceRepository,
) {

    suspend operator fun invoke(
        transaction: Transaction,
    ): AddTransactionResult {
        if (transaction.id != 0L) {
            return AddTransactionResult.TransactionAlreadySaved(
                transactionId = transaction.id,
            )
        }

        val financialMonth =
            financeRepository.getFinancialMonthById(
                financialMonthId = transaction.financialMonthId,
            )
                ?: return AddTransactionResult.FinancialMonthNotFound(
                    financialMonthId = transaction.financialMonthId,
                )

        if (financialMonth.isClosed) {
            return AddTransactionResult.FinancialMonthClosed(
                financialMonthId = financialMonth.id,
            )
        }

        val category =
            financeRepository.getCategoryById(
                categoryId = transaction.categoryId,
            )
                ?: return AddTransactionResult.CategoryNotFound(
                    categoryId = transaction.categoryId,
                )

        if (category.type != transaction.type) {
            return AddTransactionResult.CategoryTypeMismatch(
                transactionType = transaction.type,
                categoryType = category.type,
            )
        }

        val transactionId =
            financeRepository.addTransaction(
                transaction = transaction,
            )

        return AddTransactionResult.Success(
            transactionId = transactionId,
        )
    }
}