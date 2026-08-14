package ru.lilnaro.finflow.data.local.initializer

import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.lilnaro.finflow.data.local.dao.TransactionCategoryDao
import ru.lilnaro.finflow.data.local.entity.TransactionCategoryEntity
import ru.lilnaro.finflow.domain.model.TransactionType

class DefaultTransactionCategoriesInitializer(
    private val transactionCategoryDao: TransactionCategoryDao,
) {

    private val initializationMutex = Mutex()

    suspend fun initialize() {
        initializationMutex.withLock {
            ensureCategoriesExist(
                type = TransactionType.EXPENSE,
                categoryNames = DEFAULT_EXPENSE_CATEGORIES,
            )

            ensureCategoriesExist(
                type = TransactionType.INCOME,
                categoryNames = DEFAULT_INCOME_CATEGORIES,
            )
        }
    }

    private suspend fun ensureCategoriesExist(
        type: TransactionType,
        categoryNames: List<String>,
    ) {
        val existingBuiltInCategoryNames =
            transactionCategoryDao
                .observeCategoriesByType(
                    type = type,
                )
                .first()
                .filter { category ->
                    !category.isCustom
                }
                .map { category ->
                    category.name.normalizeForComparison()
                }
                .toSet()

        categoryNames
            .filterNot { categoryName ->
                categoryName.normalizeForComparison() in
                        existingBuiltInCategoryNames
            }
            .forEach { categoryName ->
                transactionCategoryDao.insertCategory(
                    category = TransactionCategoryEntity(
                        name = categoryName,
                        type = type,
                        isCustom = false,
                        parentCategoryId = null,
                    ),
                )
            }
    }

    private fun String.normalizeForComparison(): String {
        return trim()
            .lowercase(Locale.ROOT)
    }

    private companion object {

        val DEFAULT_EXPENSE_CATEGORIES = listOf(
            "Продукты",
            "Транспорт",
            "Жильё",
            "Здоровье",
            "Досуг",
            "Покупки",
            "Другое",
        )

        val DEFAULT_INCOME_CATEGORIES = listOf(
            "Зарплата",
            "Подработка",
            "Подарки",
            "Инвестиции",
            "Другое",
        )
    }
}