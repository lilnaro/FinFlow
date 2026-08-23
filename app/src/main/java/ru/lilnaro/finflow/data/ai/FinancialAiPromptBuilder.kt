package ru.lilnaro.finflow.data.ai

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ru.lilnaro.finflow.domain.model.FinancialAiMessage
import ru.lilnaro.finflow.domain.model.FinancialAnalysisCategory
import ru.lilnaro.finflow.domain.model.FinancialAnalysisContext
import ru.lilnaro.finflow.domain.model.FinancialAnalysisMonth
import ru.lilnaro.finflow.domain.model.FinancialAnalyticsSnapshot
import ru.lilnaro.finflow.domain.model.FinancialCategoryChange
import ru.lilnaro.finflow.domain.model.FinancialLargeTransactionSignal
import ru.lilnaro.finflow.domain.model.FinancialSemanticTransactionContext
import ru.lilnaro.finflow.domain.model.TransactionType

class FinancialAiPromptBuilder {

    fun buildSystemInstruction(
        question: String,
        analysisContext: FinancialAnalysisContext,
        analyticsSnapshot: FinancialAnalyticsSnapshot,
    ): String {
        val intent =
            detectIntent(
                question = question,
            )

        return buildString {
            appendLine(
                "Ты финансовый аналитик FinFlow.",
            )
            appendLine(
                "Отвечай на русском языке, если пользователь не попросил иначе.",
            )
            appendLine(
                "Контекст ниже уже подготовлен приложением специально под текущий вопрос.",
            )
            appendLine()
            appendRules()
            appendLine()
            appendLine(
                "ТИП КОНТЕКСТА: ${intent.name}",
            )
            appendLine()
            appendContextHeader(
                analysisContext = analysisContext,
            )
            appendFocusFacts(
                analyticsSnapshot =
                    analyticsSnapshot,
            )

            when (intent) {
                FinancialContextIntent.INCOME -> {
                    appendIncomeContext(
                        analysisContext =
                            analysisContext,
                        analyticsSnapshot =
                            analyticsSnapshot,
                    )
                }

                FinancialContextIntent.EXPENSE -> {
                    appendExpenseContext(
                        analysisContext =
                            analysisContext,
                        analyticsSnapshot =
                            analyticsSnapshot,
                    )
                }

                FinancialContextIntent.SEMANTIC_OTHER -> {
                    appendSemanticContext(
                        analysisContext =
                            analysisContext,
                    )
                }

                FinancialContextIntent.GENERAL -> {
                    appendGeneralContext(
                        question = question,
                        analysisContext =
                            analysisContext,
                        analyticsSnapshot =
                            analyticsSnapshot,
                    )
                }
            }
        }
    }

    fun buildConversationHistory(
        history: List<FinancialAiMessage>,
    ): List<FinancialAiMessage> {
        return history
            .filter { message ->
                message.text.isNotBlank()
            }
            .takeLast(
                MAX_HISTORY_MESSAGES,
            )
    }

    private fun StringBuilder.appendRules() {
        appendLine("КРИТИЧЕСКИЕ ПРАВИЛА:")
        appendLine(
            "1. Используй только предоставленные финансовые факты и текст пользователя.",
        )
        appendLine(
            "2. Не придумывай отсутствующие доходы, расходы, цели, регулярность или причины операций.",
        )
        appendLine(
            "3. Не пересчитывай денежные суммы, если приложение уже дало готовый показатель.",
        )
        appendLine(
            "4. Различай факт, наблюдение, прогноз и рекомендацию.",
        )
        appendLine(
            "5. Крупная операция не является плохой автоматически.",
        )
        appendLine(
            "6. Если истории мало, прямо говори об ограниченной уверенности.",
        )
        appendLine(
            "7. Название личной категории и комментарий под «Другое» помогают понять смысл операции, но не доказывают её необходимость, полезность или осознанность.",
        )
        appendLine(
            "8. Не называй расход инвестицией, вложением в себя или осознанной тратой только из-за названия категории или комментария. Без цели пользователя говори нейтрально: например, «расход на обучение».",
        )
        appendLine(
            "9. Смысловой контекст не должен менять числовой ранг: крупнейшая категория определяется суммой.",
        )
        appendLine(
            "10. Не сравнивай несвязанные показатели только потому, что их можно сравнить численно. Например, стартовый бюджет и доход месяца имеют разный смысл.",
        )
        appendLine(
            "11. Если готового прогноза нет, не экстраполируй первые дни месяца самостоятельно.",
        )
        appendLine(
            "12. Давай конкретные рекомендации без морализаторства и категоричных утверждений.",
        )
        appendLine(
            "13. Используй простой Markdown: **жирный текст** и маркированные списки. Таблицы не используй.",
        )
        appendLine(
            "14. Если не попросили подробный отчёт, отвечай компактно: примерно 120–200 слов.",
        )
    }

