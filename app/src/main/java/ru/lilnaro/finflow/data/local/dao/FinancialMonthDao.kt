package ru.lilnaro.finflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.lilnaro.finflow.data.local.entity.FinancialMonthEntity

@Dao
interface FinancialMonthDao {

    @Query(
        """
        SELECT *
        FROM financial_months
        WHERE closed_at_millis IS NULL
        ORDER BY started_at_millis DESC
        LIMIT 1
        """
    )
    fun observeActiveFinancialMonth(): Flow<FinancialMonthEntity?>

    @Query(
        """
        SELECT *
        FROM financial_months
        ORDER BY year DESC, month_number DESC
        """
    )
    fun observeFinancialMonths(): Flow<List<FinancialMonthEntity>>

    @Query(
        """
        SELECT *
        FROM financial_months
        WHERE id = :financialMonthId
        LIMIT 1
        """
    )
    suspend fun getFinancialMonthById(
        financialMonthId: Long,
    ): FinancialMonthEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFinancialMonth(
        financialMonth: FinancialMonthEntity,
    ): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateFinancialMonth(
        financialMonth: FinancialMonthEntity,
    )
}