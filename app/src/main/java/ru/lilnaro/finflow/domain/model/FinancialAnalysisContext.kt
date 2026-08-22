package ru.lilnaro.finflow.domain.model

import java.math.BigDecimal

data class FinancialAnalysisContext(
    val focusMonthId: Long?,
    val months: List<FinancialAnalysisMonth>,
    val closedMonthChanges: List<FinancialMonthChange>,
    val semanticTransactions:
    List<FinancialSemanticTransactionContext>,
    val availableMonthCount: Int,
    val totalTransactionCount: Int,
    val confidence: FinancialAnalysisConfidence,
)

data class FinancialAnalysisMonth(
    val financialMonthId: Long,
    val year: Int,
    val monthNumber: Int,
    val isClosed: Boolean,
    val startedAtMillis: Long,
    val closedAtMillis: Long?,
    val initialBudget: BigDecimal,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val finalBalance: BigDecimal,
    val transactionCount: Int,
    val expenseBreakdown:
    List<FinancialAnalysisCategory>,
    val incomeBreakdown:
    List<FinancialAnalysisCategory>,
    val transactions:
    List<FinancialAnalysisTransaction>,
)

data class FinancialAnalysisCategory(
    val categoryId: Long,
    val categoryName: String?,
    val isCustomCategory: Boolean,
    val parentCategoryName: String?,
    val amount: BigDecimal,
    val transactionCount: Int,
    val sharePercent: Double,
)

data class FinancialAnalysisTransaction(
    val transactionId: Long,
    val type: TransactionType,
    val amount: BigDecimal,
    val categoryId: Long,
    val categoryName: String?,
    val isCustomCategory: Boolean,
    val parentCategoryName: String?,
    val createdAtMillis: Long,
)

data class FinancialMonthChange(
    val previousFinancialMonthId: Long,
    val currentFinancialMonthId: Long,
    val incomeDelta: BigDecimal,
    val incomeChangePercent: Double?,
    val expenseDelta: BigDecimal,
    val expenseChangePercent: Double?,
    val finalBalanceDelta: BigDecimal,
)

data class FinancialSemanticTransactionContext(
    val financialMonthId: Long,
    val transactionId: Long,
    val type: TransactionType,
    val amount: BigDecimal,
    val customCategoryName: String,
    val parentCategoryName: String,
    val note: String?,
    val createdAtMillis: Long,
)

enum class FinancialAnalysisConfidence {
    LOW,
    MEDIUM,
    HIGH,
}