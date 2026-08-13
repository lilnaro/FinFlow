package ru.lilnaro.finflow.domain.usecase

import ru.lilnaro.finflow.domain.model.result.DeleteTransactionsResult
import ru.lilnaro.finflow.domain.repository.FinanceRepository

class DeleteTransactionsUseCase(
    private val financeRepository: FinanceRepository,
) {

    suspend operator fun invoke(
        transactionIds: Set<Long>,
    ): DeleteTransactionsResult {
        if (transactionIds.isEmpty()) {
            return DeleteTransactionsResult.EmptySelection
        }

        val invalidTransactionId =
            transactionIds.firstOrNull { transactionId ->
                transactionId <= 0L
            }

        if (invalidTransactionId != null) {
            return DeleteTransactionsResult.InvalidTransactionId(
                transactionId = invalidTransactionId,
            )
        }

        val transactions = transactionIds.map { transactionId ->
            financeRepository.getTransactionById(
                transactionId = transactionId,
            )
                ?: return DeleteTransactionsResult.TransactionNotFound(
                    transactionId = transactionId,
                )
        }

        val financialMonthIds =
            transactions
                .map { transaction ->
                    transaction.financialMonthId
                }
                .toSet()

        if (financialMonthIds.size != 1) {
            return DeleteTransactionsResult
                .TransactionsBelongToDifferentMonths
        }

        val financialMonthId =
            financialMonthIds.first()

        val financialMonth =
            financeRepository.getFinancialMonthById(
                financialMonthId = financialMonthId,
            )
                ?: return DeleteTransactionsResult.FinancialMonthNotFound(
                    financialMonthId = financialMonthId,
                )

        if (financialMonth.isClosed) {
            return DeleteTransactionsResult.FinancialMonthClosed(
                financialMonthId = financialMonth.id,
            )
        }

        transactionIds.forEach { transactionId ->
            financeRepository.deleteTransactionById(
                transactionId = transactionId,
            )
        }

        return DeleteTransactionsResult.Success(
            deletedCount = transactionIds.size,
        )
    }
}