package ru.lilnaro.finflow.domain.usecase

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

        var totalIncomeInKopecks = 0L
        var totalExpenseInKopecks = 0L

        transactions.forEach { transaction ->
            require(
                transaction.financialMonthId == financialMonth.id,
            ) {
                "Транзакция ${transaction.id} относится к другому финансовому месяцу"
            }

            when (transaction.type) {
                TransactionType.INCOME -> {
                    totalIncomeInKopecks +=
                        transaction.amountInKopecks
                }

                TransactionType.EXPENSE -> {
                    totalExpenseInKopecks +=
                        transaction.amountInKopecks
                }
            }
        }

        return MonthSummary(
            financialMonthId = financialMonth.id,
            initialBudgetInKopecks =
                financialMonth.initialBudgetInKopecks,
            totalIncomeInKopecks = totalIncomeInKopecks,
            totalExpenseInKopecks = totalExpenseInKopecks,
        )
    }
}