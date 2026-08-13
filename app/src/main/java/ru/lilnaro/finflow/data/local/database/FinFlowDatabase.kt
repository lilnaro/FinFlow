package ru.lilnaro.finflow.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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

    companion object {

        private const val DATABASE_NAME = "finflow.db"

        @Volatile
        private var instance: FinFlowDatabase? = null

        fun getInstance(context: Context): FinFlowDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinFlowDatabase::class.java,
                    DATABASE_NAME,
                )
                    .build()
                    .also { database ->
                        instance = database
                    }
            }
        }
    }
}