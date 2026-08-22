package ru.lilnaro.finflow.domain.model

import java.math.BigDecimal

data class FinancialMonthReport(
    val financialMonth: FinancialMonth,
    val initialBudget: BigDecimal,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val finalBalance: BigDecimal,
    val transactionCount: Int,
    val transactions: List<FinancialReportTransaction>,
    val expenseBreakdown: List<FinancialCategoryBreakdown>,
    val incomeBreakdown: List<FinancialCategoryBreakdown>,
)

data class FinancialCategoryBreakdown(
    val categoryId: Long,
    val categoryName: String?,
    val isCustomCategory: Boolean,
    val parentCategoryId: Long?,
    val parentCategoryName: String?,
    val amount: BigDecimal,
    val transactionCount: Int,
    val sharePercent: Double,
)

data class FinancialReportTransaction(
    val id: Long,
    val amount: BigDecimal,
    val type: TransactionType,
    val categoryId: Long,
    val categoryName: String?,
    val isCustomCategory: Boolean,
    val parentCategoryId: Long?,
    val parentCategoryName: String?,
    val note: String,
    val createdAtMillis: Long,
)