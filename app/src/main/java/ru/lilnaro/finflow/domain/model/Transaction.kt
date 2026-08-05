package ru.lilnaro.finflow.domain.model

import java.math.BigDecimal

data class Transaction(
    val id: Long = 0L,
    val financialMonthId: Long,
    val amount: BigDecimal,
    val type: TransactionType,
    val categoryId: Long,
    val note: String = "",
    val createdAtMillis: Long,
) {

    init {
        require(id >= 0L) {
            "Идентификатор транзакции не может быть отрицательным"
        }

        require(financialMonthId > 0L) {
            "Идентификатор финансового месяца должен быть положительным"
        }

        require(amount > BigDecimal.ZERO) {
            "Сумма транзакции должна быть больше нуля"
        }

        require(categoryId > 0L) {
            "Идентификатор категории должен быть положительным"
        }

        require(createdAtMillis > 0L) {
            "Время создания транзакции должно быть положительным"
        }
    }
}