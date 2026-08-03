package ru.lilnaro.finflow.domain.usecase

import ru.lilnaro.finflow.domain.repository.FinanceRepository

class CloseFinancialMonthUseCase(
    private val financeRepository: FinanceRepository,
) {

    suspend operator fun invoke(
        financialMonthId: Long,
        closedAtMillis: Long,
    ): CloseFinancialMonthResult {
        if (financialMonthId <= 0L) {
            return CloseFinancialMonthResult
                .InvalidFinancialMonthId(
                    financialMonthId = financialMonthId,
                )
        }

        if (closedAtMillis <= 0L) {
            return CloseFinancialMonthResult
                .InvalidClosedAtMillis(
                    closedAtMillis = closedAtMillis,
                )
        }

        val financialMonth =
            financeRepository.getFinancialMonthById(
                financialMonthId = financialMonthId,
            )
                ?: return CloseFinancialMonthResult
                    .FinancialMonthNotFound(
                        financialMonthId = financialMonthId,
                    )

        val existingClosedAtMillis =
            financialMonth.closedAtMillis

        if (existingClosedAtMillis != null) {
            return CloseFinancialMonthResult
                .FinancialMonthAlreadyClosed(
                    financialMonthId = financialMonth.id,
                    existingClosedAtMillis =
                        existingClosedAtMillis,
                )
        }

        if (closedAtMillis < financialMonth.startedAtMillis) {
            return CloseFinancialMonthResult
                .ClosingTimeBeforeMonthStart(
                    startedAtMillis =
                        financialMonth.startedAtMillis,
                    closedAtMillis = closedAtMillis,
                )
        }

        val closedFinancialMonth =
            financialMonth.copy(
                closedAtMillis = closedAtMillis,
            )

        financeRepository.updateFinancialMonth(
            financialMonth = closedFinancialMonth,
        )

        return CloseFinancialMonthResult.Success(
            financialMonthId = closedFinancialMonth.id,
            closedAtMillis = closedAtMillis,
        )
    }
}