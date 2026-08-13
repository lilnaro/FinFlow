package ru.lilnaro.finflow.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.lilnaro.finflow.data.local.converter.BigDecimalConverters
import ru.lilnaro.finflow.data.local.dao.FinancialMonthDao
import ru.lilnaro.finflow.data.local.dao.TransactionCategoryDao
import ru.lilnaro.finflow.data.local.dao.TransactionDao
import ru.lilnaro.finflow.data.local.entity.FinancialMonthEntity
import ru.lilnaro.finflow.data.local.entity.TransactionCategoryEntity
import ru.lilnaro.finflow.data.local.entity.TransactionEntity

@Database(
    entities = [
        FinancialMonthEntity::class,
        TransactionCategoryEntity::class,
        TransactionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(BigDecimalConverters::class)
abstract class FinFlowDatabase : RoomDatabase() {

    abstract fun financialMonthDao(): FinancialMonthDao

    abstract fun transactionDao(): TransactionDao

    abstract fun transactionCategoryDao(): TransactionCategoryDao
}