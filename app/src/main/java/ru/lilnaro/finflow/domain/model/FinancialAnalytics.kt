package ru.lilnaro.finflow.domain.model

import java.math.BigDecimal

data class FinancialAnalyticsSnapshot(
    val generatedAtMillis: Long,
    val confidence: FinancialAnalysisConfidence,
    val focusMonth:
    FinancialFocusMonthAnalytics?,
    val history: FinancialHistoryAnalytics,
    val expenseCategoryChanges:
    List<FinancialCategoryChange>,
    val incomeCategoryChanges:
    List<FinancialCategoryChange>,
    val largeTransactionSignals:
    List<FinancialLargeTransactionSignal>,
    val semanticTransactions:
    List<FinancialSemanticTransactionContext>,
)

data class FinancialFocusMonthAnalytics(
    val financialMonthId: Long,
    val year: Int,
    val monthNumber: Int,
    val isClosed: Boolean,
    val currentBalance: BigDecimal,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val topExpenseCategory:
    FinancialAnalysisCategory?,
    val topIncomeCategory:
    FinancialAnalysisCategory?,
    val pace: FinancialMonthPace?,
)

data class FinancialMonthPace(
    val elapsedDays: Int,
    val totalPeriodDays: Int,
    val remainingDays: Int,
    val averageDailyIncome: BigDecimal,
    val averageDailyExpense: BigDecimal,
    val projectedIncome: BigDecimal,
    val projectedExpense: BigDecimal,
    val projectedFinalBalance: BigDecimal,
)

data class FinancialHistoryAnalytics(
    val closedMonthCount: Int,
    val averageMonthlyIncome: BigDecimal?,
    val averageMonthlyExpense: BigDecimal?,
    val averageFinalBalance: BigDecimal?,
    val incomeVolatilityPercent: Double?,
    val expenseVolatilityPercent: Double?,
    val latestClosedMonthChange:
    FinancialMonthChange?,
)

data class FinancialCategoryChange(
    val categoryId: Long,
    val categoryName: String?,
    val isCustomCategory: Boolean,
    val parentCategoryName: String?,
    val previousAmount: BigDecimal,
    val currentAmount: BigDecimal,
    val delta: BigDecimal,
    val changePercent: Double?,
    val direction: FinancialTrendDirection,
)

data class FinancialLargeTransactionSignal(
    val financialMonthId: Long,
    val transactionId: Long,
    val type: TransactionType,
    val amount: BigDecimal,
    val categoryName: String?,
    val parentCategoryName: String?,
    val monthlyTypeSharePercent: Double,
    val medianMultiplier: Double,
    val createdAtMillis: Long,
)

enum class FinancialTrendDirection {
    UP,
    DOWN,
    STABLE,
}