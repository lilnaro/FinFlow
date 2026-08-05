package ru.lilnaro.finflow.domain.usecase

import java.math.BigDecimal
import ru.lilnaro.finflow.domain.model.FinancialMonth
import ru.lilnaro.finflow.domain.model.MonthSummary
import ru.lilnaro.finflow.domain.model.Transaction
import ru.lilnaro.finflow.domain.model.TransactionType

class CalculateMonthSummaryUseCase {

    operator fun invoke(
        financialMonth: FinancialMonth,
        transactions: List<Transaction>,
    ): MonthSummary {
        require(financialMonth.id > 0L) {
            "Для расчёта итогов финансовый месяц должен быть сохранён"
        }

        var totalIncome = BigDecimal.ZERO
        var totalExpense = BigDecimal.ZERO

        transactions.forEach { transaction ->
            require(
                transaction.financialMonthId == financialMonth.id,
            ) {
                "Транзакция ${transaction.id} относится к другому финансовому месяцу"
            }

            when (transaction.type) {
                TransactionType.INCOME -> {
                    totalIncome += transaction.amount
                }

                TransactionType.EXPENSE -> {
                    totalExpense += transaction.amount
                }
            }
        }

        return MonthSummary(
            financialMonthId = financialMonth.id,
            initialBudget = financialMonth.initialBudget,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
        )
    }
}