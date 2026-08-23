package ru.lilnaro.finflow.domain.usecase

import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale
import ru.lilnaro.finflow.domain.model.FinancialAnalysisCategory
import ru.lilnaro.finflow.domain.model.FinancialAnalysisConfidence
import ru.lilnaro.finflow.domain.model.FinancialAnalysisContext
import ru.lilnaro.finflow.domain.model.FinancialAnalysisMonth
import ru.lilnaro.finflow.domain.model.FinancialAnalysisTransaction
import ru.lilnaro.finflow.domain.model.FinancialCategoryBreakdown
import ru.lilnaro.finflow.domain.model.FinancialMonthChange
import ru.lilnaro.finflow.domain.model.FinancialMonthReport
import ru.lilnaro.finflow.domain.model.FinancialReportTransaction
import ru.lilnaro.finflow.domain.model.FinancialSemanticTransactionContext

class BuildFinancialAnalysisContextUseCase {

    operator fun invoke(
        reports: List<FinancialMonthReport>,
    ): FinancialAnalysisContext {
        val orderedReports =
            reports.sortedWith(
                compareBy<FinancialMonthReport>(
                    { report ->
                        report.financialMonth.year
                    },
                    { report ->
                        report.financialMonth.monthNumber
                    },
                ),
            )

        val months =
            orderedReports.map { report ->
                report.toAnalysisMonth()
            }

        val focusMonthId =
            orderedReports
                .firstOrNull { report ->
                    !report.financialMonth.isClosed
                }
                ?.financialMonth
                ?.id
                ?: orderedReports
                    .lastOrNull()
                    ?.financialMonth
                    ?.id

        val closedReports =
            orderedReports.filter { report ->
                report.financialMonth.isClosed
            }

        val closedMonthChanges =
            closedReports
                .zipWithNext { previous, current ->
                    createClosedMonthChange(
                        previous = previous,
                        current = current,
                    )
                }

        val semanticTransactions =
            orderedReports.flatMap { report ->
                report.transactions
                    .asSequence()
                    .filter { transaction ->
                        transaction.isCustomCategory &&
                                transaction.categoryName != null &&
                                isOtherCategory(
                                    transaction.parentCategoryName,
                                )
                    }
                    .map { transaction ->
                        FinancialSemanticTransactionContext(
                            financialMonthId =
                                report.financialMonth.id,
                            transactionId =
                                transaction.id,
                            type = transaction.type,
                            amount = transaction.amount,
                            customCategoryName =
                                requireNotNull(
                                    transaction.categoryName,
                                ),
                            parentCategoryName =
                                requireNotNull(
                                    transaction.parentCategoryName,
                                ),
                            note =
                                transaction.note
                                    .trim()
                                    .takeIf { note ->
                                        note.isNotEmpty()
                                    },
                            createdAtMillis =
                                transaction.createdAtMillis,
                        )
                    }
                    .toList()
            }

        val totalTransactionCount =
            orderedReports.sumOf { report ->
                report.transactionCount
            }

        return FinancialAnalysisContext(
            focusMonthId = focusMonthId,
            months = months,
            closedMonthChanges = closedMonthChanges,
            semanticTransactions =
                semanticTransactions,
            availableMonthCount =
                orderedReports.size,
            totalTransactionCount =
                totalTransactionCount,
            confidence =
                calculateConfidence(
                    monthCount =
                        orderedReports.size,
                    transactionCount =
                        totalTransactionCount,
                ),
        )
    }

    private fun FinancialMonthReport.toAnalysisMonth():
            FinancialAnalysisMonth {
        return FinancialAnalysisMonth(
            financialMonthId =
                financialMonth.id,
            year = financialMonth.year,
            monthNumber =
                financialMonth.monthNumber,
            isClosed =
                financialMonth.isClosed,
            startedAtMillis =
                financialMonth.startedAtMillis,
            closedAtMillis =
                financialMonth.closedAtMillis,
            initialBudget = initialBudget,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            finalBalance = finalBalance,
            transactionCount =
                transactionCount,
            expenseBreakdown =
                expenseBreakdown.map { category ->
                    category.toAnalysisCategory()
                },
            incomeBreakdown =
                incomeBreakdown.map { category ->
                    category.toAnalysisCategory()
                },
            transactions =
                transactions.map { transaction ->
                    transaction.toAnalysisTransaction()
                },
        )
    }

