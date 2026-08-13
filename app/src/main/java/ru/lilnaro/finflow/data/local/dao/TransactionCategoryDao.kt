package ru.lilnaro.finflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.lilnaro.finflow.data.local.entity.TransactionCategoryEntity
import ru.lilnaro.finflow.domain.model.TransactionType

@Dao
interface TransactionCategoryDao {

    @Query(
        """
        SELECT *
        FROM transaction_categories
        WHERE type = :type
        """
    )
    fun observeCategoriesByType(
        type: TransactionType,
    ): Flow<List<TransactionCategoryEntity>>

    @Query(
        """
        SELECT *
        FROM transaction_categories
        WHERE id = :categoryId
        LIMIT 1
        """
    )
    suspend fun getCategoryById(
        categoryId: Long,
    ): TransactionCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(
        category: TransactionCategoryEntity,
    ): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateCategory(
        category: TransactionCategoryEntity,
    )

    @Query(
        """
        DELETE FROM transaction_categories
        WHERE id = :categoryId
        """
    )
    suspend fun deleteCategoryById(
        categoryId: Long,
    )
}