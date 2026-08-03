package ru.lilnaro.finflow.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.lilnaro.finflow.domain.model.TransactionCategory
import ru.lilnaro.finflow.domain.model.TransactionType
import ru.lilnaro.finflow.domain.repository.FinanceRepository

class ObserveCategoriesByTypeUseCase(
    private val financeRepository: FinanceRepository,
) {

    operator fun invoke(
        type: TransactionType,
    ): Flow<List<TransactionCategory>> {
        return financeRepository
            .observeCategoriesByType(type)
            .map { categories ->
                categories.sortedWith(
                    compareBy<TransactionCategory>(
                        { category -> category.isCustom },
                        { category -> category.name.lowercase() },
                    ),
                )
            }
            .distinctUntilChanged()
    }
}