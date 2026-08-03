package ru.lilnaro.finflow.domain.usecase

sealed interface CloseFinancialMonthResult {

    data class Success(
        val financialMonthId: Long,
        val closedAtMillis: Long,
    ) : CloseFinancialMonthResult

    data class InvalidFinancialMonthId(
        val financialMonthId: Long,
    ) : CloseFinancialMonthResult

    data class InvalidClosedAtMillis(
        val closedAtMillis: Long,
    ) : CloseFinancialMonthResult

    data class FinancialMonthNotFound(
        val financialMonthId: Long,
    ) : CloseFinancialMonthResult

    data class FinancialMonthAlreadyClosed(
        val financialMonthId: Long,
        val existingClosedAtMillis: Long,
    ) : CloseFinancialMonthResult

    data class ClosingTimeBeforeMonthStart(
        val startedAtMillis: Long,
        val closedAtMillis: Long,
    ) : CloseFinancialMonthResult
}