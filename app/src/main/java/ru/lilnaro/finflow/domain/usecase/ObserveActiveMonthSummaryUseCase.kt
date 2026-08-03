package ru.lilnaro.finflow.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ru.lilnaro.finflow.domain.model.MonthSummary

class ObserveActiveMonthSummaryUseCase(
    private val observeActiveFinancialMonthUseCase:
    ObserveActiveFinancialMonthUseCase,
    private val observeTransactionsByMonthUseCase:
    ObserveTransactionsByMonthUseCase,
    private val calculateMonthSummaryUseCase:
    CalculateMonthSummaryUseCase,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<MonthSummary?> {
        return observeActiveFinancialMonthUseCase()
            .flatMapLatest { financialMonth ->
                if (financialMonth == null) {
                    flowOf<MonthSummary?>(null)
                } else {
                    observeTransactionsByMonthUseCase(
                        financialMonthId = financialMonth.id,
                    ).map { transactions ->
                        calculateMonthSummaryUseCase(
                            financialMonth = financialMonth,
                            transactions = transactions,
                        )
                    }
                }
            }
    }
}