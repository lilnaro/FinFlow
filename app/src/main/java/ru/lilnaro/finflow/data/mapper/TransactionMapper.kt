package ru.lilnaro.finflow.data.mapper

import ru.lilnaro.finflow.data.local.entity.TransactionEntity
import ru.lilnaro.finflow.domain.model.Transaction

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        financialMonthId = financialMonthId,
        amount = amount,
        type = type,
        categoryId = categoryId,
        note = note,
        createdAtMillis = createdAtMillis,
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        financialMonthId = financialMonthId,
        amount = amount,
        type = type,
        categoryId = categoryId,
        note = note,
        createdAtMillis = createdAtMillis,
    )
}