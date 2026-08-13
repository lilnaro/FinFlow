package ru.lilnaro.finflow.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "financial_months",
    indices = [
        Index(
            value = ["year", "month_number"],
            unique = true,
        ),
    ],
)
data class FinancialMonthEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val year: Int,

    @ColumnInfo(name = "month_number")
    val monthNumber: Int,

    @ColumnInfo(name = "initial_budget")
    val initialBudget: BigDecimal,

    @ColumnInfo(name = "started_at_millis")
    val startedAtMillis: Long,

    @ColumnInfo(name = "closed_at_millis")
    val closedAtMillis: Long? = null,
)