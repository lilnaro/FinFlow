package ru.lilnaro.finflow.domain.usecase

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Calendar
import kotlin.math.sqrt
import ru.lilnaro.finflow.domain.model.FinancialAnalysisCategory
import ru.lilnaro.finflow.domain.model.FinancialAnalysisContext
import ru.lilnaro.finflow.domain.model.FinancialAnalysisMonth
import ru.lilnaro.finflow.domain.model.FinancialAnalysisTransaction
import ru.lilnaro.finflow.domain.model.FinancialAnalyticsSnapshot
import ru.lilnaro.finflow.domain.model.FinancialCategoryChange
import ru.lilnaro.finflow.domain.model.FinancialFocusMonthAnalytics
import ru.lilnaro.finflow.domain.model.FinancialHistoryAnalytics
import ru.lilnaro.finflow.domain.model.FinancialLargeTransactionSignal
import ru.lilnaro.finflow.domain.model.FinancialMonthPace
import ru.lilnaro.finflow.domain.model.FinancialTrendDirection
import ru.lilnaro.finflow.domain.model.TransactionType

class AnalyzeFinancialContextUseCase {

    operator fun invoke(
        context: FinancialAnalysisContext,
        nowMillis: Long,
    ): FinancialAnalyticsSnapshot {
        require(nowMillis > 0L) {
            "Текущее время должно быть положительным"
        }

        val focusMonth =
            context.focusMonthId?.let { focusMonthId ->
                context.months.firstOrNull { month ->
                    month.financialMonthId ==
                            focusMonthId
                }
            }

        val closedMonths =
            context.months.filter { month ->
                month.isClosed
            }

        return FinancialAnalyticsSnapshot(
            generatedAtMillis = nowMillis,
            confidence = context.confidence,
            focusMonth =
                focusMonth?.let { month ->
                    createFocusMonthAnalytics(
                        month = month,
                        nowMillis = nowMillis,
                    )
                },
            history =
                createHistoryAnalytics(
                    closedMonths = closedMonths,
                    latestClosedMonthChange =
                        context.closedMonthChanges
                            .lastOrNull(),
                ),
            expenseCategoryChanges =
                createLatestClosedCategoryChanges(
                    closedMonths = closedMonths,
                    type = TransactionType.EXPENSE,
                ),
            incomeCategoryChanges =
                createLatestClosedCategoryChanges(
                    closedMonths = closedMonths,
                    type = TransactionType.INCOME,
                ),
            largeTransactionSignals =
                createLargeTransactionSignals(
                    months = context.months,
                ),
            semanticTransactions =
                context.semanticTransactions,
        )
    }

    private fun createFocusMonthAnalytics(
        month: FinancialAnalysisMonth,
        nowMillis: Long,
    ): FinancialFocusMonthAnalytics {
        return FinancialFocusMonthAnalytics(
            financialMonthId =
                month.financialMonthId,
            year = month.year,
            monthNumber = month.monthNumber,
            isClosed = month.isClosed,
            currentBalance =
                month.finalBalance,
            totalIncome =
                month.totalIncome,
            totalExpense =
                month.totalExpense,
            topExpenseCategory =
                month.expenseBreakdown
                    .maxByOrNull { category ->
                        category.amount
                    },
            topIncomeCategory =
                month.incomeBreakdown
                    .maxByOrNull { category ->
                        category.amount
                    },
            pace =
                createMonthPace(
                    month = month,
                    nowMillis = nowMillis,
                ),
        )
    }

