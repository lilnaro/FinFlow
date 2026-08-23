package ru.lilnaro.finflow.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.lilnaro.finflow.domain.model.FinancialAnalyticsSnapshot

class ObserveFinancialAnalyticsUseCase(
    private val observeFinancialAnalysisContextUseCase:
    ObserveFinancialAnalysisContextUseCase,
    private val analyzeFinancialContextUseCase:
    AnalyzeFinancialContextUseCase,
) {

    operator fun invoke(
        nowMillis: Long,
    ): Flow<FinancialAnalyticsSnapshot> {
        require(nowMillis > 0L) {
            "Текущее время должно быть положительным"
        }

        return observeFinancialAnalysisContextUseCase()
            .map { context ->
                analyzeFinancialContextUseCase(
                    context = context,
                    nowMillis = nowMillis,
                )
            }
    }
}