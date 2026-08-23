package ru.lilnaro.finflow.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import ru.lilnaro.finflow.domain.model.FinancialAnalysisContext
import ru.lilnaro.finflow.domain.model.FinancialMonth
import ru.lilnaro.finflow.domain.repository.FinanceRepository

class ObserveFinancialAnalysisContextUseCase(
    private val financeRepository: FinanceRepository,
    private val observeFinancialMonthReportUseCase:
    ObserveFinancialMonthReportUseCase,
    private val buildFinancialAnalysisContextUseCase:
    BuildFinancialAnalysisContextUseCase,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<FinancialAnalysisContext> {
        return financeRepository
            .observeFinancialMonths()
            .flatMapLatest { financialMonths ->
                val savedMonths =
                    financialMonths
                        .filter { month ->
                            month.id > 0L
                        }
                        .sortedWith(
                            compareBy<FinancialMonth>(
                                { month ->
                                    month.year
                                },
                                { month ->
                                    month.monthNumber
                                },
                            ),
                        )

                if (savedMonths.isEmpty()) {
                    flowOf(
                        buildFinancialAnalysisContextUseCase(
                            reports = emptyList(),
                        ),
                    )
                } else {
                    val reportFlows =
                        savedMonths.map { month ->
                            observeFinancialMonthReportUseCase(
                                financialMonthId =
                                    month.id,
                            )
                        }

                    combine(
                        reportFlows,
                    ) { reports ->
                        buildFinancialAnalysisContextUseCase(
                            reports =
                                reports.filterNotNull(),
                        )
                    }
                }
            }
            .distinctUntilChanged()
    }
}