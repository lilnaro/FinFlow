package ru.lilnaro.finflow.domain.model.result

import java.math.BigDecimal

sealed interface CreateFinancialMonthResult {

    data class Success(
        val financialMonthId: Long,
    ) : CreateFinancialMonthResult

    data class YearOutOfRange(
        val actualYear: Int,
        val minimumYear: Int,
        val maximumYear: Int,
    ) : CreateFinancialMonthResult

    data class InvalidMonthNumber(
        val actualMonthNumber: Int,
    ) : CreateFinancialMonthResult

    data class NegativeInitialBudget(
        val initialBudget: BigDecimal,
    ) : CreateFinancialMonthResult

    data class InvalidStartedAtMillis(
        val startedAtMillis: Long,
    ) : CreateFinancialMonthResult

    data class ActiveFinancialMonthAlreadyExists(
        val activeFinancialMonthId: Long,
    ) : CreateFinancialMonthResult

    data class FinancialMonthAlreadyExists(
        val existingFinancialMonthId: Long,
        val year: Int,
        val monthNumber: Int,
    ) : CreateFinancialMonthResult
}