    private fun StringBuilder.appendContextHeader(
        analysisContext: FinancialAnalysisContext,
    ) {
        appendLine("ОБЪЁМ ДАННЫХ:")
        appendLine(
            "- месяцев: ${analysisContext.availableMonthCount}",
        )
        appendLine(
            "- операций: ${analysisContext.totalTransactionCount}",
        )
        appendLine(
            "- уверенность по объёму данных: ${analysisContext.confidence}",
        )
    }

    private fun StringBuilder.appendFocusFacts(
        analyticsSnapshot: FinancialAnalyticsSnapshot,
    ) {
        val focus =
            analyticsSnapshot.focusMonth
                ?: return

        appendLine()
        appendLine("ТЕКУЩИЙ ФОКУС:")
        appendLine(
            "- период: ${focus.monthNumber.toMonthName()} ${focus.year}",
        )
        appendLine(
            "- баланс: ${focus.currentBalance.toMoneyText()}",
        )
        appendLine(
            "- доходы: ${focus.totalIncome.toMoneyText()}",
        )
        appendLine(
            "- расходы: ${focus.totalExpense.toMoneyText()}",
        )

        focus.topIncomeCategory?.let { category ->
            appendLine(
                "- крупнейший источник дохода: ${category.categoryName ?: UNKNOWN_CATEGORY}, ${category.amount.toMoneyText()}",
            )
        }

        focus.topExpenseCategory?.let { category ->
            appendLine(
                "- крупнейшая статья расходов: ${category.categoryName ?: UNKNOWN_CATEGORY}, ${category.amount.toMoneyText()}",
            )
        }

        focus.pace?.let { pace ->
            appendLine(
                "- прошло дней: ${pace.elapsedDays}/${pace.totalPeriodDays}, осталось ${pace.remainingDays}",
            )
            appendLine(
                "- средний доход за прошедшие дни: ${pace.averageDailyIncome.toMoneyText()} в день",
            )
            appendLine(
                "- средний расход за прошедшие дни: ${pace.averageDailyExpense.toMoneyText()} в день",
            )

            if (
                pace.projectedIncome != null &&
                pace.projectedExpense != null &&
                pace.projectedFinalBalance != null
            ) {
                appendLine(
                    "- проверенный ориентир доходов: ${pace.projectedIncome.toMoneyText()}",
                )
                appendLine(
                    "- проверенный ориентир расходов: ${pace.projectedExpense.toMoneyText()}",
                )
                appendLine(
                    "- проверенный ориентир баланса: ${pace.projectedFinalBalance.toMoneyText()}",
                )
            } else {
                appendLine(
                    "- надёжный прогноз пока отсутствует; самостоятельная экстраполяция запрещена",
                )
            }
        }
    }

