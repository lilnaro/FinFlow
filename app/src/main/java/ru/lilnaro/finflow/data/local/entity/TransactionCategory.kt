package ru.lilnaro.finflow.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.lilnaro.finflow.domain.model.TransactionType

@Entity(
    tableName = "transaction_categories",
    foreignKeys = [
        ForeignKey(
            entity = TransactionCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["type"]),
        Index(value = ["parent_category_id"]),
    ],
)
data class TransactionCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val name: String,

    val type: TransactionType,

    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean,

    @ColumnInfo(name = "parent_category_id")
    val parentCategoryId: Long? = null,
)