    private fun createMonthPace(
        month: FinancialAnalysisMonth,
        nowMillis: Long,
    ): FinancialMonthPace? {
        if (month.isClosed) {
            return null
        }

        val periodEndMillis =
            createMonthEndMillis(
                year = month.year,
                monthNumber = month.monthNumber,
            )

        if (
            nowMillis < month.startedAtMillis ||
            month.startedAtMillis >
            periodEndMillis
        ) {
            return null
        }

        val effectiveNowMillis =
            nowMillis.coerceAtMost(
                periodEndMillis,
            )

        val elapsedDays =
            countCalendarDaysInclusive(
                startMillis =
                    month.startedAtMillis,
                endMillis =
                    effectiveNowMillis,
            )

        val totalPeriodDays =
            countCalendarDaysInclusive(
                startMillis =
                    month.startedAtMillis,
                endMillis =
                    periodEndMillis,
            )

        if (
            elapsedDays <= 0 ||
            totalPeriodDays <= 0
        ) {
            return null
        }

        val remainingDays =
            (totalPeriodDays - elapsedDays)
                .coerceAtLeast(0)

        val averageDailyIncome =
            divideMoney(
                value = month.totalIncome,
                divisor = elapsedDays,
            )

        val averageDailyExpense =
            divideMoney(
                value = month.totalExpense,
                divisor = elapsedDays,
            )

        val projectedIncome =
            projectMoney(
                value = month.totalIncome,
                elapsedDays = elapsedDays,
                totalPeriodDays =
                    totalPeriodDays,
            )

        val projectedExpense =
            projectMoney(
                value = month.totalExpense,
                elapsedDays = elapsedDays,
                totalPeriodDays =
                    totalPeriodDays,
            )

        val projectedFinalBalance =
            month.initialBudget
                .add(projectedIncome)
                .subtract(projectedExpense)
                .setScale(
                    MONEY_SCALE,
                    RoundingMode.HALF_UP,
                )

        return FinancialMonthPace(
            elapsedDays = elapsedDays,
            totalPeriodDays =
                totalPeriodDays,
            remainingDays =
                remainingDays,
            averageDailyIncome =
                averageDailyIncome,
            averageDailyExpense =
                averageDailyExpense,
            projectedIncome =
                projectedIncome,
            projectedExpense =
                projectedExpense,
            projectedFinalBalance =
                projectedFinalBalance,
        )
    }

    private fun createHistoryAnalytics(
        closedMonths: List<FinancialAnalysisMonth>,
        latestClosedMonthChange:
        ru.lilnaro.finflow.domain.model.FinancialMonthChange?,
    ): FinancialHistoryAnalytics {
        if (closedMonths.isEmpty()) {
            return FinancialHistoryAnalytics(
                closedMonthCount = 0,
                averageMonthlyIncome = null,
                averageMonthlyExpense = null,
                averageFinalBalance = null,
                incomeVolatilityPercent = null,
                expenseVolatilityPercent = null,
                latestClosedMonthChange = null,
            )
        }

        return FinancialHistoryAnalytics(
            closedMonthCount =
                closedMonths.size,
            averageMonthlyIncome =
                averageMoney(
                    values =
                        closedMonths.map { month ->
                            month.totalIncome
                        },
                ),
            averageMonthlyExpense =
                averageMoney(
                    values =
                        closedMonths.map { month ->
                            month.totalExpense
                        },
                ),
            averageFinalBalance =
                averageMoney(
                    values =
                        closedMonths.map { month ->
                            month.finalBalance
                        },
                ),
            incomeVolatilityPercent =
                calculateVolatilityPercent(
                    values =
                        closedMonths.map { month ->
                            month.totalIncome
                        },
                ),
            expenseVolatilityPercent =
                calculateVolatilityPercent(
                    values =
                        closedMonths.map { month ->
                            month.totalExpense
                        },
                ),
            latestClosedMonthChange =
                latestClosedMonthChange,
        )
    }

    private fun createLatestClosedCategoryChanges(
        closedMonths: List<FinancialAnalysisMonth>,
        type: TransactionType,
    ): List<FinancialCategoryChange> {
        if (closedMonths.size < 2) {
            return emptyList()
        }

        val previousMonth =
            closedMonths[
                closedMonths.lastIndex - 1
            ]

        val currentMonth =
            closedMonths.last()

        val previousCategories =
            breakdownForType(
                month = previousMonth,
                type = type,
            ).associateBy { category ->
                category.categoryId
            }

        val currentCategories =
            breakdownForType(
                month = currentMonth,
                type = type,
            ).associateBy { category ->
                category.categoryId
            }

        val categoryIds =
            (previousCategories.keys +
                    currentCategories.keys)
                .distinct()

        return categoryIds
            .map { categoryId ->
                val previous =
                    previousCategories[
                        categoryId
                    ]

                val current =
                    currentCategories[
                        categoryId
                    ]

                val previousAmount =
                    previous?.amount
                        ?: BigDecimal.ZERO

                val currentAmount =
                    current?.amount
                        ?: BigDecimal.ZERO

                val delta =
                    currentAmount
                        .subtract(
                            previousAmount,
                        )

                val referenceCategory =
                    current ?: previous

                FinancialCategoryChange(
                    categoryId = categoryId,
                    categoryName =
                        referenceCategory
                            ?.categoryName,
                    isCustomCategory =
                        referenceCategory
                            ?.isCustomCategory ==
                                true,
                    parentCategoryName =
                        referenceCategory
                            ?.parentCategoryName,
                    previousAmount =
                        previousAmount,
                    currentAmount =
                        currentAmount,
                    delta = delta,
                    changePercent =
                        calculateChangePercent(
                            previous =
                                previousAmount,
                            current =
                                currentAmount,
                        ),
                    direction =
                        when {
                            delta >
                                    CHANGE_STABILITY_EPSILON -> {
                                FinancialTrendDirection.UP
                            }

                            delta <
                                    CHANGE_STABILITY_EPSILON
                                        .negate() -> {
                                FinancialTrendDirection.DOWN
                            }

                            else -> {
                                FinancialTrendDirection.STABLE
                            }
                        },
                )
            }
            .sortedByDescending { change ->
                change.delta.abs()
            }
    }