    private fun StringBuilder.appendIncomeContext(
        analysisContext: FinancialAnalysisContext,
        analyticsSnapshot: FinancialAnalyticsSnapshot,
    ) {
        appendLine()
        appendLine("РЕЛЕВАНТНЫЙ КОНТЕКСТ: ДОХОДЫ")

        analysisContext.months
            .takeLast(INCOME_HISTORY_MONTHS)
            .forEach { month ->
                appendIncomeMonth(
                    month = month,
                )
            }

        val history =
            analyticsSnapshot.history

        appendLine()
        appendLine("ИСТОРИЧЕСКИЕ ПОКАЗАТЕЛИ ДОХОДОВ:")
        history.averageMonthlyIncome?.let {
            appendLine(
                "- средний месячный доход: ${it.toMoneyText()}",
            )
        }
        history.incomeVolatilityPercent?.let {
            appendLine(
                "- волатильность доходов: ${it.toPercentText()}",
            )
        }
        history.latestClosedMonthChange?.let { change ->
            appendLine(
                "- изменение дохода последнего закрытого месяца: ${change.incomeDelta.toSignedMoneyText()}, ${change.incomeChangePercent?.toSignedPercentText() ?: "процент н/д"}",
            )
        }

        appendCategoryChanges(
            title =
                "ИЗМЕНЕНИЯ ИСТОЧНИКОВ ДОХОДА",
            changes =
                analyticsSnapshot.incomeCategoryChanges,
            limit =
                MAX_RELEVANT_CHANGES,
        )

        appendLargeSignals(
            signals =
                analyticsSnapshot
                    .largeTransactionSignals
                    .filter { signal ->
                        signal.type ==
                                TransactionType.INCOME
                    },
        )

        appendSemanticTransactions(
            title =
                "ЛИЧНЫЕ ДОХОДНЫЕ КАТЕГОРИИ ПОД «ДРУГОЕ»",
            transactions =
                analysisContext
                    .semanticTransactions
                    .filter { transaction ->
                        transaction.type ==
                                TransactionType.INCOME
                    },
            limit =
                MAX_RELEVANT_SEMANTIC,
        )
    }

    private fun StringBuilder.appendExpenseContext(
        analysisContext: FinancialAnalysisContext,
        analyticsSnapshot: FinancialAnalyticsSnapshot,
    ) {
        appendLine()
        appendLine("РЕЛЕВАНТНЫЙ КОНТЕКСТ: РАСХОДЫ")

        analysisContext.months
            .takeLast(EXPENSE_HISTORY_MONTHS)
            .forEach { month ->
                appendExpenseMonth(
                    month = month,
                )
            }

        val history =
            analyticsSnapshot.history

        appendLine()
        appendLine("ИСТОРИЧЕСКИЕ ПОКАЗАТЕЛИ РАСХОДОВ:")
        history.averageMonthlyExpense?.let {
            appendLine(
                "- средний месячный расход: ${it.toMoneyText()}",
            )
        }
        history.expenseVolatilityPercent?.let {
            appendLine(
                "- волатильность расходов: ${it.toPercentText()}",
            )
        }
        history.latestClosedMonthChange?.let { change ->
            appendLine(
                "- изменение расходов последнего закрытого месяца: ${change.expenseDelta.toSignedMoneyText()}, ${change.expenseChangePercent?.toSignedPercentText() ?: "процент н/д"}",
            )
        }

        appendCategoryChanges(
            title =
                "ИЗМЕНЕНИЯ КАТЕГОРИЙ РАСХОДОВ",
            changes =
                analyticsSnapshot.expenseCategoryChanges,
            limit =
                MAX_RELEVANT_CHANGES,
        )

        appendLargeSignals(
            signals =
                analyticsSnapshot
                    .largeTransactionSignals
                    .filter { signal ->
                        signal.type ==
                                TransactionType.EXPENSE
                    },
        )

        appendSemanticTransactions(
            title =
                "ЛИЧНЫЕ РАСХОДНЫЕ КАТЕГОРИИ ПОД «ДРУГОЕ»",
            transactions =
                analysisContext
                    .semanticTransactions
                    .filter { transaction ->
                        transaction.type ==
                                TransactionType.EXPENSE
                    },
            limit =
                MAX_RELEVANT_SEMANTIC,
        )
    }

    private fun StringBuilder.appendSemanticContext(
        analysisContext: FinancialAnalysisContext,
    ) {
        appendLine()
        appendLine(
            "РЕЛЕВАНТНЫЙ КОНТЕКСТ: ЛИЧНЫЕ КАТЕГОРИИ ПОД «ДРУГОЕ»",
        )
        appendLine(
            "Важно: комментарии ниже — слова пользователя, а не доказательство полезности или причины операции.",
        )

        appendSemanticTransactions(
            title =
                "СМЫСЛОВЫЕ ОПЕРАЦИИ",
            transactions =
                analysisContext.semanticTransactions,
            limit =
                MAX_SEMANTIC_FOCUS,
        )

        val relatedMonthIds =
            analysisContext
                .semanticTransactions
                .map { transaction ->
                    transaction.financialMonthId
                }
                .toSet()

        analysisContext.months
            .filter { month ->
                month.financialMonthId in
                        relatedMonthIds
            }
            .takeLast(SEMANTIC_MONTHS)
            .forEach { month ->
                appendLine()
                appendLine(
                    "${month.monthNumber.toMonthName()} ${month.year}: доходы ${month.totalIncome.toMoneyText()}, расходы ${month.totalExpense.toMoneyText()}, баланс ${month.finalBalance.toMoneyText()}",
                )
            }
    }

