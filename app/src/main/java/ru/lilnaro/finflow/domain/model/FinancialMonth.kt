package ru.lilnaro.finflow.domain.model

data class FinancialMonth(
    val id: Long = 0L,
    val year: Int,
    val monthNumber: Int,
    val initialBudgetInKopecks: Long,
    val startedAtMillis: Long,
    val closedAtMillis: Long? = null,
) {

    val isClosed: Boolean
        get() = closedAtMillis != null

    init {
        require(year in MIN_YEAR..MAX_YEAR) {
            "Год финансового месяца должен находиться в диапазоне $MIN_YEAR–$MAX_YEAR"
        }

        require(monthNumber in 1..12) {
            "Номер месяца должен находиться в диапазоне от 1 до 12"
        }

        require(initialBudgetInKopecks >= 0L) {
            "Стартовый бюджет не может быть отрицательным"
        }

        require(startedAtMillis > 0L) {
            "Время начала финансового месяца должно быть положительным"
        }

        require(
            closedAtMillis == null ||
                    closedAtMillis >= startedAtMillis,
        ) {
            "Время завершения месяца не может быть раньше времени его начала"
        }
    }

    companion object {
        const val MIN_YEAR = 2026

        const val MAX_YEAR = 2100
    }
}