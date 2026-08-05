package ru.lilnaro.finflow.domain.model

import java.math.BigDecimal
import java.math.MathContext

data class MonthSummary(
    val financialMonthId: Long,
    val initialBudget: BigDecimal,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
) {

    val currentBalance: BigDecimal
        get() = initialBudget +
                totalIncome -
                totalExpense

    val balanceChangePercent: Double?
        get() {
            if (initialBudget.signum() == 0) {
                return null
            }

            val balanceDifference =
                currentBalance - initialBudget

            return balanceDifference
                .divide(initialBudget, MathContext.DECIMAL64)
                .multiply(PERCENT_MULTIPLIER)
                .toDouble()
        }

    init {
        require(financialMonthId > 0L) {
            "Идентификатор финансового месяца должен быть положительным"
        }

        require(initialBudget >= BigDecimal.ZERO) {
            "Начальный бюджет не может быть отрицательным"
        }

        require(totalIncome >= BigDecimal.ZERO) {
            "Общая сумма доходов не может быть отрицательной"
        }

        require(totalExpense >= BigDecimal.ZERO) {
            "Общая сумма расходов не может быть отрицательной"
        }
    }

    private companion object {
        val PERCENT_MULTIPLIER: BigDecimal = BigDecimal("100")
    }
}