    private fun StringBuilder.appendGeneralContext(
        question: String,
        analysisContext: FinancialAnalysisContext,
        analyticsSnapshot: FinancialAnalyticsSnapshot,
    ) {
        appendLine()
        appendLine("РЕЛЕВАНТНЫЙ КОНТЕКСТ: ОБЩАЯ КАРТИНА")

        val maxMonths =
            if (
                requestsLongHistory(
                    question = question,
                )
            ) {
                MAX_LONG_HISTORY_MONTHS
            } else {
                GENERAL_HISTORY_MONTHS
            }

        analysisContext.months
            .takeLast(maxMonths)
            .forEach { month ->
                appendGeneralMonth(
                    month = month,
                )
            }

        val history =
            analyticsSnapshot.history

        appendLine()
        appendLine("ИСТОРИЯ:")
        appendLine(
            "- закрытых месяцев: ${history.closedMonthCount}",
        )
        history.averageMonthlyIncome?.let {
            appendLine(
                "- средний доход: ${it.toMoneyText()}",
            )
        }
        history.averageMonthlyExpense?.let {
            appendLine(
                "- средний расход: ${it.toMoneyText()}",
            )
        }
        history.averageFinalBalance?.let {
            appendLine(
                "- средний итоговый баланс: ${it.toMoneyText()}",
            )
        }

        appendCategoryChanges(
            title =
                "ЗАМЕТНЫЕ ИЗМЕНЕНИЯ РАСХОДОВ",
            changes =
                analyticsSnapshot.expenseCategoryChanges,
            limit =
                GENERAL_CHANGE_LIMIT,
        )

        appendCategoryChanges(
            title =
                "ЗАМЕТНЫЕ ИЗМЕНЕНИЯ ДОХОДОВ",
            changes =
                analyticsSnapshot.incomeCategoryChanges,
            limit =
                GENERAL_CHANGE_LIMIT,
        )

        appendLargeSignals(
            signals =
                analyticsSnapshot
                    .largeTransactionSignals,
        )

        appendSemanticTransactions(
            title =
                "РЕЛЕВАНТНЫЕ ЛИЧНЫЕ КАТЕГОРИИ ПОД «ДРУГОЕ»",
            transactions =
                analysisContext.semanticTransactions,
            limit =
                GENERAL_SEMANTIC_LIMIT,
        )
    }

    private fun StringBuilder.appendGeneralMonth(
        month: FinancialAnalysisMonth,
    ) {
        appendLine()
        appendLine(
            "${month.monthNumber.toMonthName()} ${month.year} ${if (month.isClosed) "[закрыт]" else "[активный]"}",
        )
        appendLine(
            "- доходы ${month.totalIncome.toMoneyText()}, расходы ${month.totalExpense.toMoneyText()}, баланс ${month.finalBalance.toMoneyText()}, операций ${month.transactionCount}",
        )

        month.incomeBreakdown
            .firstOrNull()
            ?.let { category ->
                appendLine(
                    "- топ доход: ${category.categoryName ?: UNKNOWN_CATEGORY}, ${category.amount.toMoneyText()}",
                )
            }

        month.expenseBreakdown
            .firstOrNull()
            ?.let { category ->
                appendLine(
                    "- топ расход: ${category.categoryName ?: UNKNOWN_CATEGORY}, ${category.amount.toMoneyText()}",
                )
            }
    }

    private fun StringBuilder.appendIncomeMonth(
        month: FinancialAnalysisMonth,
    ) {
        appendLine()
        appendLine(
            "${month.monthNumber.toMonthName()} ${month.year}: доходы ${month.totalIncome.toMoneyText()}",
        )
        appendCategories(
            categories =
                month.incomeBreakdown,
            limit =
                MAX_CATEGORIES_PER_TYPE,
        )
    }

