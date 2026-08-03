package ru.lilnaro.finflow.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.lilnaro.finflow.domain.model.FinancialMonth
import ru.lilnaro.finflow.domain.repository.FinanceRepository

class ObserveArchivedFinancialMonthsUseCase(
    private val financeRepository: FinanceRepository,
) {

    operator fun invoke(): Flow<List<FinancialMonth>> {
        return financeRepository
            .observeFinancialMonths()
            .map { financialMonths ->
                financialMonths.filter { financialMonth ->
                    financialMonth.isClosed
                }
            }
    }
}