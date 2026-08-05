package ru.lilnaro.finflow.domain.usecase

import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import ru.lilnaro.finflow.domain.model.FinancialMonth
import ru.lilnaro.finflow.domain.repository.FinanceRepository

class CreateFinancialMonthUseCase(
    private val financeRepository: FinanceRepository,
) {

    suspend operator fun invoke(
        year: Int,
        monthNumber: Int,
        initialBudget: BigDecimal,
        startedAtMillis: Long,
    ): CreateFinancialMonthResult {
        if (
            year !in
            FinancialMonth.MIN_YEAR..FinancialMonth.MAX_YEAR
        ) {
            return CreateFinancialMonthResult.YearOutOfRange(
                actualYear = year,
                minimumYear = FinancialMonth.MIN_YEAR,
                maximumYear = FinancialMonth.MAX_YEAR,
            )
        }

        if (monthNumber !in 1..12) {
            return CreateFinancialMonthResult.InvalidMonthNumber(
                actualMonthNumber = monthNumber,
            )
        }

        if (initialBudget < BigDecimal.ZERO) {
            return CreateFinancialMonthResult.NegativeInitialBudget(
                initialBudget = initialBudget,
            )
        }

        if (startedAtMillis <= 0L) {
            return CreateFinancialMonthResult.InvalidStartedAtMillis(
                startedAtMillis = startedAtMillis,
            )
        }

        val activeFinancialMonth =
            financeRepository
                .observeActiveFinancialMonth()
                .first()

        if (activeFinancialMonth != null) {
            return CreateFinancialMonthResult
                .ActiveFinancialMonthAlreadyExists(
                    activeFinancialMonthId =
                        activeFinancialMonth.id,
                )
        }

        val financialMonths =
            financeRepository
                .observeFinancialMonths()
                .first()

        val existingFinancialMonth =
            financialMonths.firstOrNull { financialMonth ->
                financialMonth.year == year &&
                        financialMonth.monthNumber == monthNumber
            }

        if (existingFinancialMonth != null) {
            return CreateFinancialMonthResult
                .FinancialMonthAlreadyExists(
                    existingFinancialMonthId =
                        existingFinancialMonth.id,
                    year = existingFinancialMonth.year,
                    monthNumber =
                        existingFinancialMonth.monthNumber,
                )
        }

        val financialMonth = FinancialMonth(
            year = year,
            monthNumber = monthNumber,
            initialBudget = initialBudget,
            startedAtMillis = startedAtMillis,
        )

        val financialMonthId =
            financeRepository.addFinancialMonth(
                financialMonth = financialMonth,
            )

        return CreateFinancialMonthResult.Success(
            financialMonthId = financialMonthId,
        )
    }
}