    private fun StringBuilder.appendExpenseMonth(
        month: FinancialAnalysisMonth,
    ) {
        appendLine()
        appendLine(
            "${month.monthNumber.toMonthName()} ${month.year}: расходы ${month.totalExpense.toMoneyText()}",
        )
        appendCategories(
            categories =
                month.expenseBreakdown,
            limit =
                MAX_CATEGORIES_PER_TYPE,
        )
    }

    private fun StringBuilder.appendCategories(
        categories:
        List<FinancialAnalysisCategory>,
        limit: Int,
    ) {
        if (categories.isEmpty()) {
            appendLine("- категорий нет")
            return
        }

        categories
            .take(limit)
            .forEach { category ->
                appendLine(
                    "- ${category.categoryName ?: UNKNOWN_CATEGORY}: ${category.amount.toMoneyText()}, ${category.sharePercent.toPercentText()}, операций ${category.transactionCount}",
                )
            }
    }

    private fun StringBuilder.appendCategoryChanges(
        title: String,
        changes: List<FinancialCategoryChange>,
        limit: Int,
    ) {
        val limited =
            changes.take(limit)

        if (limited.isEmpty()) {
            return
        }

        appendLine()
        appendLine("$title:")

        limited.forEach { change ->
            appendLine(
                "- ${change.categoryName ?: UNKNOWN_CATEGORY}: ${change.previousAmount.toMoneyText()} -> ${change.currentAmount.toMoneyText()}, дельта ${change.delta.toSignedMoneyText()}, изменение ${change.changePercent?.toSignedPercentText() ?: "н/д"}",
            )
        }
    }

    private fun StringBuilder.appendLargeSignals(
        signals: List<FinancialLargeTransactionSignal>,
    ) {
        val limited =
            signals.take(
                MAX_LARGE_SIGNALS,
            )

        if (limited.isEmpty()) {
            return
        }

        appendLine()
        appendLine("КРУПНЫЕ ОПЕРАЦИИ-СИГНАЛЫ:")

        limited.forEach { signal ->
            appendLine(
                "- ${signal.type.toRussianName()}; ${signal.categoryName ?: UNKNOWN_CATEGORY}; ${signal.amount.toMoneyText()}; ${signal.monthlyTypeSharePercent.toPercentText()} суммы типа; x${signal.medianMultiplier.toOneDecimalText()} медианы; ${signal.createdAtMillis.toDateText()}",
            )
        }
    }

    private fun StringBuilder.appendSemanticTransactions(
        title: String,
        transactions:
        List<FinancialSemanticTransactionContext>,
        limit: Int,
    ) {
        val limited =
            transactions
                .takeLast(limit)

        if (limited.isEmpty()) {
            return
        }

        appendLine()
        appendLine("$title:")

        limited.forEach { transaction ->
            append(
                "- ${transaction.type.toRussianName()}; ",
            )
            append(
                "${transaction.amount.toMoneyText()}; ",
            )
            append(
                "личная категория «${transaction.customCategoryName}»; ",
            )
            append(
                "родитель «${transaction.parentCategoryName}»; ",
            )
            append(
                transaction.createdAtMillis.toDateText(),
            )

            transaction.note
                ?.takeIf { note ->
                    note.isNotBlank()
                }
                ?.let { note ->
                    append(
                        "; комментарий пользователя: «$note»",
                    )
                }

            appendLine()
        }
    }

    private fun detectIntent(
        question: String,
    ): FinancialContextIntent {
        val normalized =
            question
                .trim()
                .lowercase(
                    Locale.ROOT,
                )

        val semantic =
            normalized.containsAny(
                "другое",
                "личн",
                "свою катег",
                "свои катег",
                "пользовательск",
                "кастом",
                "комментар",
            )

        val income =
            normalized.containsAny(
                "доход",
                "зарплат",
                "подработ",
                "заработ",
                "поступлен",
                "источник денег",
            )

        val expense =
            normalized.containsAny(
                "расход",
                "трат",
                "потрат",
                "покуп",
                "эконом",
                "сэконом",
            )

        return when {
            semantic -> {
                FinancialContextIntent.SEMANTIC_OTHER
            }

            income && !expense -> {
                FinancialContextIntent.INCOME
            }

            expense && !income -> {
                FinancialContextIntent.EXPENSE
            }

            else -> {
                FinancialContextIntent.GENERAL
            }
        }
    }

