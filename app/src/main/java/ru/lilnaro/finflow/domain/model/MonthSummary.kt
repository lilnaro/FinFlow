package ru.lilnaro.finflow.domain.model

data class MonthSummary(
    val financialMonthId: Long,
    val initialBudgetInKopecks: Long,
    val totalIncomeInKopecks: Long,
    val totalExpenseInKopecks: Long,
) {
        val currentBalanceInKopecks: Long
        get() = initialBudgetInKopecks +
                totalIncomeInKopecks -
                totalExpenseInKopecks

    val balanceChangePercent: Double?
        get() {
            if (initialBudgetInKopecks == 0L) {
                return null
            }

            val balanceDifference =
                currentBalanceInKopecks - initialBudgetInKopecks

            return balanceDifference.toDouble() /
                    initialBudgetInKopecks.toDouble() *
                    100.0
        }

    init {
        require(financialMonthId > 0L) {
            "Идентификатор финансового месяца должен быть положительным"
        }

        require(initialBudgetInKopecks >= 0L) {
            "Начальный бюджет не может быть отрицательным"
        }

        require(totalIncomeInKopecks >= 0L) {
            "Общая сумма доходов не может быть отрицательной"
        }

        require(totalExpenseInKopecks >= 0L) {
            "Общая сумма расходов не может быть отрицательной"
        }
    }
}