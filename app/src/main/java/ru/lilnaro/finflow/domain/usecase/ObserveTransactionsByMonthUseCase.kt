package ru.lilnaro.finflow.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.lilnaro.finflow.domain.model.Transaction
import ru.lilnaro.finflow.domain.repository.FinanceRepository

class ObserveTransactionsByMonthUseCase(
    private val financeRepository: FinanceRepository,
) {

    operator fun invoke(
        financialMonthId: Long,
    ): Flow<List<Transaction>> {
        require(financialMonthId > 0L) {
            "Идентификатор финансового месяца должен быть положительным"
        }

        return financeRepository.observeTransactionsByMonth(
            financialMonthId = financialMonthId,
        )
    }
}