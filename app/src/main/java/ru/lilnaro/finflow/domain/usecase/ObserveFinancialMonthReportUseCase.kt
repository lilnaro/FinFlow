package ru.lilnaro.finflow.domain.usecase

import java.math.BigDecimal
import java.math.MathContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import ru.lilnaro.finflow.domain.model.FinancialCategoryBreakdown
import ru.lilnaro.finflow.domain.model.FinancialMonthReport
import ru.lilnaro.finflow.domain.model.FinancialReportTransaction
import ru.lilnaro.finflow.domain.model.Transaction
import ru.lilnaro.finflow.domain.model.TransactionCategory
import ru.lilnaro.finflow.domain.model.TransactionType
import ru.lilnaro.finflow.domain.repository.FinanceRepository

class ObserveFinancialMonthReportUseCase(
    private val financeRepository: FinanceRepository,
    private val observeTransactionsByMonthUseCase:
    ObserveTransactionsByMonthUseCase,
    private val observeCategoriesByTypeUseCase:
    ObserveCategoriesByTypeUseCase,
    private val calculateMonthSummaryUseCase:
    CalculateMonthSummaryUseCase,
) {

    operator fun invoke(
        financialMonthId: Long,
    ): Flow<FinancialMonthReport?> {
        require(financialMonthId > 0L) {
            "Идентификатор финансового месяца должен быть положительным"
        }

        return combine(
            financeRepository.observeFinancialMonths(),
            observeTransactionsByMonthUseCase(
                financialMonthId = financialMonthId,
            ),
            observeCategoriesByTypeUseCase(
                type = TransactionType.EXPENSE,
            ),
            observeCategoriesByTypeUseCase(
                type = TransactionType.INCOME,
            ),
        ) {
                financialMonths,
                transactions,
                expenseCategories,
                incomeCategories,
            ->

            val financialMonth =
                financialMonths.firstOrNull { month ->
                    month.id == financialMonthId
                }
                    ?: return@combine null

            val categories =
                expenseCategories + incomeCategories

            val categoriesById =
                categories.associateBy { category ->
                    category.id
                }

            val monthSummary =
                calculateMonthSummaryUseCase(
                    financialMonth = financialMonth,
                    transactions = transactions,
                )

            val finalBalance =
                monthSummary.initialBudget
                    .add(monthSummary.totalIncome)
                    .subtract(monthSummary.totalExpense)

            FinancialMonthReport(
                financialMonth = financialMonth,
                initialBudget = monthSummary.initialBudget,
                totalIncome = monthSummary.totalIncome,
                totalExpense = monthSummary.totalExpense,
                finalBalance = finalBalance,
                transactionCount = transactions.size,
                transactions =
                    createTransactions(
                        transactions = transactions,
                        categoriesById = categoriesById,
                    ),
                expenseBreakdown =
                    createCategoryBreakdown(
                        transactions = transactions,
                        categoriesById = categoriesById,
                        type = TransactionType.EXPENSE,
                        typeTotal = monthSummary.totalExpense,
                    ),
                incomeBreakdown =
                    createCategoryBreakdown(
                        transactions = transactions,
                        categoriesById = categoriesById,
                        type = TransactionType.INCOME,
                        typeTotal = monthSummary.totalIncome,
                    ),
            )
        }
            .distinctUntilChanged()
    }

    private fun createTransactions(
        transactions: List<Transaction>,
        categoriesById: Map<Long, TransactionCategory>,
    ): List<FinancialReportTransaction> {
        return transactions
            .sortedByDescending { transaction ->
                transaction.createdAtMillis
            }
            .map { transaction ->
                val category =
                    categoriesById[transaction.categoryId]

                FinancialReportTransaction(
                    id = transaction.id,
                    amount = transaction.amount,
                    type = transaction.type,
                    categoryId = transaction.categoryId,
                    categoryName = category?.name,
                    isCustomCategory =
                        category?.isCustom == true,
                    note = transaction.note,
                    createdAtMillis =
                        transaction.createdAtMillis,
                )
            }
    }

    private fun createCategoryBreakdown(
        transactions: List<Transaction>,
        categoriesById: Map<Long, TransactionCategory>,
        type: TransactionType,
        typeTotal: BigDecimal,
    ): List<FinancialCategoryBreakdown> {
        return transactions
            .asSequence()
            .filter { transaction ->
                transaction.type == type
            }
            .groupBy { transaction ->
                transaction.categoryId
            }
            .map { (categoryId, categoryTransactions) ->
                val amount =
                    categoryTransactions.fold(
                        BigDecimal.ZERO,
                    ) { total, transaction ->
                        total.add(transaction.amount)
                    }

                val sharePercent =
                    calculateSharePercent(
                        amount = amount,
                        total = typeTotal,
                    )

                val category =
                    categoriesById[categoryId]

                FinancialCategoryBreakdown(
                    categoryId = categoryId,
                    categoryName = category?.name,
                    isCustomCategory =
                        category?.isCustom == true,
                    amount = amount,
                    transactionCount =
                        categoryTransactions.size,
                    sharePercent = sharePercent,
                )
            }
            .sortedByDescending { breakdown ->
                breakdown.amount
            }
    }

    private fun calculateSharePercent(
        amount: BigDecimal,
        total: BigDecimal,
    ): Double {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0
        }

        return amount
            .divide(
                total,
                MathContext.DECIMAL64,
            )
            .multiply(PERCENT_MULTIPLIER)
            .toDouble()
    }

    private companion object {

        val PERCENT_MULTIPLIER =
            BigDecimal("100")
    }
}