    private fun createLargeTransactionSignals(
        months: List<FinancialAnalysisMonth>,
    ): List<FinancialLargeTransactionSignal> {
        return months
            .flatMap { month ->
                TransactionType.entries
                    .flatMap { type ->
                        createLargeTransactionSignalsForType(
                            month = month,
                            type = type,
                        )
                    }
            }
            .sortedWith(
                compareByDescending<
                        FinancialLargeTransactionSignal
                        > { signal ->
                    signal.monthlyTypeSharePercent
                }.thenByDescending { signal ->
                    signal.amount
                },
            )
            .take(MAX_LARGE_TRANSACTION_SIGNALS)
    }

    private fun createLargeTransactionSignalsForType(
        month: FinancialAnalysisMonth,
        type: TransactionType,
    ): List<FinancialLargeTransactionSignal> {
        val transactions =
            month.transactions
                .filter { transaction ->
                    transaction.type == type
                }

        if (
            transactions.size <
            MIN_TRANSACTIONS_FOR_LARGE_SIGNAL
        ) {
            return emptyList()
        }

        val total =
            transactions.fold(
                BigDecimal.ZERO,
            ) { sum, transaction ->
                sum.add(transaction.amount)
            }

        if (
            total.compareTo(
                BigDecimal.ZERO,
            ) == 0
        ) {
            return emptyList()
        }

        val median =
            calculateMedianAmount(
                transactions = transactions,
            )

        if (
            median.compareTo(
                BigDecimal.ZERO,
            ) <= 0
        ) {
            return emptyList()
        }

        return transactions
            .mapNotNull { transaction ->
                val medianMultiplier =
                    transaction.amount
                        .divide(
                            median,
                            MathContext.DECIMAL64,
                        )
                        .toDouble()

                val monthlySharePercent =
                    calculateSharePercent(
                        value =
                            transaction.amount,
                        total = total,
                    )

                if (
                    medianMultiplier <
                    LARGE_TRANSACTION_MEDIAN_MULTIPLIER ||
                    monthlySharePercent <
                    LARGE_TRANSACTION_MIN_SHARE_PERCENT
                ) {
                    return@mapNotNull null
                }

                FinancialLargeTransactionSignal(
                    financialMonthId =
                        month.financialMonthId,
                    transactionId =
                        transaction.transactionId,
                    type = transaction.type,
                    amount = transaction.amount,
                    categoryName =
                        transaction.categoryName,
                    parentCategoryName =
                        transaction.parentCategoryName,
                    monthlyTypeSharePercent =
                        monthlySharePercent,
                    medianMultiplier =
                        medianMultiplier,
                    createdAtMillis =
                        transaction.createdAtMillis,
                )
            }
    }

    private fun breakdownForType(
        month: FinancialAnalysisMonth,
        type: TransactionType,
    ): List<FinancialAnalysisCategory> {
        return when (type) {
            TransactionType.EXPENSE -> {
                month.expenseBreakdown
            }

            TransactionType.INCOME -> {
                month.incomeBreakdown
            }
        }
    }

    private fun calculateMedianAmount(
        transactions: List<FinancialAnalysisTransaction>,
    ): BigDecimal {
        val values =
            transactions
                .map { transaction ->
                    transaction.amount
                }
                .sorted()

        val middle =
            values.size / 2

        return if (
            values.size % 2 == 1
        ) {
            values[middle]
        } else {
            values[middle - 1]
                .add(values[middle])
                .divide(
                    TWO,
                    MathContext.DECIMAL64,
                )
        }
    }

