package ru.lilnaro.finflow.data.mapper

import ru.lilnaro.finflow.data.local.entity.FinancialMonthEntity
import ru.lilnaro.finflow.domain.model.FinancialMonth

fun FinancialMonthEntity.toDomain(): FinancialMonth {
    return FinancialMonth(
        id = id,
        year = year,
        monthNumber = monthNumber,
        initialBudget = initialBudget,
        startedAtMillis = startedAtMillis,
        closedAtMillis = closedAtMillis,
    )
}

fun FinancialMonth.toEntity(): FinancialMonthEntity {
    return FinancialMonthEntity(
        id = id,
        year = year,
        monthNumber = monthNumber,
        initialBudget = initialBudget,
        startedAtMillis = startedAtMillis,
        closedAtMillis = closedAtMillis,
    )
}