    private fun FinancialCategoryBreakdown
            .toAnalysisCategory():
            FinancialAnalysisCategory {
        return FinancialAnalysisCategory(
            categoryId = categoryId,
            categoryName = categoryName,
            isCustomCategory =
                isCustomCategory,
            parentCategoryName =
                parentCategoryName,
            amount = amount,
            transactionCount =
                transactionCount,
            sharePercent = sharePercent,
        )
    }

    private fun FinancialReportTransaction
            .toAnalysisTransaction():
            FinancialAnalysisTransaction {
        return FinancialAnalysisTransaction(
            transactionId = id,
            type = type,
            amount = amount,
            categoryId = categoryId,
            categoryName = categoryName,
            isCustomCategory =
                isCustomCategory,
            parentCategoryName =
                parentCategoryName,
            createdAtMillis =
                createdAtMillis,
        )
    }

    private fun createClosedMonthChange(
        previous: FinancialMonthReport,
        current: FinancialMonthReport,
    ): FinancialMonthChange {
        return FinancialMonthChange(
            previousFinancialMonthId =
                previous.financialMonth.id,
            currentFinancialMonthId =
                current.financialMonth.id,
            incomeDelta =
                current.totalIncome
                    .subtract(
                        previous.totalIncome,
                    ),
            incomeChangePercent =
                calculateChangePercent(
                    previous =
                        previous.totalIncome,
                    current =
                        current.totalIncome,
                ),
            expenseDelta =
                current.totalExpense
                    .subtract(
                        previous.totalExpense,
                    ),
            expenseChangePercent =
                calculateChangePercent(
                    previous =
                        previous.totalExpense,
                    current =
                        current.totalExpense,
                ),
            finalBalanceDelta =
                current.finalBalance
                    .subtract(
                        previous.finalBalance,
                    ),
        )
    }

    private fun calculateChangePercent(
        previous: BigDecimal,
        current: BigDecimal,
    ): Double? {
        if (
            previous.compareTo(
                BigDecimal.ZERO,
            ) == 0
        ) {
            return null
        }

        return current
            .subtract(previous)
            .divide(
                previous,
                MathContext.DECIMAL64,
            )
            .multiply(PERCENT_MULTIPLIER)
            .toDouble()
    }

    private fun isOtherCategory(
        categoryName: String?,
    ): Boolean {
        return categoryName
            ?.trim()
            ?.lowercase(Locale.ROOT) ==
                OTHER_CATEGORY_NAME
    }

    private fun calculateConfidence(
        monthCount: Int,
        transactionCount: Int,
    ): FinancialAnalysisConfidence {
        return when {
            monthCount >= HIGH_CONFIDENCE_MONTHS &&
                    transactionCount >=
                    HIGH_CONFIDENCE_TRANSACTIONS -> {
                FinancialAnalysisConfidence.HIGH
            }

            monthCount >= MEDIUM_CONFIDENCE_MONTHS &&
                    transactionCount >=
                    MEDIUM_CONFIDENCE_TRANSACTIONS -> {
                FinancialAnalysisConfidence.MEDIUM
            }

            else -> {
                FinancialAnalysisConfidence.LOW
            }
        }
    }

    private companion object {

        const val OTHER_CATEGORY_NAME =
            "другое"

        const val MEDIUM_CONFIDENCE_MONTHS =
            2

        const val MEDIUM_CONFIDENCE_TRANSACTIONS =
            10

        const val HIGH_CONFIDENCE_MONTHS =
            4

        const val HIGH_CONFIDENCE_TRANSACTIONS =
            40

        val PERCENT_MULTIPLIER =
            BigDecimal("100")
    }
}