    private fun averageMoney(
        values: List<BigDecimal>,
    ): BigDecimal {
        val total =
            values.fold(
                BigDecimal.ZERO,
            ) { sum, value ->
                sum.add(value)
            }

        return total
            .divide(
                BigDecimal(
                    values.size,
                ),
                MathContext.DECIMAL64,
            )
            .setScale(
                MONEY_SCALE,
                RoundingMode.HALF_UP,
            )
    }

    private fun calculateVolatilityPercent(
        values: List<BigDecimal>,
    ): Double? {
        if (values.size < 2) {
            return null
        }

        val doubles =
            values.map { value ->
                value.toDouble()
            }

        val mean =
            doubles.average()

        if (mean == 0.0) {
            return null
        }

        val variance =
            doubles
                .map { value ->
                    val difference =
                        value - mean

                    difference * difference
                }
                .average()

        val standardDeviation =
            sqrt(variance)

        return (
                standardDeviation /
                        kotlin.math.abs(mean) *
                        100.0
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

    private fun calculateSharePercent(
        value: BigDecimal,
        total: BigDecimal,
    ): Double {
        if (
            total.compareTo(
                BigDecimal.ZERO,
            ) == 0
        ) {
            return 0.0
        }

        return value
            .divide(
                total,
                MathContext.DECIMAL64,
            )
            .multiply(PERCENT_MULTIPLIER)
            .toDouble()
    }

    private fun divideMoney(
        value: BigDecimal,
        divisor: Int,
    ): BigDecimal {
        return value
            .divide(
                BigDecimal(divisor),
                MathContext.DECIMAL64,
            )
            .setScale(
                MONEY_SCALE,
                RoundingMode.HALF_UP,
            )
    }

    private fun projectMoney(
        value: BigDecimal,
        elapsedDays: Int,
        totalPeriodDays: Int,
    ): BigDecimal {
        return value
            .divide(
                BigDecimal(elapsedDays),
                MathContext.DECIMAL64,
            )
            .multiply(
                BigDecimal(totalPeriodDays),
            )
            .setScale(
                MONEY_SCALE,
                RoundingMode.HALF_UP,
            )
    }

    private fun createMonthEndMillis(
        year: Int,
        monthNumber: Int,
    ): Long {
        return Calendar
            .getInstance()
            .apply {
                clear()
                set(
                    year,
                    monthNumber - 1,
                    1,
                    23,
                    59,
                    59,
                )
                set(
                    Calendar.MILLISECOND,
                    999,
                )
                set(
                    Calendar.DAY_OF_MONTH,
                    getActualMaximum(
                        Calendar.DAY_OF_MONTH,
                    ),
                )
            }
            .timeInMillis
    }

    private fun countCalendarDaysInclusive(
        startMillis: Long,
        endMillis: Long,
    ): Int {
        if (endMillis < startMillis) {
            return 0
        }

        val cursor =
            Calendar.getInstance().apply {
                timeInMillis =
                    startMillis
                normalizeToStartOfDay()
            }

        val end =
            Calendar.getInstance().apply {
                timeInMillis =
                    endMillis
                normalizeToStartOfDay()
            }

        var count = 0

        while (
            !cursor.after(end)
        ) {
            count += 1
            cursor.add(
                Calendar.DAY_OF_MONTH,
                1,
            )
        }

        return count
    }

    private fun Calendar.normalizeToStartOfDay() {
        set(
            Calendar.HOUR_OF_DAY,
            0,
        )
        set(
            Calendar.MINUTE,
            0,
        )
        set(
            Calendar.SECOND,
            0,
        )
        set(
            Calendar.MILLISECOND,
            0,
        )
    }

    private companion object {

        const val MONEY_SCALE =
            2

        const val MIN_TRANSACTIONS_FOR_LARGE_SIGNAL =
            3

        const val LARGE_TRANSACTION_MEDIAN_MULTIPLIER =
            3.0

        const val LARGE_TRANSACTION_MIN_SHARE_PERCENT =
            20.0

        const val MAX_LARGE_TRANSACTION_SIGNALS =
            8

        val TWO =
            BigDecimal("2")

        val PERCENT_MULTIPLIER =
            BigDecimal("100")

        val CHANGE_STABILITY_EPSILON =
            BigDecimal("0.01")
    }
}