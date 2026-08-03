package ru.lilnaro.finflow.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.lilnaro.finflow.domain.model.FinancialMonth
import ru.lilnaro.finflow.domain.repository.FinanceRepository

class ObserveActiveFinancialMonthUseCase(
    private val financeRepository: FinanceRepository,
) {

    operator fun invoke(): Flow<FinancialMonth?> {
        return financeRepository.observeActiveFinancialMonth()
    }
}