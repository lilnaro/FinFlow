package ru.lilnaro.finflow.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import ru.lilnaro.finflow.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = FinancialMonthEntity::class,
            parentColumns = ["id"],
            childColumns = ["financial_month_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = TransactionCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["financial_month_id"]),
        Index(value = ["category_id"]),
        Index(value = ["type"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "financial_month_id")
    val financialMonthId: Long,

    val amount: BigDecimal,

    val type: TransactionType,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    val note: String = "",

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long,
)