    private fun requestsLongHistory(
        question: String,
    ): Boolean {
        val normalized =
            question.lowercase(
                Locale.ROOT,
            )

        return normalized.containsAny(
            "за год",
            "годов",
            "12 месяц",
            "все месяц",
            "за всё время",
            "за все время",
            "долгоср",
            "истор",
        )
    }

    private fun String.containsAny(
        vararg values: String,
    ): Boolean {
        return values.any { value ->
            contains(value)
        }
    }

    private fun TransactionType.toRussianName():
            String {
        return when (this) {
            TransactionType.INCOME -> "доход"
            TransactionType.EXPENSE -> "расход"
        }
    }

    private fun Int.toMonthName():
            String {
        return MONTH_NAMES.getOrElse(
            index = this - 1,
        ) {
            "Месяц $this"
        }
    }

    private fun BigDecimal.toMoneyText():
            String {
        val formatter =
            NumberFormat.getNumberInstance(
                Locale.forLanguageTag(
                    "ru-RU",
                ),
            ).apply {
                isGroupingUsed = true
                minimumFractionDigits = 0
                maximumFractionDigits = 2
                roundingMode =
                    RoundingMode.HALF_UP
            }

        return "${formatter.format(this)} ₽"
    }

    private fun BigDecimal.toSignedMoneyText():
            String {
        val sign =
            when {
                this > BigDecimal.ZERO -> "+"
                this < BigDecimal.ZERO -> "−"
                else -> ""
            }

        return "$sign${abs().toMoneyText()}"
    }

    private fun Double.toPercentText():
            String {
        return "${toOneDecimalText()}%"
    }

    private fun Double.toSignedPercentText():
            String {
        val sign =
            when {
                this > 0.0 -> "+"
                this < 0.0 -> "−"
                else -> ""
            }

        return "$sign${kotlin.math.abs(this).toOneDecimalText()}%"
    }

    private fun Double.toOneDecimalText():
            String {
        val formatter =
            NumberFormat.getNumberInstance(
                Locale.forLanguageTag(
                    "ru-RU",
                ),
            ).apply {
                minimumFractionDigits = 1
                maximumFractionDigits = 1
                roundingMode =
                    RoundingMode.HALF_UP
            }

        return formatter.format(this)
    }

    private fun Long.toDateText():
            String {
        return DATE_FORMAT.format(
            Date(this),
        )
    }

    private enum class FinancialContextIntent {
        GENERAL,
        INCOME,
        EXPENSE,
        SEMANTIC_OTHER,
    }

    private companion object {

        const val GENERAL_HISTORY_MONTHS =
            4

        const val MAX_LONG_HISTORY_MONTHS =
            12

        const val INCOME_HISTORY_MONTHS =
            6

        const val EXPENSE_HISTORY_MONTHS =
            6

        const val SEMANTIC_MONTHS =
            6

        const val MAX_CATEGORIES_PER_TYPE =
            7

        const val MAX_RELEVANT_CHANGES =
            6

        const val GENERAL_CHANGE_LIMIT =
            3

        const val MAX_LARGE_SIGNALS =
            5

        const val MAX_RELEVANT_SEMANTIC =
            8

        const val GENERAL_SEMANTIC_LIMIT =
            4

        const val MAX_SEMANTIC_FOCUS =
            24

        const val MAX_HISTORY_MESSAGES =
            8

        const val UNKNOWN_CATEGORY =
            "Категория недоступна"

        val MONTH_NAMES =
            listOf(
                "Январь",
                "Февраль",
                "Март",
                "Апрель",
                "Май",
                "Июнь",
                "Июль",
                "Август",
                "Сентябрь",
                "Октябрь",
                "Ноябрь",
                "Декабрь",
            )

        val DATE_FORMAT =
            SimpleDateFormat(
                "d MMMM yyyy",
                Locale.forLanguageTag(
                    "ru-RU",
                ),
            )
    }
}