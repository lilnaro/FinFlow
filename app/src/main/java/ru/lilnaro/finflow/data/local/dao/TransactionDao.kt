package ru.lilnaro.finflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.lilnaro.finflow.data.local.entity.TransactionEntity

@Dao
interface TransactionDao {

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE financial_month_id = :financialMonthId
        ORDER BY created_at_millis DESC, id DESC
        """
    )
    fun observeTransactionsByMonth(
        financialMonthId: Long,
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE id = :transactionId
        LIMIT 1
        """
    )
    suspend fun getTransactionById(
        transactionId: Long,
    ): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(
        transaction: TransactionEntity,
    ): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateTransaction(
        transaction: TransactionEntity,
    )

    @Query(
        """
        DELETE FROM transactions
        WHERE id = :transactionId
        """
    )
    suspend fun deleteTransactionById(
        transactionId: Long,
    )

    @Query(
        """
        DELETE FROM transactions
        WHERE id IN (:transactionIds)
        """
    )
    suspend fun deleteTransactionsByIds(
        transactionIds: List<Long>,
    )
}