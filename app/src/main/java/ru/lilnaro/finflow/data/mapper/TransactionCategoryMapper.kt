package ru.lilnaro.finflow.data.mapper

import ru.lilnaro.finflow.data.local.entity.TransactionCategoryEntity
import ru.lilnaro.finflow.domain.model.TransactionCategory

fun TransactionCategoryEntity.toDomain(): TransactionCategory {
    return TransactionCategory(
        id = id,
        name = name,
        type = type,
        isCustom = isCustom,
        parentCategoryId = parentCategoryId,
    )
}

fun TransactionCategory.toEntity(): TransactionCategoryEntity {
    return TransactionCategoryEntity(
        id = id,
        name = name,
        type = type,
        isCustom = isCustom,
        parentCategoryId = parentCategoryId,
    )
}